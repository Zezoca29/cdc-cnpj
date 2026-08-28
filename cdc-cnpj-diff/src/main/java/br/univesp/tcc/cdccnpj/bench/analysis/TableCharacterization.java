package br.univesp.tcc.cdccnpj.bench.analysis;

import java.util.Map;

public record TableCharacterization(
        String competencia,
        String tabela,
        long recordCount,
        int bucketsPresent,
        Map<Integer, Long> recordsPerBucket) {
}
