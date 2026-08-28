package br.univesp.tcc.cdccnpj.diff.canonical;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TableDefinitions {

    private static final Set<String> PROVENANCE = Set.of(
            "competencia", "arquivo_origem", "sha256_origem", "linha_origem");

    private TableDefinitions() {
    }

    public static List<String> naturalKeyColumns(String tabela) {
        return switch (tabela) {
            case "empresas" -> List.of("cnpj_basico");
            case "estabelecimentos" -> List.of("cnpj_basico", "cnpj_ordem", "cnpj_dv");
            case "socios" -> List.of("cnpj_basico", "socio_identity_key");
            case "simples" -> List.of("cnpj_basico");
            default -> throw new IllegalArgumentException("Unsupported table: " + tabela);
        };
    }

    public static List<String> naiveNaturalKeyColumns(String tabela) {
        if ("socios".equals(tabela)) {
            return List.of("cnpj_basico", "identificador_socio", "cpf_cnpj_socio");
        }
        return naturalKeyColumns(tabela);
    }

    public static List<String> comparableColumns(String tabela) {
        return switch (tabela) {
            case "empresas" -> List.of(
                    "cnpj_basico",
                    "razao_social",
                    "natureza_juridica",
                    "qualificacao_responsavel",
                    "capital_social",
                    "porte",
                    "ente_federativo_responsavel");
            case "estabelecimentos" -> List.of(
                    "cnpj_basico",
                    "cnpj_ordem",
                    "cnpj_dv",
                    "cnpj",
                    "identificador_matriz_filial",
                    "nome_fantasia",
                    "situacao_cadastral",
                    "data_situacao_cadastral",
                    "motivo_situacao_cadastral",
                    "nome_cidade_exterior",
                    "pais",
                    "data_inicio_atividade",
                    "cnae_fiscal_principal",
                    "cnae_fiscal_secundaria",
                    "tipo_logradouro",
                    "logradouro",
                    "numero",
                    "complemento",
                    "bairro",
                    "cep",
                    "uf",
                    "municipio_receita",
                    "municipio_ibge",
                    "ddd_1",
                    "telefone_1",
                    "ddd_2",
                    "telefone_2",
                    "ddd_fax",
                    "fax",
                    "correio_eletronico",
                    "situacao_especial",
                    "data_situacao_especial");
            case "socios" -> List.of(
                    "cnpj_basico",
                    "identificador_socio",
                    "nome_socio",
                    "cpf_cnpj_socio",
                    "qualificacao_socio",
                    "data_entrada_sociedade",
                    "pais",
                    "representante_legal",
                    "nome_representante",
                    "qualificacao_representante_legal",
                    "faixa_etaria");
            case "simples" -> List.of(
                    "cnpj_basico",
                    "opcao_pelo_simples",
                    "data_opcao_simples",
                    "data_exclusao_simples",
                    "opcao_pelo_mei",
                    "data_opcao_mei",
                    "data_exclusao_mei");
            default -> throw new IllegalArgumentException("Unsupported table: " + tabela);
        };
    }

    public static boolean isProvenance(String column) {
        return PROVENANCE.contains(column);
    }

    public static Map<String, String> entityKey(String tabela, Map<String, String> row) {
        return entityKeyFromColumns(naturalKeyColumns(tabela), row);
    }

    public static Map<String, String> entityKeyFromColumns(List<String> keyColumns, Map<String, String> row) {
        return keyColumns.stream().collect(java.util.stream.Collectors.toMap(k -> k, row::get));
    }

    public static String entityKeyToken(String tabela, Map<String, String> row) {
        return entityKeyTokenFromColumns(naturalKeyColumns(tabela), row);
    }

    public static String entityKeyTokenFromColumns(List<String> keyColumns, Map<String, String> row) {
        return String.join("|", keyColumns.stream().map(row::get).toList());
    }
}
