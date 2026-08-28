package br.univesp.tcc.cdccnpj.ingestion.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser CSV posicional da Receita: separador {@code ;}, aspas RFC-style (CONTRATOS.md §3.1).
 */
public final class CsvLineParser {

    private CsvLineParser() {
    }

    public static List<String> parseFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                inQuotes = true;
            } else if (character == ';') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quote in CSV line");
        }

        fields.add(current.toString());
        return List.copyOf(fields);
    }
}
