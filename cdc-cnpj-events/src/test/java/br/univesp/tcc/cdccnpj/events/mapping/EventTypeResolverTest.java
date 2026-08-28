package br.univesp.tcc.cdccnpj.events.mapping;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTypeResolverTest {

    @Test
    void resolvesEmpresaLifecycle() {
        assertEquals("empresa.criada", EventTypeResolver.resolve(change("empresas", ChangeType.INSERT, List.of())));
        assertEquals("empresa.encerrada", EventTypeResolver.resolve(change("empresas", ChangeType.DELETE, List.of())));
        assertEquals(
                "empresa.alterada",
                EventTypeResolver.resolve(change("empresas", ChangeType.UPDATE, List.of("capital_social"))));
    }

    @Test
    void resolvesEstabelecimentoPrecedence() {
        assertEquals(
                "estabelecimento.situacao_alterada",
                EventTypeResolver.resolve(change(
                        "estabelecimentos",
                        ChangeType.UPDATE,
                        List.of("data_situacao_cadastral", "motivo_situacao_cadastral", "situacao_cadastral"))));
        assertEquals(
                "estabelecimento.endereco_alterado",
                EventTypeResolver.resolve(change("estabelecimentos", ChangeType.UPDATE, List.of("logradouro"))));
        assertEquals(
                "estabelecimento.cnae_alterado",
                EventTypeResolver.resolve(change("estabelecimentos", ChangeType.UPDATE, List.of("cnae_fiscal_principal"))));
        assertEquals(
                "estabelecimento.alterado",
                EventTypeResolver.resolve(change(
                        "estabelecimentos", ChangeType.UPDATE, List.of("ddd_1", "nome_fantasia"))));
    }

    @Test
    void resolvesSocioLifecycle() {
        assertEquals("socio.incluido", EventTypeResolver.resolve(change("socios", ChangeType.INSERT, List.of())));
        assertEquals("socio.excluido", EventTypeResolver.resolve(change("socios", ChangeType.DELETE, List.of())));
        assertEquals(
                "socio.alterado",
                EventTypeResolver.resolve(change("socios", ChangeType.UPDATE, List.of("qualificacao_socio"))));
    }

    @Test
    void resolvesSimplesAsOpcaoAlterada() {
        assertEquals(
                "simples.opcao_alterada",
                EventTypeResolver.resolve(change("simples", ChangeType.INSERT, List.of())));
        assertEquals(
                "simples.opcao_alterada",
                EventTypeResolver.resolve(change("simples", ChangeType.UPDATE, List.of("opcao_pelo_simples"))));
        assertEquals(
                "simples.opcao_alterada",
                EventTypeResolver.resolve(change("simples", ChangeType.DELETE, List.of())));
    }

    @Test
    void deleteEstabelecimentoWithSituacao08IsSituacaoAlterada() {
        ChangeRecord deleted = new ChangeRecord(
                ChangeType.DELETE,
                "estabelecimentos",
                0,
                Map.of("cnpj_basico", "12345678", "cnpj_ordem", "0001", "cnpj_dv", "95"),
                Map.of("situacao_cadastral", "08"),
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");
        assertEquals("estabelecimento.situacao_alterada", EventTypeResolver.resolve(deleted));
    }

    private static ChangeRecord change(String tabela, ChangeType changeType, List<String> campos) {
        return new ChangeRecord(
                changeType,
                tabela,
                0,
                Map.of("cnpj_basico", "12345678"),
                Map.of(),
                Map.of(),
                campos,
                "2026-06",
                "2026-07");
    }
}
