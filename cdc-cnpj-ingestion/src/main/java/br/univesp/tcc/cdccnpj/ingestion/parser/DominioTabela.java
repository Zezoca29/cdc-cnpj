package br.univesp.tcc.cdccnpj.ingestion.parser;

import java.util.Locale;

public enum DominioTabela {
    CNAES("cnaes"),
    MUNICIPIOS("municipios"),
    MOTIVOS("motivos"),
    NATUREZAS("naturezas"),
    QUALIFICACOES("qualificacoes"),
    PAISES("paises");

    private final String tabela;

    DominioTabela(String tabela) {
        this.tabela = tabela;
    }

    public String tabela() {
        return tabela;
    }

    public static DominioTabela fromZipFileName(String zipFileName) {
        String base = zipFileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return switch (base.toLowerCase(Locale.ROOT)) {
            case "cnaes" -> CNAES;
            case "municipios" -> MUNICIPIOS;
            case "motivos" -> MOTIVOS;
            case "naturezas" -> NATUREZAS;
            case "qualificacoes" -> QUALIFICACOES;
            case "paises" -> PAISES;
            default -> throw new IllegalArgumentException("Unknown dominio zip: " + zipFileName);
        };
    }
}
