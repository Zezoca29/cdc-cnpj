package br.univesp.tcc.cdccnpj.events.pipeline;

import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.events.kafka.ChangelogConsumer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaTopicProvisioner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * CLI do marco ponta-a-ponta sintético (bucket 000).
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.SyntheticPipelineCli}
 */
public final class SyntheticPipelineCli {

    private SyntheticPipelineCli() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path fixturesRoot = optionalPath(args, "--fixtures", projectRoot.resolve("data/fixtures"));
        String engine = optionalValue(args, "--engine", "E3");
        boolean diffOnly = hasFlag(args, "--diff-only");
        boolean rewriteFixtures = hasFlag(args, "--rewrite-fixtures");

        if (rewriteFixtures) {
            deleteRecursively(fixturesRoot);
        }
        if (rewriteFixtures || !Files.isDirectory(fixturesRoot)) {
            Files.createDirectories(fixturesRoot);
            SyntheticPipelineRunner.writeFixtures(fixturesRoot);
            System.out.println("fixtures written to " + fixturesRoot);
        }

        if (diffOnly) {
            SyntheticPipelineRunner.SyntheticPipelineReport report =
                    SyntheticPipelineRunner.runDiffOnly(fixturesRoot, engine);
            printReport(report, engine, fixturesRoot, false);
            if (report.diffChanges() != 9 || report.eventsPublished() != 9) {
                System.exit(1);
            }
            return;
        }

        String runId = "synthetic-pipeline-" + System.currentTimeMillis();
        String groupId = runId + "-consumer";
        KafkaTopicProvisioner.ensureNucleoTopics();
        PipelineJdbc.truncateChangelog();
        try (KafkaEventProducer producer = new KafkaEventProducer(KafkaEventProducer.localhostDefaults());
                ChangelogConsumer consumer = new ChangelogConsumer(
                        ChangelogConsumer.localhostDefaults(groupId), PipelineJdbc.URL, PipelineJdbc.USER, PipelineJdbc.PASSWORD)) {
            consumer.subscribeAllTopics();
            consumer.awaitAssignment(Duration.ofSeconds(30));
            consumer.seekToEnd();

            SyntheticPipelineRunner.SyntheticPipelineReport report =
                    SyntheticPipelineRunner.runWithKafka(fixturesRoot, engine, runId, producer, consumer);
            printReport(report, engine, fixturesRoot, true);
            if (report.diffChanges() != 9 || report.eventsPublished() != 9 || report.changelogInserted() != 9) {
                System.exit(1);
            }
        }
        System.out.println("Synthetic E2E pipeline completed (run_id=" + runId + ")");
    }

    private static void printReport(
            SyntheticPipelineRunner.SyntheticPipelineReport report,
            String engine,
            Path fixturesRoot,
            boolean kafka) {
        System.out.printf("engine=%s bucket=%03d fixtures=%s%n", engine, SyntheticSnapshotGenerator.PRIMARY_BUCKET, fixturesRoot);
        System.out.printf("diff_changes=%d events_published=%d%n", report.diffChanges(), report.eventsPublished());
        System.out.printf(
                "quarantine cancelled=%d immediate_confirms=%d%n",
                report.quarantine().cancelledByResurrection(),
                report.quarantine().immediatelyConfirmed());
        if (kafka) {
            System.out.printf("changelog_inserted=%d%n", report.changelogInserted());
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Path optionalPath(String[] args, String flag, Path defaultPath) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return Paths.get(args[i + 1]);
            }
        }
        return defaultPath;
    }

    private static String optionalValue(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static void deleteRecursively(Path root) throws java.io.IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
        }
    }
}
