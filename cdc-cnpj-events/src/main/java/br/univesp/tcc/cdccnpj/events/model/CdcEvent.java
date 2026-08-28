package br.univesp.tcc.cdccnpj.events.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CdcEvent(
        String schemaVersion,
        String eventId,
        String eventType,
        Map<String, String> entityKey,
        String competenciaDe,
        String competenciaPara,
        List<String> camposAlterados,
        Map<String, String> before,
        Map<String, String> after,
        String detectedAt,
        PipelineInfo pipeline) {

    public record PipelineInfo(String estrategia, String runId) {}
}
