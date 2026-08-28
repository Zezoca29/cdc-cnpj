package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;

/**
 * Constantes do protocolo experimental (spec §9) e Expansão X1.
 */
public final class BenchmarkConstants {

    /** Repetições por configuração; a primeira é aquecimento (JIT/cache). */
    public static final int REPETITIONS_PER_CONFIG = 5;

    public static final int WARMUP_RUNS_DISCARDED = 1;

    public static final int[] BUCKET_SCALES = {1, 4, 8, 16};

    /** Escala completa da Expansão X1 (ADR-002). */
    public static final int FULL_SCALE_BUCKET_COUNT = CnpjPartitioner.BUCKET_COUNT;

    /** Critério de aceitação H2 (spec §4). */
    public static final long H2_ACCEPTANCE_HOURS = 24;

    private BenchmarkConstants() {
    }
}
