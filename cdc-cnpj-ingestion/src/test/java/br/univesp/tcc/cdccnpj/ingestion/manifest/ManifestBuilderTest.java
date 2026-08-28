package br.univesp.tcc.cdccnpj.ingestion.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestBuilderTest {

    @Test
    void buildsManifestWithSha256AndTabela(@TempDir Path rawDir, @TempDir Path manifestDir) throws Exception {
        Path zipPath = rawDir.resolve("Empresas0.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("Empresas0.csv"));
            zipOutputStream.write("12345678;RAZAO;2062;49;1000,00;01;".getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }

        Manifesto manifesto = ManifestBuilder.fromRawDirectory(rawDir, "2026-06", "casa_dos_dados", "https://example");
        Path manifestPath = manifestDir.resolve("manifesto.json");
        ManifestIo.write(manifestPath, manifesto);

        Manifesto loaded = ManifestIo.read(manifestPath);
        assertEquals("2026-06", loaded.competencia());
        assertEquals(1, loaded.arquivos().size());
        ManifestArquivo arquivo = loaded.arquivos().getFirst();
        assertEquals("empresas", arquivo.tabela());
        assertEquals(Sha256Hasher.hashFile(zipPath), arquivo.sha256());
        assertTrue(arquivo.tamanhoBytes() > 0);
    }

    @Test
    void resolvesSimplesZipTabela(@TempDir Path rawDir) throws Exception {
        Path zipPath = rawDir.resolve("Simples.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("Simples.csv"));
            zipOutputStream.write(
                    "12345678;S;20190101;00000000;N;00000000;00000000".getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }

        Manifesto manifesto = ManifestBuilder.fromRawDirectory(rawDir, "2026-06", "casa_dos_dados", "https://example");
        assertEquals(1, manifesto.arquivos().size());
        assertEquals("simples", manifesto.arquivos().getFirst().tabela());
        assertEquals(0, manifesto.arquivos().getFirst().parte());
    }
}
