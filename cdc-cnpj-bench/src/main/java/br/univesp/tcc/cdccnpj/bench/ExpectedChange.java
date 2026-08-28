package br.univesp.tcc.cdccnpj.bench;

import br.univesp.tcc.cdccnpj.diff.ChangeType;

import java.util.List;
import java.util.Map;

/**
 * Mudança esperada documentada para testes de propriedade sobre fixtures sintéticas.
 */
public record ExpectedChange(
        ChangeType changeType,
        String tabela,
        Map<String, String> entityKey,
        List<String> camposAlterados) {
}
