package br.univesp.tcc.cdccnpj.events.mapping;

import br.univesp.tcc.cdccnpj.cnpj.CnpjValidator;
import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.event.EventIdGenerator;
import br.univesp.tcc.cdccnpj.events.model.CdcEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EventMapper {

    private EventMapper() {
    }

    public static CdcEvent toEvent(ChangeRecord change, String estrategia, String runId) {
        String eventType = EventTypeResolver.resolve(change);
        Map<String, String> envelopeEntityKey = envelopeEntityKey(change);
        Map<String, String> before = projectState(change, change.before());
        Map<String, String> after = projectState(change, change.after());

        String eventId = EventIdGenerator.generate(
                        change.competenciaDe(),
                        change.competenciaPara(),
                        envelopeEntityKey,
                        eventType,
                        change.camposAlterados())
                .toString();

        return new CdcEvent(
                "1.0",
                eventId,
                eventType,
                envelopeEntityKey,
                change.competenciaDe(),
                change.competenciaPara(),
                change.camposAlterados(),
                before,
                after,
                Instant.now().toString(),
                new CdcEvent.PipelineInfo(estrategia, runId));
    }

    public static String partitionKey(ChangeRecord change) {
        if ("empresas".equals(change.tabela())
                || "estabelecimentos".equals(change.tabela())
                || "socios".equals(change.tabela())
                || "simples".equals(change.tabela())) {
            return change.entityKey().get("cnpj_basico");
        }
        return "dominio";
    }

    private static Map<String, String> envelopeEntityKey(ChangeRecord change) {
        return switch (change.tabela()) {
            case "empresas" -> Map.of("cnpj_basico", change.entityKey().get("cnpj_basico"));
            case "estabelecimentos" -> Map.of("cnpj", fullCnpj(change));
            case "socios" -> Map.of(
                    "cnpj_basico", change.entityKey().get("cnpj_basico"),
                    "socio_identity_key", change.entityKey().get("socio_identity_key"));
            case "simples" -> Map.of("cnpj_basico", change.entityKey().get("cnpj_basico"));
            default -> Map.of(
                    "tabela", change.tabela(),
                    "codigo", change.entityKey().getOrDefault("codigo", ""));
        };
    }

    private static String fullCnpj(ChangeRecord change) {
        Map<String, String> source = change.changeType() == ChangeType.DELETE ? change.before() : change.after();
        if (source.containsKey("cnpj") && source.get("cnpj") != null) {
            return source.get("cnpj");
        }
        return CnpjValidator.compose(
                change.entityKey().get("cnpj_basico"),
                change.entityKey().get("cnpj_ordem"),
                change.entityKey().get("cnpj_dv"));
    }

    private static Map<String, String> projectState(ChangeRecord change, Map<String, String> state) {
        if (state == null || state.isEmpty()) {
            return Map.of();
        }
        if (change.changeType() == ChangeType.INSERT) {
            return Map.copyOf(state);
        }
        if (change.changeType() == ChangeType.DELETE) {
            return Map.copyOf(state);
        }
        Map<String, String> projected = new LinkedHashMap<>();
        for (String field : change.camposAlterados()) {
            projected.put(field, state.get(field));
        }
        return Map.copyOf(projected);
    }
}
