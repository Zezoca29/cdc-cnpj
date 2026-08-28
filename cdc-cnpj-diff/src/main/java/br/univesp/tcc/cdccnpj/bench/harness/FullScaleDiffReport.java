package br.univesp.tcc.cdccnpj.bench.harness;

import java.util.List;

public record FullScaleDiffReport(
        String runId,
        EngineKind engine,
        String competenciaDe,
        String competenciaPara,
        long totalWallTimeNanos,
        long totalCpuTimeNanos,
        long peakHeapBytes,
        long peakRssBytes,
        long totalBytesRead,
        long totalEventCount,
        List<MeasurementResult> perTableResults) {

    public double totalWallTimeHours() {
        return totalWallTimeNanos / 3_600_000_000_000.0;
    }
}
