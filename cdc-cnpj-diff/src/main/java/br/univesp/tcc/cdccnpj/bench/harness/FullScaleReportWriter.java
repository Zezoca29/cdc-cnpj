package br.univesp.tcc.cdccnpj.bench.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FullScaleReportWriter {

    private FullScaleReportWriter() {
    }

    public static void write(Path outputPath, FullScaleDiffReport report, H2ValidationResult h2) throws IOException {
        Files.createDirectories(outputPath.getParent());
        StringBuilder body = new StringBuilder();
        body.append("run_id,engine,competencia_de,competencia_para,bucket_scale,total_wall_hours,total_events,peak_heap_bytes,peak_rss_bytes,bytes_read,h2_passed\n");
        body.append(report.runId()).append(',');
        body.append(report.engine()).append(',');
        body.append(report.competenciaDe()).append(',');
        body.append(report.competenciaPara()).append(',');
        body.append(BenchmarkConstants.FULL_SCALE_BUCKET_COUNT).append(',');
        body.append(report.totalWallTimeHours()).append(',');
        body.append(report.totalEventCount()).append(',');
        body.append(report.peakHeapBytes()).append(',');
        body.append(report.peakRssBytes()).append(',');
        body.append(report.totalBytesRead()).append(',');
        body.append(h2.passed()).append('\n');
        Files.writeString(outputPath, body.toString(), StandardCharsets.UTF_8);

        Path detailPath = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".csv", "-detail.csv"));
        BenchmarkCsvWriter.write(detailPath, report.perTableResults());
    }
}
