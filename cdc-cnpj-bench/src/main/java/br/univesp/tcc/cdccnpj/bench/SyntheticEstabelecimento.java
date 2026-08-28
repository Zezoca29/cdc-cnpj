package br.univesp.tcc.cdccnpj.bench;

import br.univesp.tcc.cdccnpj.cnpj.CnpjValidator;

import java.util.LinkedHashMap;
import java.util.Map;

public record SyntheticEstabelecimento(
        String cnpjBasico,
        String cnpjOrdem,
        String cnpjDv,
        String identificadorMatrizFilial,
        String nomeFantasia,
        String situacaoCadastral,
        String dataSituacaoCadastral,
        String motivoSituacaoCadastral,
        String nomeCidadeExterior,
        String pais,
        String dataInicioAtividade,
        String cnaeFiscalPrincipal,
        String cnaeFiscalSecundaria,
        String tipoLogradouro,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cep,
        String uf,
        String municipioReceita,
        String municipioIbge,
        String ddd1,
        String telefone1,
        String ddd2,
        String telefone2,
        String dddFax,
        String fax,
        String correioEletronico,
        String situacaoEspecial,
        String dataSituacaoEspecial) {

    public String cnpj() {
        return CnpjValidator.compose(cnpjBasico, cnpjOrdem, cnpjDv);
    }

    Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("cnpj_basico", cnpjBasico);
        map.put("cnpj_ordem", cnpjOrdem);
        map.put("cnpj_dv", cnpjDv);
        map.put("cnpj", cnpj());
        map.put("identificador_matriz_filial", identificadorMatrizFilial);
        map.put("nome_fantasia", nomeFantasia);
        map.put("situacao_cadastral", situacaoCadastral);
        map.put("data_situacao_cadastral", dataSituacaoCadastral);
        map.put("motivo_situacao_cadastral", motivoSituacaoCadastral);
        map.put("nome_cidade_exterior", nomeCidadeExterior);
        map.put("pais", pais);
        map.put("data_inicio_atividade", dataInicioAtividade);
        map.put("cnae_fiscal_principal", cnaeFiscalPrincipal);
        map.put("cnae_fiscal_secundaria", cnaeFiscalSecundaria);
        map.put("tipo_logradouro", tipoLogradouro);
        map.put("logradouro", logradouro);
        map.put("numero", numero);
        map.put("complemento", complemento);
        map.put("bairro", bairro);
        map.put("cep", cep);
        map.put("uf", uf);
        map.put("municipio_receita", municipioReceita);
        map.put("municipio_ibge", municipioIbge);
        map.put("ddd_1", ddd1);
        map.put("telefone_1", telefone1);
        map.put("ddd_2", ddd2);
        map.put("telefone_2", telefone2);
        map.put("ddd_fax", dddFax);
        map.put("fax", fax);
        map.put("correio_eletronico", correioEletronico);
        map.put("situacao_especial", situacaoEspecial);
        map.put("data_situacao_especial", dataSituacaoEspecial);
        return Map.copyOf(map);
    }

    Map<String, String> entityKey() {
        return Map.of(
                "cnpj_basico", cnpjBasico,
                "cnpj_ordem", cnpjOrdem,
                "cnpj_dv", cnpjDv);
    }

    Map<String, String> entityKeyCnpjCompleto() {
        return Map.of("cnpj", cnpj());
    }
}
