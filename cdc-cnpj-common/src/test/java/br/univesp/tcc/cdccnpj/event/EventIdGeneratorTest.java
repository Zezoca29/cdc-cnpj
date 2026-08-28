package br.univesp.tcc.cdccnpj.event;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventIdGeneratorTest {

    @Test
    void namespaceMatchesContract() {
        assertEquals(UUID.fromString("1f7a95b3-e59c-5ed5-af9e-7e2d84a7a8cd"), EventIdGenerator.NAMESPACE_CDC_CNPJ);
    }

    @Test
    void generateIsDeterministic() {
        Map<String, String> entityKey = Map.of("cnpj", "12345678000195");
        List<String> fields = List.of("data_situacao_cadastral", "situacao_cadastral");
        UUID first = EventIdGenerator.generate(
                "2026-06", "2026-07", entityKey, "estabelecimento.situacao_alterada", fields);
        UUID second = EventIdGenerator.generate(
                "2026-06", "2026-07", entityKey, "estabelecimento.situacao_alterada", fields);
        assertEquals(first, second);
        assertEquals(UUID.fromString("3fbb7062-db48-5f86-a421-93f6adaf987e"), first);
    }

    @Test
    void canonicalStringOrdersEntityKeyAndFields() {
        String canonical = EventIdGenerator.buildCanonicalString(
                "2026-06",
                "2026-07",
                Map.of("cnpj", "12345678000195"),
                "estabelecimento.situacao_alterada",
                List.of("situacao_cadastral", "data_situacao_cadastral"));
        assertEquals(
                "2026-06|2026-07|cnpj=12345678000195|estabelecimento.situacao_alterada|data_situacao_cadastral,situacao_cadastral",
                canonical);
    }
}
