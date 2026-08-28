package br.univesp.tcc.cdccnpj.events.mapping;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.event.EventIdGenerator;
import br.univesp.tcc.cdccnpj.events.model.CdcEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMapperTest {

    @Test
    void mapsUpdateWithDeterministicEventId() {
        ChangeRecord change = new ChangeRecord(
                ChangeType.UPDATE,
                "empresas",
                0,
                Map.of("cnpj_basico", "12345678"),
                Map.of("capital_social", "1000,00"),
                Map.of("capital_social", "2500,00"),
                List.of("capital_social"),
                "2026-06",
                "2026-07");

        CdcEvent event = EventMapper.toEvent(change, "E1", "run-1");

        assertEquals("1.0", event.schemaVersion());
        assertEquals("empresa.alterada", event.eventType());
        assertEquals(Map.of("cnpj_basico", "12345678"), event.entityKey());
        assertEquals(Map.of("capital_social", "1000,00"), event.before());
        assertEquals(Map.of("capital_social", "2500,00"), event.after());
        assertEquals("12345678", EventMapper.partitionKey(change));

        UUID expected = EventIdGenerator.generate(
                "2026-06",
                "2026-07",
                Map.of("cnpj_basico", "12345678"),
                "empresa.alterada",
                List.of("capital_social"));
        assertEquals(expected.toString(), event.eventId());
    }

    @Test
    void mapsEstabelecimentoEntityKeyWithFullCnpj() {
        ChangeRecord change = new ChangeRecord(
                ChangeType.UPDATE,
                "estabelecimentos",
                0,
                Map.of("cnpj_basico", "12345678", "cnpj_ordem", "0001", "cnpj_dv", "95"),
                Map.of("logradouro", "Rua A"),
                Map.of("logradouro", "Avenida Nova"),
                List.of("logradouro"),
                "2026-06",
                "2026-07");

        CdcEvent event = EventMapper.toEvent(change, "E1", "run-1");

        assertEquals(Map.of("cnpj", "12345678000195"), event.entityKey());
        assertTrue(event.eventType().startsWith("estabelecimento."));
    }
}
