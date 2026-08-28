package br.univesp.tcc.cdccnpj.ingestion.download;

import java.nio.file.Path;
import java.util.List;

public record DownloadReport(
        String competencia,
        String snapshotDate,
        List<String> downloaded,
        List<String> skipped,
        Path manifestPath) {
}
