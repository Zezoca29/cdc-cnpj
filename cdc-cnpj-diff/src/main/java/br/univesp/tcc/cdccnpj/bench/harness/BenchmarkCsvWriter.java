package br.univesp.tcc.cdccnpj.bench.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BenchmarkCsvWriter {

    private static final String HEADER =
            "timestamp,run_id,engine,tabela,bucket_scale,repetition,warmup,wall_time_ms,cpu_time_ms,peak_heap_bytes,peak_rss_bytes,bytes_read,event_count,throughput_events_per_sec";

    private BenchmarkCsvWriter() {
    }

    public static void write(Path outputPath, List<MeasurementResult> results) throws IOException {
        Files.createDirectories(outputPath.getParent());
        StringBuilder body = new StringBuilder(HEADER).append('\n');
        for (MeasurementResult result : results) {
            body.append(result.timestamp()).append(',');
            body.append(result.runId()).append(',');
            body.append(result.engine()).append(',');
            body.append(result.tabela()).append(',');
            body.append(result.bucketScale()).append(',');
            body.append(result.repetition()).append(',');
            body.append(result.warmup()).append(',');
            body.append(result.wallTimeNanos() / 1_000_000.0).append(',');
            body.append(result.cpuTimeNanos() / 1_000_000.0).append(',');
            body.append(result.peakHeapBytes()).append(',');
            body.append(result.peakRssBytes()).append(',');
            body.append(result.bytesRead()).append(',');
            body.append(result.eventCount()).append(',');
            body.append(result.throughputEventsPerSecond()).append('\n');
        }
        Files.writeString(outputPath, body.toString(), StandardCharsets.UTF_8);
    }
}
