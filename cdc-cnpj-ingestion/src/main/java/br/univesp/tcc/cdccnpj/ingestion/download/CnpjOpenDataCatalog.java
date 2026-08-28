package br.univesp.tcc.cdccnpj.ingestion.download;

import java.util.List;

/**
 * Arquivos exigidos pelo Núcleo (CONTRATOS.md §3.5) — sem Simples/Socios (Expansão).
 */
public final class CnpjOpenDataCatalog {

    private static final List<String> DOMINIO = List.of(
            "Cnaes.zip", "Municipios.zip", "Motivos.zip", "Naturezas.zip", "Qualificacoes.zip", "Paises.zip");

    private static final List<String> EMPRESAS =
            List.of("Empresas0.zip", "Empresas1.zip", "Empresas2.zip", "Empresas3.zip", "Empresas4.zip", "Empresas5.zip", "Empresas6.zip", "Empresas7.zip", "Empresas8.zip", "Empresas9.zip");

    private static final List<String> ESTABELECIMENTOS = List.of(
            "Estabelecimentos0.zip",
            "Estabelecimentos1.zip",
            "Estabelecimentos2.zip",
            "Estabelecimentos3.zip",
            "Estabelecimentos4.zip",
            "Estabelecimentos5.zip",
            "Estabelecimentos6.zip",
            "Estabelecimentos7.zip",
            "Estabelecimentos8.zip",
            "Estabelecimentos9.zip");

    public static final List<String> NUCLEO_FILES = concat(DOMINIO, EMPRESAS, ESTABELECIMENTOS);

    private CnpjOpenDataCatalog() {
    }

    @SafeVarargs
    private static List<String> concat(List<String>... groups) {
        return java.util.Arrays.stream(groups).flatMap(List::stream).toList();
    }
}
