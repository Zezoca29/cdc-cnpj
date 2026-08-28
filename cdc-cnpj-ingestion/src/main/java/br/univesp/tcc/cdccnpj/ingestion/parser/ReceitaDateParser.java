package br.univesp.tcc.cdccnpj.ingestion.parser;

import java.util.Map;

final class ReceitaDateParser {

    private ReceitaDateParser() {
    }

    static String parse(String raw, String columnName, Map<String, Long> sentinelCounters) {
        if (raw == null || raw.isBlank() || "0".equals(raw) || "00000000".equals(raw)) {
            return null;
        }
        if (raw.length() != 8 || !raw.chars().allMatch(Character::isDigit)) {
            incrementSentinel(columnName, sentinelCounters);
            return null;
        }
        int year = Integer.parseInt(raw.substring(0, 4));
        int month = Integer.parseInt(raw.substring(4, 6));
        int day = Integer.parseInt(raw.substring(6, 8));
        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1900) {
            incrementSentinel(columnName, sentinelCounters);
            return null;
        }
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    private static void incrementSentinel(String columnName, Map<String, Long> sentinelCounters) {
        sentinelCounters.merge("sentinel_dates_" + columnName, 1L, Long::sum);
    }
}
