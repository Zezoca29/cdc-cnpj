package br.univesp.tcc.cdccnpj.ingestion.download;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI para A1–A2: baixa ZIPs de uma competência e grava manifesto.
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-ingestion exec:java -Dexec.args="--competencia 2026-06"}
 */
public final class DownloadCli {

    private DownloadCli() {
    }

    public static void main(String[] args) {
        String competencia = require(args, "--competencia");
        String snapshotDate = optional(args, "--snapshot");
        if (snapshotDate == null) {
            snapshotDate = CompetenciaDownloader.resolveSnapshotDate(competencia);
        }

        Path projectRoot = Paths.get("").toAbsolutePath();
        Path rawDir = projectRoot.resolve("data/raw/competencia=" + competencia);
        Path manifestPath = projectRoot.resolve("data/manifests/competencia=" + competencia + "/manifesto.json");

        CompetenciaDownloader downloader = new CompetenciaDownloader();
        DownloadReport report = downloader.downloadCompetencia(competencia, snapshotDate, rawDir, manifestPath);

        System.out.printf(
                "competencia=%s snapshot=%s downloaded=%d skipped=%d manifest=%s%n",
                report.competencia(),
                report.snapshotDate(),
                report.downloaded().size(),
                report.skipped().size(),
                report.manifestPath());
    }

    private static String require(String[] args, String flag) {
        String value = optional(args, flag);
        if (value == null) {
            throw new IllegalArgumentException("Missing required flag " + flag);
        }
        return value;
    }

    private static String optional(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
}
