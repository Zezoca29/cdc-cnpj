package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.diff.DiffEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Matriz experimental: repetições × motores × escalas de bucket (spec §9, D2–D3).
 */
public final class BenchmarkMatrix {

    private final DiffMeasurementHarness harness = new DiffMeasurementHarness();

    public List<MeasurementResult> run(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tabelas,
            List<EngineKind> engines,
            int[] bucketScales,
            int repetitions,
            int warmupDiscarded) {
        String runId = DiffMeasurementHarness.newRunId();
        List<MeasurementResult> results = new ArrayList<>();

        for (EngineKind engineKind : engines) {
            DiffEngine engine = harness.createEngine(engineKind);
            for (String tabela : tabelas) {
                for (int scale : bucketScales) {
                    List<Integer> bucketIds = DiffMeasurementHarness.bucketIdsForScale(scale);
                    for (int repetition = 1; repetition <= repetitions; repetition++) {
                        boolean warmup = repetition <= warmupDiscarded;
                        results.add(harness.measure(
                                engine,
                                engineKind,
                                canonicalRoot,
                                competenciaDe,
                                competenciaPara,
                                tabela,
                                bucketIds,
                                repetition,
                                warmup,
                                runId));
                    }
                }
            }
        }
        return List.copyOf(results);
    }
}
