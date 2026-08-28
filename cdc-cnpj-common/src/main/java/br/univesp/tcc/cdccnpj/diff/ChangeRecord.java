package br.univesp.tcc.cdccnpj.diff;

import java.util.List;
import java.util.Map;

/**
 * Registro interno de diferença entre snapshots (CONTRATOS.md §5.2).
 */
public record ChangeRecord(
        ChangeType changeType,
        String tabela,
        int bucketId,
        Map<String, String> entityKey,
        Map<String, String> before,
        Map<String, String> after,
        List<String> camposAlterados,
        String competenciaDe,
        String competenciaPara) {
}
