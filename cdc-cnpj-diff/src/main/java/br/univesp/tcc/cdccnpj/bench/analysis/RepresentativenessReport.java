package br.univesp.tcc.cdccnpj.bench.analysis;

public record RepresentativenessReport(
        double populationMeanPerBucket,
        double sampleMeanPerBucket,
        double relativeMeanDeviation,
        double sampleShareOfRecords,
        double expectedShare,
        double shareDeviation,
        boolean withinTolerance,
        double tolerance) {
}
