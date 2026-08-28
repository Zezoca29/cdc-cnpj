package br.univesp.tcc.cdccnpj.ingestion.download;

import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Manifesto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetenciaDownloaderTest {

  @TempDir
  Path tempDir;

  private HttpServer server;
  private String baseUrl;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/arquivos/2026-05-10/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      String fileName = path.substring(path.lastIndexOf('/') + 1);
      byte[] body = tinyZip(fileName.replace(".zip", ".csv"));
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort() + "/arquivos";
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void downloadsNucleoFilesAndWritesManifest() throws IOException {
    DownloadSource source = new DownloadSource() {
      @Override
      public java.util.List<DownloadTarget> targets(String snapshotDate, java.util.List<String> fileNames) {
        return fileNames.stream()
            .map(name -> new DownloadTarget(name, baseUrl + "/" + snapshotDate + "/" + name, true))
            .toList();
      }

      @Override
      public String origem() {
        return "test";
      }

      @Override
      public String origemUrlBase() {
        return baseUrl;
      }
    };

    Path rawDir = tempDir.resolve("raw");
    Path manifestPath = tempDir.resolve("manifesto.json");
    CompetenciaDownloader downloader = new CompetenciaDownloader(new HttpFileDownloader(HttpClient.newHttpClient()), source);

    DownloadReport report = downloader.downloadCompetencia("2026-06", "2026-05-10", rawDir, manifestPath);

    assertEquals(CnpjOpenDataCatalog.NUCLEO_FILES.size(), report.downloaded().size());
    assertEquals(0, report.skipped().size());
    assertTrue(Files.exists(manifestPath));
    Manifesto manifesto = ManifestIo.read(manifestPath);
    assertEquals("2026-06", manifesto.competencia());
    assertEquals(CnpjOpenDataCatalog.NUCLEO_FILES.size(), manifesto.arquivos().size());
  }

  @Test
  void skipsExistingFiles() throws IOException {
    Path rawDir = tempDir.resolve("raw");
    Files.createDirectories(rawDir);
    for (String fileName : CnpjOpenDataCatalog.NUCLEO_FILES) {
      Files.write(rawDir.resolve(fileName), tinyZip("x"));
    }

    DownloadSource source = new DownloadSource() {
      @Override
      public java.util.List<DownloadTarget> targets(String snapshotDate, java.util.List<String> fileNames) {
        return fileNames.stream()
            .map(name -> new DownloadTarget(name, baseUrl + "/" + snapshotDate + "/" + name, true))
            .toList();
      }

      @Override
      public String origem() {
        return "test";
      }

      @Override
      public String origemUrlBase() {
        return baseUrl;
      }
    };

    CompetenciaDownloader downloader = new CompetenciaDownloader(new HttpFileDownloader(HttpClient.newHttpClient()), source);
    DownloadReport report = downloader.downloadCompetencia("2026-06", "2026-05-10", rawDir, tempDir.resolve("manifesto.json"));

    assertEquals(0, report.downloaded().size());
    assertEquals(CnpjOpenDataCatalog.NUCLEO_FILES.size(), report.skipped().size());
  }

  @Test
  void resolveSnapshotDateFromProperties() {
    assertEquals("2026-05-10", CompetenciaDownloader.resolveSnapshotDate("2026-06"));
    assertEquals("2026-05-14", CompetenciaDownloader.resolveSnapshotDate("2026-07"));
  }

  private static byte[] tinyZip(String entryName) throws IOException {
    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write("x".getBytes());
      zip.closeEntry();
    }
    return buffer.toByteArray();
  }
}
