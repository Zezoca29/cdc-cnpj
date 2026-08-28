package br.univesp.tcc.cdccnpj.bench.harness;

import java.time.Instant;

public record MeasurementResult(
        Instant timestamp,
        String runId,
        EngineKind engine,
        String tabela,
        int bucketScale,
        int repetition,
        boolean warmup,
        long wallTimeNanos,
        long cpuTimeNanos,
        long peakHeapBytes,
        long peakRssBytes,
        long bytesRead,
        long eventCount,
        double throughputEventsPerSecond) {
}
