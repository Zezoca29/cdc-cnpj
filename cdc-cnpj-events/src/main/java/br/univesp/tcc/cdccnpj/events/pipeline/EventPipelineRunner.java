package br.univesp.tcc.cdccnpj.events.pipeline;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.e1.PostgresDiffEngine;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;
import br.univesp.tcc.cdccnpj.events.kafka.ChangelogConsumer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;
import br.univesp.tcc.cdccnpj.events.quarantine.QuarantineEventPublisher;
import br.univesp.tcc.cdccnpj.events.quarantine.QuarantineResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Diff → quarentena → Kafka → changelog sobre Parquet canônico (sintético ou real).
 */
public final class EventPipelineRunner {

    public static final int BUCKET_COUNT = 256;
    public static final List<String> NUCLEO_TABLES = List.of("empresas", "estabelecimentos");

    public record PipelineReport(
            int diffChanges,
            int eventsPublished,
            int changelogInserted,
            QuarantineResult quarantine) {
    }

    private EventPipelineRunner() {
    }

    public static List<Integer> allBuckets() {
        return IntStream.range(0, BUCKET_COUNT).boxed().toList();
    }

    public static List<ChangeRecord> diff(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            List<Integer> buckets,
            DiffEngine engine) {
        List<ChangeRecord> changes = new ArrayList<>();
        for (int bucket : buckets) {
            for (String tabela : tables) {
                changes.addAll(engine.diff(canonicalRoot, competenciaDe, competenciaPara, tabela, bucket)
                        .toList());
            }
        }
        return List.copyOf(changes);
    }

    public static PipelineReport runDiffOnly(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            List<Integer> buckets,
            String engineName) {
        List<ChangeRecord> changes = diff(
                canonicalRoot, competenciaDe, competenciaPara, tables, buckets, resolveEngine(engineName));
        QuarantineResult quarantine = QuarantineResult.acceptAndFlush(changes);
        return new PipelineReport(changes.size(), quarantine.toPublish().size(), 0, quarantine);
    }

    public static PipelineReport runWithKafka(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            List<Integer> buckets,
            String engineName,
            String runId,
            KafkaEventProducer producer,
            ChangelogConsumer consumer,
            Duration idleGrace) {
        List<ChangeRecord> changes = diff(
                canonicalRoot, competenciaDe, competenciaPara, tables, buckets, resolveEngine(engineName));
        QuarantineResult quarantine = QuarantineEventPublisher.publishAll(producer, changes, engineName, runId);
        producer.flush();
        int inserted = consumer.consumeUntilIdle(Duration.ofSeconds(2), idleGrace);
        return new PipelineReport(changes.size(), quarantine.toPublish().size(), inserted, quarantine);
    }

    public static DiffEngine resolveEngine(String engineName) {
        if ("E1".equalsIgnoreCase(engineName)) {
            return PostgresDiffEngine.localhostDefaults();
        }
        if ("E3".equalsIgnoreCase(engineName)) {
            return new HashDiffEngine();
        }
        throw new IllegalArgumentException("Unsupported engine: " + engineName + " (use E1 or E3)");
    }
}
