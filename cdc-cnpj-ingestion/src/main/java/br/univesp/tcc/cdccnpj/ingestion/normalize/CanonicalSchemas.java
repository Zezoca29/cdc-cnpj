package br.univesp.tcc.cdccnpj.ingestion.normalize;

import org.apache.avro.Schema;

public final class CanonicalSchemas {

    private CanonicalSchemas() {
    }

    public static Schema estabelecimentos() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "EstabelecimentoCanonical",
                  "namespace": "br.univesp.tcc.cdccnpj.canonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "cnpj_ordem", "type": "string"},
                    {"name": "cnpj_dv", "type": "string"},
                    {"name": "cnpj", "type": "string"},
                    {"name": "identificador_matriz_filial", "type": ["null", "string"], "default": null},
                    {"name": "nome_fantasia", "type": ["null", "string"], "default": null},
                    {"name": "situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "data_situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "motivo_situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "nome_cidade_exterior", "type": ["null", "string"], "default": null},
                    {"name": "pais", "type": ["null", "string"], "default": null},
                    {"name": "data_inicio_atividade", "type": ["null", "string"], "default": null},
                    {"name": "cnae_fiscal_principal", "type": ["null", "string"], "default": null},
                    {"name": "cnae_fiscal_secundaria", "type": ["null", "string"], "default": null},
                    {"name": "tipo_logradouro", "type": ["null", "string"], "default": null},
                    {"name": "logradouro", "type": ["null", "string"], "default": null},
                    {"name": "numero", "type": ["null", "string"], "default": null},
                    {"name": "complemento", "type": ["null", "string"], "default": null},
                    {"name": "bairro", "type": ["null", "string"], "default": null},
                    {"name": "cep", "type": ["null", "string"], "default": null},
                    {"name": "uf", "type": ["null", "string"], "default": null},
                    {"name": "municipio_receita", "type": ["null", "string"], "default": null},
                    {"name": "municipio_ibge", "type": ["null", "string"], "default": null},
                    {"name": "ddd_1", "type": ["null", "string"], "default": null},
                    {"name": "telefone_1", "type": ["null", "string"], "default": null},
                    {"name": "ddd_2", "type": ["null", "string"], "default": null},
                    {"name": "telefone_2", "type": ["null", "string"], "default": null},
                    {"name": "ddd_fax", "type": ["null", "string"], "default": null},
                    {"name": "fax", "type": ["null", "string"], "default": null},
                    {"name": "correio_eletronico", "type": ["null", "string"], "default": null},
                    {"name": "situacao_especial", "type": ["null", "string"], "default": null},
                    {"name": "data_situacao_especial", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    public static Schema empresas() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "EmpresaCanonical",
                  "namespace": "br.univesp.tcc.cdccnpj.canonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "razao_social", "type": ["null", "string"], "default": null},
                    {"name": "natureza_juridica", "type": ["null", "string"], "default": null},
                    {"name": "qualificacao_responsavel", "type": ["null", "string"], "default": null},
                    {"name": "capital_social", "type": ["null", "string"], "default": null},
                    {"name": "porte", "type": ["null", "string"], "default": null},
                    {"name": "ente_federativo_responsavel", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    public static Schema dominio() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "DominioCanonical",
                  "namespace": "br.univesp.tcc.cdccnpj.canonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "codigo", "type": "string"},
                    {"name": "descricao", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    public static Schema socios() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "SocioCanonical",
                  "namespace": "br.univesp.tcc.cdccnpj.canonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "socio_identity_key", "type": "string"},
                    {"name": "identificador_socio", "type": ["null", "string"], "default": null},
                    {"name": "nome_socio", "type": ["null", "string"], "default": null},
                    {"name": "cpf_cnpj_socio", "type": ["null", "string"], "default": null},
                    {"name": "qualificacao_socio", "type": ["null", "string"], "default": null},
                    {"name": "data_entrada_sociedade", "type": ["null", "string"], "default": null},
                    {"name": "pais", "type": ["null", "string"], "default": null},
                    {"name": "representante_legal", "type": ["null", "string"], "default": null},
                    {"name": "nome_representante", "type": ["null", "string"], "default": null},
                    {"name": "qualificacao_representante_legal", "type": ["null", "string"], "default": null},
                    {"name": "faixa_etaria", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    public static Schema simples() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "SimplesCanonical",
                  "namespace": "br.univesp.tcc.cdccnpj.canonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "opcao_pelo_simples", "type": ["null", "string"], "default": null},
                    {"name": "data_opcao_simples", "type": ["null", "string"], "default": null},
                    {"name": "data_exclusao_simples", "type": ["null", "string"], "default": null},
                    {"name": "opcao_pelo_mei", "type": ["null", "string"], "default": null},
                    {"name": "data_opcao_mei", "type": ["null", "string"], "default": null},
                    {"name": "data_exclusao_mei", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }
}
