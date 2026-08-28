package br.univesp.tcc.cdccnpj.bench.harness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agrega medições brutas em mediana, IQR e speedup (spec §9, D3).
 */
public final class BenchmarkAnalyzer {

    public record BenchmarkSummary(
            String engine,
            String tabela,
            int bucketScale,
            double medianWallMs,
            double q1WallMs,
            double q3WallMs,
            double medianThroughput,
            double medianPeakHeapBytes,
            Double speedupVsE1) {
    }

    public List<BenchmarkSummary> summarize(List<MeasurementResult> results) {
        Map<String, List<MeasurementResult>> grouped = results.stream()
                .filter(result -> !result.warmup())
                .collect(Collectors.groupingBy(
                        result -> result.engine() + "|" + result.tabela() + "|" + result.bucketScale(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, Double> e1Medians = new LinkedHashMap<>();
        List<BenchmarkSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<MeasurementResult>> entry : grouped.entrySet()) {
            List<MeasurementResult> group = entry.getValue();
            MeasurementResult first = group.getFirst();
            double medianWall = median(group.stream().map(MeasurementResult::wallTimeNanos).toList()) / 1_000_000.0;
            double q1 = quartile(group.stream().map(MeasurementResult::wallTimeNanos).toList(), 0.25) / 1_000_000.0;
            double q3 = quartile(group.stream().map(MeasurementResult::wallTimeNanos).toList(), 0.75) / 1_000_000.0;
            double medianThroughput = median(group.stream()
                    .map(MeasurementResult::throughputEventsPerSecond)
                    .toList());
            double medianHeap = median(group.stream()
                    .map(result -> (double) result.peakHeapBytes())
                    .toList());

            String key = first.tabela() + "|" + first.bucketScale();
            if (first.engine() == EngineKind.E1) {
                e1Medians.put(key, medianWall);
            }

            summaries.add(new BenchmarkSummary(
                    first.engine().name(),
                    first.tabela(),
                    first.bucketScale(),
                    medianWall,
                    q1,
                    q3,
                    medianThroughput,
                    medianHeap,
                    null));
        }

        List<BenchmarkSummary> withSpeedup = new ArrayList<>();
        for (BenchmarkSummary summary : summaries) {
            Double speedup = null;
            if ("E3".equals(summary.engine())) {
                Double e1Median = e1Medians.get(summary.tabela() + "|" + summary.bucketScale());
                if (e1Median != null && summary.medianWallMs() > 0) {
                    speedup = e1Median / summary.medianWallMs();
                }
            }
            withSpeedup.add(new BenchmarkSummary(
                    summary.engine(),
                    summary.tabela(),
                    summary.bucketScale(),
                    summary.medianWallMs(),
                    summary.q1WallMs(),
                    summary.q3WallMs(),
                    summary.medianThroughput(),
                    summary.medianPeakHeapBytes(),
                    speedup));
        }
        return List.copyOf(withSpeedup);
    }

    public static String formatSummaries(List<BenchmarkSummary> summaries) {
        StringBuilder out = new StringBuilder();
        out.append(String.format(
                Locale.ROOT,
                "%-4s %-18s %6s %10s %10s %10s %12s %8s%n",
                "eng",
                "tabela",
                "scale",
                "median_ms",
                "q1_ms",
                "q3_ms",
                "throughput",
                "speedup"));
        for (BenchmarkSummary summary : summaries) {
            out.append(String.format(
                    Locale.ROOT,
                    "%-4s %-18s %6d %10.2f %10.2f %10.2f %12.2f %8s%n",
                    summary.engine(),
                    summary.tabela(),
                    summary.bucketScale(),
                    summary.medianWallMs(),
                    summary.q1WallMs(),
                    summary.q3WallMs(),
                    summary.medianThroughput(),
                    summary.speedupVsE1() == null ? "-" : String.format(Locale.ROOT, "%.2f", summary.speedupVsE1())));
        }
        return out.toString();
    }

    private static double median(List<? extends Number> values) {
        List<Double> sorted = values.stream().map(Number::doubleValue).sorted().toList();
        if (sorted.isEmpty()) {
            return 0;
        }
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
        return sorted.get(mid);
    }

    private static double quartile(List<Long> values, double quantile) {
        List<Long> sorted = values.stream().sorted().toList();
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}
