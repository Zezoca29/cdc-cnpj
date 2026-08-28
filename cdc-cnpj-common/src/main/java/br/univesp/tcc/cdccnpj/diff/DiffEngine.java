package br.univesp.tcc.cdccnpj.diff;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Interface comum dos motores E1 e E3 (CONTRATOS.md §5.1).
 */
public interface DiffEngine {

    Stream<ChangeRecord> diff(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            int bucketId);
}
