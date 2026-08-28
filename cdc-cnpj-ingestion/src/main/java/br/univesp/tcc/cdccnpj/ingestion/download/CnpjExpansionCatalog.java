package br.univesp.tcc.cdccnpj.ingestion.download;

import java.util.List;

/**
 * Arquivos da Expansão (fora do Núcleo) — Socios e Simples.
 */
public final class CnpjExpansionCatalog {

    private static final List<String> SOCIOS = List.of(
            "Socios0.zip",
            "Socios1.zip",
            "Socios2.zip",
            "Socios3.zip",
            "Socios4.zip",
            "Socios5.zip",
            "Socios6.zip",
            "Socios7.zip",
            "Socios8.zip",
            "Socios9.zip");

    public static final List<String> X2_SOCIOS_FILES = List.copyOf(SOCIOS);

    public static final List<String> X3_SIMPLES_FILES = List.of("Simples.zip");

    private CnpjExpansionCatalog() {
    }
}
