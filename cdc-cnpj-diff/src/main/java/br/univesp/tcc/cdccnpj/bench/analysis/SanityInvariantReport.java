package br.univesp.tcc.cdccnpj.bench.analysis;

import java.util.List;

public record SanityInvariantReport(
        String competenciaDe,
        String competenciaPara,
        String tabela,
        List<SanityInvariantResult> results,
        boolean allPassed) {
}
