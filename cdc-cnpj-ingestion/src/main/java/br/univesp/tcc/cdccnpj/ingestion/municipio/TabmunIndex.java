package br.univesp.tcc.cdccnpj.ingestion.municipio;

import java.util.Map;
import java.util.Optional;

/**
 * Índice SIAFI (Receita) → IBGE carregado de tabmun.csv (CONTRATOS.md §3.4).
 */
public final class TabmunIndex {

    private final Map<String, String> siafiToIbge;

    public TabmunIndex(Map<String, String> siafiToIbge) {
        this.siafiToIbge = Map.copyOf(siafiToIbge);
    }

    public static TabmunIndex empty() {
        return new TabmunIndex(Map.of());
    }

    public Optional<String> resolveIbge(String municipioReceita) {
        if (municipioReceita == null || municipioReceita.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(siafiToIbge.get(padSiafi(municipioReceita)));
    }

    static String padSiafi(String municipioReceita) {
        String trimmed = municipioReceita.trim();
        if (trimmed.length() > 4) {
            return trimmed.substring(trimmed.length() - 4);
        }
        return "0".repeat(4 - trimmed.length()) + trimmed;
    }

    static String normalizeIbge(String ibge) {
        String digits = ibge.trim();
        if (digits.length() > 7) {
            return digits.substring(digits.length() - 7);
        }
        return "0".repeat(7 - digits.length()) + digits;
    }
}
