package br.univesp.tcc.cdccnpj.bench.analysis;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.event.EventIdGenerator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Amostra eventos para validação externa com semente registrada (spec §10.2, D5).
 */
public final class ExternalValidationSampler {

    public record SampledEvent(
            String eventId,
            String eventType,
            String tabela,
            int bucketId,
            String entityKey,
            String camposAlterados,
            String competenciaDe,
            String competenciaPara) {
    }

    public List<SampledEvent> sample(
            DiffEngine engine,
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tabelas,
            List<Integer> bucketIds,
            int sampleSize,
            long seed) {
        List<ChangeRecord> allChanges = new ArrayList<>();
        for (String tabela : tabelas) {
            for (int bucketId : bucketIds) {
                allChanges.addAll(engine.diff(canonicalRoot, competenciaDe, competenciaPara, tabela, bucketId)
                        .toList());
            }
        }

        List<ChangeRecord> shuffled = new ArrayList<>(allChanges);
        shuffled.sort(Comparator.comparing(change -> stableKey(change, competenciaDe, competenciaPara)));
        Random random = new Random(seed);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ChangeRecord tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }

        int limit = Math.min(sampleSize, shuffled.size());
        List<SampledEvent> sampled = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            ChangeRecord change = shuffled.get(i);
            String eventType = resolveEventType(change);
            String eventId = EventIdGenerator.generate(
                    competenciaDe,
                    competenciaPara,
                    change.entityKey(),
                    eventType,
                    change.camposAlterados()).toString();
            sampled.add(new SampledEvent(
                    eventId,
                    eventType,
                    change.tabela(),
                    change.bucketId(),
                    change.entityKey().toString(),
                    String.join("|", change.camposAlterados()),
                    competenciaDe,
                    competenciaPara));
        }
        return List.copyOf(sampled);
    }

    public static void writeCsv(Path output, long seed, List<SampledEvent> events) throws Exception {
        Files.createDirectories(output.getParent());
        StringBuilder body = new StringBuilder(
                "seed,event_id,event_type,tabela,bucket_id,entity_key,campos_alterados,competencia_de,competencia_para,verified,notes\n");
        for (SampledEvent event : events) {
            body.append(seed).append(',');
            body.append(event.eventId()).append(',');
            body.append(event.eventType()).append(',');
            body.append(event.tabela()).append(',');
            body.append(event.bucketId()).append(',');
            body.append(escape(event.entityKey())).append(',');
            body.append(escape(event.camposAlterados())).append(',');
            body.append(event.competenciaDe()).append(',');
            body.append(event.competenciaPara()).append(',');
            body.append(',').append('\n');
        }
        Files.writeString(output, body.toString(), StandardCharsets.UTF_8);
    }

    private static String resolveEventType(ChangeRecord change) {
        return switch (change.tabela()) {
            case "empresas" -> switch (change.changeType()) {
                case INSERT -> "empresa.criada";
                case DELETE -> "empresa.encerrada";
                case UPDATE -> "empresa.alterada";
            };
            case "estabelecimentos" -> switch (change.changeType()) {
                case INSERT -> "estabelecimento.criado";
                case DELETE -> "estabelecimento.alterado";
                case UPDATE -> "estabelecimento.alterado";
            };
            default -> "dominio.tabela_alterada";
        };
    }

    private static String stableKey(ChangeRecord change, String competenciaDe, String competenciaPara) {
        String eventType = resolveEventType(change);
        return EventIdGenerator.generate(
                        competenciaDe, competenciaPara, change.entityKey(), eventType, change.camposAlterados())
                .toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
