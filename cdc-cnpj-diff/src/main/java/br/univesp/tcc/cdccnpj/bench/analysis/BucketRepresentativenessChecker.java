package br.univesp.tcc.cdccnpj.bench.analysis;

import java.util.List;
import java.util.Map;

/**
 * Verifica se os 16 buckets experimentais são representativos dos 256 (D7).
 */
public final class BucketRepresentativenessChecker {

    public static final double DEFAULT_TOLERANCE = 0.15;

    public RepresentativenessReport check(
            Map<Integer, Long> recordsPerBucket, List<Integer> sampleBucketIds, double tolerance) {
        long totalRecords = recordsPerBucket.values().stream().mapToLong(Long::longValue).sum();
        if (totalRecords == 0) {
            return new RepresentativenessReport(0, 0, 0, 0, 0, 0, true, tolerance);
        }

        double populationMean = totalRecords / 256.0;
        long sampleTotal = sampleBucketIds.stream()
                .mapToLong(id -> recordsPerBucket.getOrDefault(id, 0L))
                .sum();
        double sampleMean = sampleTotal / (double) sampleBucketIds.size();
        double relativeMeanDeviation =
                populationMean == 0 ? 0 : Math.abs(sampleMean - populationMean) / populationMean;

        double sampleShare = sampleTotal / (double) totalRecords;
        double expectedShare = sampleBucketIds.size() / 256.0;
        double shareDeviation = Math.abs(sampleShare - expectedShare) / expectedShare;

        boolean withinTolerance = relativeMeanDeviation <= tolerance && shareDeviation <= tolerance;
        return new RepresentativenessReport(
                populationMean,
                sampleMean,
                relativeMeanDeviation,
                sampleShare,
                expectedShare,
                shareDeviation,
                withinTolerance,
                tolerance);
    }
}
