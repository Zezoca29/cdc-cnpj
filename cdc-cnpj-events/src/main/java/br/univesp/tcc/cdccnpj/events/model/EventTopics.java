package br.univesp.tcc.cdccnpj.events.model;

import java.util.Set;

public final class EventTopics {

    public static final String EMPRESAS = "cnpj.empresas";
    public static final String ESTABELECIMENTOS = "cnpj.estabelecimentos";
    public static final String SOCIOS = "cnpj.socios";
    public static final String SIMPLES = "cnpj.simples";
    public static final String DOMINIO = "cnpj.dominio";

    private static final Set<String> SITUACAO_FIELDS = Set.of(
            "situacao_cadastral",
            "data_situacao_cadastral",
            "motivo_situacao_cadastral",
            "situacao_especial",
            "data_situacao_especial");
    private static final Set<String> ENDERECO_FIELDS = Set.of(
            "tipo_logradouro",
            "logradouro",
            "numero",
            "complemento",
            "bairro",
            "cep",
            "uf",
            "municipio_receita",
            "municipio_ibge",
            "nome_cidade_exterior",
            "pais");
    private static final Set<String> CNAE_FIELDS = Set.of("cnae_fiscal_principal", "cnae_fiscal_secundaria");

    private EventTopics() {
    }

    public static String topicForTable(String tabela) {
        return switch (tabela) {
            case "empresas" -> EMPRESAS;
            case "estabelecimentos" -> ESTABELECIMENTOS;
            case "socios" -> SOCIOS;
            case "simples" -> SIMPLES;
            default -> DOMINIO;
        };
    }

    public static boolean isSubset(Set<String> group, java.util.Collection<String> fields) {
        return group.containsAll(fields);
    }

    public static Set<String> situacaoFields() {
        return SITUACAO_FIELDS;
    }

    public static Set<String> enderecoFields() {
        return ENDERECO_FIELDS;
    }

    public static Set<String> cnaeFields() {
        return CNAE_FIELDS;
    }
}
