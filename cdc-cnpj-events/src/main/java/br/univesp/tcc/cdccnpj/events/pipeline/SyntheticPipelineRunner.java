package br.univesp.tcc.cdccnpj.events.pipeline;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticFixtures;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.events.kafka.ChangelogConsumer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Marco S2–S3: fixtures sintéticas → diff → quarentena → Kafka → changelog (bucket 000).
 */
public final class SyntheticPipelineRunner {

    public record SyntheticPipelineReport(
            int diffChanges,
            int eventsPublished,
            int changelogInserted,
            br.univesp.tcc.cdccnpj.events.quarantine.QuarantineResult quarantine) {
    }

    private SyntheticPipelineRunner() {
    }

    public static List<ChangeRecord> diffPrimaryBucket(Path fixturesRoot, String engineName) {
        return EventPipelineRunner.diff(
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                EventPipelineRunner.NUCLEO_TABLES,
                List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET),
                EventPipelineRunner.resolveEngine(engineName));
    }

    public static SyntheticPipelineReport runDiffOnly(Path fixturesRoot, String engineName) {
        return toSynthetic(EventPipelineRunner.runDiffOnly(
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                EventPipelineRunner.NUCLEO_TABLES,
                List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET),
                engineName));
    }

    public static SyntheticPipelineReport runWithKafka(
            Path fixturesRoot,
            String engineName,
            String runId,
            KafkaEventProducer producer,
            ChangelogConsumer consumer) {
        return toSynthetic(EventPipelineRunner.runWithKafka(
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                EventPipelineRunner.NUCLEO_TABLES,
                List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET),
                engineName,
                runId,
                producer,
                consumer,
                Duration.ofSeconds(10)));
    }

    public static void writeFixtures(Path fixturesRoot) {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);
    }

    private static SyntheticPipelineReport toSynthetic(EventPipelineRunner.PipelineReport report) {
        return new SyntheticPipelineReport(
                report.diffChanges(),
                report.eventsPublished(),
                report.changelogInserted(),
                report.quarantine());
    }
}
