package br.univesp.tcc.cdccnpj.bench;

import java.util.List;
import java.util.Map;

public record SyntheticEmpresa(
        String cnpjBasico,
        String razaoSocial,
        String naturezaJuridica,
        String qualificacaoResponsavel,
        String capitalSocial,
        String porte,
        String enteFederativoResponsavel) {

    Map<String, String> asMap() {
        return Map.ofEntries(
                Map.entry("cnpj_basico", cnpjBasico),
                Map.entry("razao_social", razaoSocial),
                Map.entry("natureza_juridica", naturezaJuridica),
                Map.entry("qualificacao_responsavel", qualificacaoResponsavel),
                Map.entry("capital_social", capitalSocial),
                Map.entry("porte", porte),
                Map.entry("ente_federativo_responsavel", enteFederativoResponsavel));
    }

    Map<String, String> entityKey() {
        return Map.of("cnpj_basico", cnpjBasico);
    }
}
