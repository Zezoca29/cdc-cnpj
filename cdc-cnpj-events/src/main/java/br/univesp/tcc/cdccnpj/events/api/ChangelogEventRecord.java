package br.univesp.tcc.cdccnpj.events.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ChangelogEventRecord(
        String eventId,
        String schemaVersion,
        String eventType,
        Map<String, String> entityKey,
        String competenciaDe,
        String competenciaPara,
        List<String> camposAlterados,
        Map<String, String> before,
        Map<String, String> after,
        Instant detectedAt,
        String pipelineEstrategia,
        String pipelineRunId,
        Instant consumedAt) {}
