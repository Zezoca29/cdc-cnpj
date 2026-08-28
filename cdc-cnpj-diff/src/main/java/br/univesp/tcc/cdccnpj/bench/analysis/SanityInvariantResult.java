package br.univesp.tcc.cdccnpj.bench.analysis;

public record SanityInvariantResult(String name, long expected, long actual, long delta, boolean passed) {
}
