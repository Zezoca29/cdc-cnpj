package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.diff.DiffEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Diff em todos os 256 buckets — validação direta de H2 (Expansão X1).
 */
public final class FullScaleDiffBenchmark {

    private final DiffMeasurementHarness harness = new DiffMeasurementHarness();

    public FullScaleDiffReport run(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            EngineKind engine,
            List<String> tabelas) {
        String runId = DiffMeasurementHarness.newRunId();
        DiffEngine diffEngine = harness.createEngine(engine);
        List<Integer> bucketIds = DiffMeasurementHarness.bucketIdsForScale(BenchmarkConstants.FULL_SCALE_BUCKET_COUNT);

        List<MeasurementResult> perTable = new ArrayList<>();
        for (String tabela : tabelas) {
            perTable.add(harness.measure(
                    diffEngine,
                    engine,
                    canonicalRoot,
                    competenciaDe,
                    competenciaPara,
                    tabela,
                    bucketIds,
                    1,
                    false,
                    runId));
        }

        long totalWall = perTable.stream().mapToLong(MeasurementResult::wallTimeNanos).sum();
        long totalCpu = perTable.stream().mapToLong(MeasurementResult::cpuTimeNanos).sum();
        long peakHeap = perTable.stream().mapToLong(MeasurementResult::peakHeapBytes).max().orElse(0);
        long peakRss = perTable.stream().mapToLong(MeasurementResult::peakRssBytes).max().orElse(0);
        long totalBytes = perTable.stream().mapToLong(MeasurementResult::bytesRead).sum();
        long totalEvents = perTable.stream().mapToLong(MeasurementResult::eventCount).sum();

        return new FullScaleDiffReport(
                runId,
                engine,
                competenciaDe,
                competenciaPara,
                totalWall,
                totalCpu,
                peakHeap,
                peakRss,
                totalBytes,
                totalEvents,
                List.copyOf(perTable));
    }
}
