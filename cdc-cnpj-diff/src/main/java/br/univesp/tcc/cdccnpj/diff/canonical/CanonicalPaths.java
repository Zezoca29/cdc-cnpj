package br.univesp.tcc.cdccnpj.diff.canonical;

import java.nio.file.Path;

public final class CanonicalPaths {

    private CanonicalPaths() {
    }

    public static Path bucketParquet(Path canonicalRoot, String competencia, String tabela, int bucketId) {
        return canonicalRoot
                .resolve("competencia=" + competencia)
                .resolve("tabela=" + tabela)
                .resolve("bucket=" + String.format("%03d", bucketId))
                .resolve("part-0.parquet");
    }
}
