package br.univesp.tcc.cdccnpj.ingestion;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class IngestionConstants {

    /** Limite de rejeição por estágio (CONTRATOS.md §3.1, R4). */
    public static final double MAX_REJECTION_RATE = 0.001;

    public static final int ESTABELECIMENTOS_CSV_COLUMNS = 30;

    public static final int EMPRESAS_CSV_COLUMNS = 7;

    public static final int DOMINIO_CSV_COLUMNS = 2;

    public static final int SOCIOS_CSV_COLUMNS = 11;

    public static final int SIMPLES_CSV_COLUMNS = 7;

    public static final Charset SOURCE_CHARSET = StandardCharsets.ISO_8859_1;

    public static final String SCHEMA_VERSION = "1.0";

    private IngestionConstants() {
    }
}
