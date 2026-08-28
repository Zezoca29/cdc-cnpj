package br.univesp.tcc.cdccnpj.ingestion.parser;

import java.util.Locale;
import java.util.zip.ZipEntry;

/**
 * Identifica entradas de dados nos ZIPs da Receita Federal (layout aberto CNPJ).
 * Nomes reais: {@code F.K03200$Z.D60509.CNAECSV}, {@code K3241...EMPRECSV}, {@code ...ESTABELE}.
 */
public final class ReceitaFederalZipEntry {

    private ReceitaFederalZipEntry() {
    }

    public static boolean isDataFile(ZipEntry entry) {
        if (entry.isDirectory()) {
            return false;
        }
        String name = entry.getName().toLowerCase(Locale.ROOT);
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.endsWith(".csv") || name.endsWith("csv") || name.endsWith("estabele");
    }
}
