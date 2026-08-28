package br.univesp.tcc.cdccnpj.ingestion.download;

import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestBuilder;
import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Manifesto;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Baixa snapshots do CNPJ para {@code data/raw/competencia=YYYY-MM/} e gera manifesto (A1–A3).
 */
public final class CompetenciaDownloader {

    private final HttpFileDownloader fileDownloader;
    private final DownloadSource primarySource;

    public CompetenciaDownloader() {
        this(new HttpFileDownloader(), new CasaDosDadosSource());
    }

    public CompetenciaDownloader(HttpFileDownloader fileDownloader, DownloadSource primarySource) {
        this.fileDownloader = fileDownloader;
        this.primarySource = primarySource;
    }

    public DownloadReport downloadCompetencia(String competencia, String snapshotDate, Path rawDirectory, Path manifestPath) {
        List<DownloadTarget> targets =
                primarySource.targets(snapshotDate, CnpjOpenDataCatalog.NUCLEO_FILES);
        List<String> downloaded = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (DownloadTarget target : targets) {
            Path destination = rawDirectory.resolve(target.fileName());
            if (isPresent(destination)) {
                skipped.add(target.fileName());
                continue;
            }
            fileDownloader.download(target, destination);
            downloaded.add(target.fileName());
        }

        Manifesto manifesto = ManifestBuilder.fromRawDirectory(
                rawDirectory, competencia, primarySource.origem(), primarySource.origemUrlBase() + "/" + snapshotDate);
        ManifestIo.write(manifestPath, manifesto);
        return new DownloadReport(competencia, snapshotDate, List.copyOf(downloaded), List.copyOf(skipped), manifestPath);
    }

    public static String resolveSnapshotDate(String competencia) {
        Properties properties = new Properties();
        try (InputStream inputStream =
                CompetenciaDownloader.class.getResourceAsStream("/competencia-snapshots.properties")) {
            if (inputStream == null) {
                throw new ParseStageException("Missing competencia-snapshots.properties");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed loading competencia snapshot mapping", e);
        }
        String snapshot = properties.getProperty(competencia);
        if (snapshot == null || snapshot.isBlank()) {
            throw new ParseStageException(
                    "No snapshot date configured for competencia " + competencia + " — set it in competencia-snapshots.properties");
        }
        return snapshot.trim();
    }

    private static boolean isPresent(Path path) {
        try {
            return Files.exists(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
