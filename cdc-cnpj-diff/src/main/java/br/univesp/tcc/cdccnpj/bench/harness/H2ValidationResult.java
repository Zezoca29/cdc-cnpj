package br.univesp.tcc.cdccnpj.bench.harness;

public record H2ValidationResult(
        boolean passed,
        double measuredHours,
        double acceptanceHours,
        long totalEventCount,
        String summary) {
}
