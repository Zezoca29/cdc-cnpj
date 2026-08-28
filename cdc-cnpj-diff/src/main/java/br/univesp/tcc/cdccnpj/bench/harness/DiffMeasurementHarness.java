package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.e1.PostgresDiffEngine;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executa uma medição de diff com métricas de tempo, memória, I/O e throughput (D2).
 */
public final class DiffMeasurementHarness {

    private final MeterRegistry meterRegistry;

    public DiffMeasurementHarness() {
        this(new SimpleMeterRegistry());
    }

    public DiffMeasurementHarness(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public DiffEngine createEngine(EngineKind engine) {
        return switch (engine) {
            case E1 -> PostgresDiffEngine.localhostDefaults();
            case E3 -> new HashDiffEngine();
        };
    }

    public MeasurementResult measure(
            DiffEngine engine,
            EngineKind engineKind,
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            List<Integer> bucketIds,
            int repetition,
            boolean warmup,
            String runId) {
        long bytesRead =
                IoBytesEstimator.estimateBytesRead(canonicalRoot, competenciaDe, competenciaPara, tabela, bucketIds);

        long cpuStart = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        long wallStart = System.nanoTime();
        long eventCount;

        try (PeakMemorySampler sampler = PeakMemorySampler.start()) {
            eventCount = runDiff(engine, canonicalRoot, competenciaDe, competenciaPara, tabela, bucketIds);
            long wallNanos = System.nanoTime() - wallStart;
            long cpuNanos = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - cpuStart;
            timerSample.stop(Timer.builder("cdc.diff.wall")
                    .tag("engine", engineKind.name())
                    .tag("tabela", tabela)
                    .tag("bucket_scale", Integer.toString(bucketIds.size()))
                    .register(meterRegistry));

            double throughput = wallNanos > 0 ? eventCount / (wallNanos / 1_000_000_000.0) : 0.0;
            return new MeasurementResult(
                    Instant.now(),
                    runId,
                    engineKind,
                    tabela,
                    bucketIds.size(),
                    repetition,
                    warmup,
                    wallNanos,
                    cpuNanos,
                    sampler.peakHeapBytes(),
                    sampler.peakRssBytes(),
                    bytesRead,
                    eventCount,
                    throughput);
        }
    }

    private static long runDiff(
            DiffEngine engine,
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            List<Integer> bucketIds) {
        long total = 0;
        for (int bucketId : bucketIds) {
            total += engine.diff(canonicalRoot, competenciaDe, competenciaPara, tabela, bucketId).count();
        }
        return total;
    }

    public static List<Integer> bucketIdsForScale(int scale) {
        List<Integer> ids = new ArrayList<>(scale);
        for (int i = 0; i < scale; i++) {
            ids.add(i);
        }
        return List.copyOf(ids);
    }

    public static String newRunId() {
        return UUID.nameUUIDFromBytes(Long.toString(System.nanoTime()).getBytes()).toString();
    }
}
