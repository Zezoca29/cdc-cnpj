package br.univesp.tcc.cdccnpj.events.pipeline;

import br.univesp.tcc.cdccnpj.events.api.PipelineReportIo;
import br.univesp.tcc.cdccnpj.events.api.PipelineReportIo.PipelineReportSnapshot;
import br.univesp.tcc.cdccnpj.events.kafka.ChangelogConsumer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaTopicProvisioner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Marco Fase 3: Parquet canônico real → diff → quarentena → Kafka → changelog.
 *
 * <p>Pré-requisito: {@code IngestionPipelineCli} materializou {@code data/canonical/}.
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.CanonicalPipelineCli -Dexec.args="--bucket 0"}
 */
public final class CanonicalPipelineCli {

    private static final String DEFAULT_COMPETENCIA_DE = "2026-06";
    private static final String DEFAULT_COMPETENCIA_PARA = "2026-07";

    private CanonicalPipelineCli() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path canonicalRoot = optionalPath(args, "--canonical-root", projectRoot.resolve("data/canonical"));
        String competenciaDe = optionalValue(args, "--competencia-de", DEFAULT_COMPETENCIA_DE);
        String competenciaPara = optionalValue(args, "--competencia-para", DEFAULT_COMPETENCIA_PARA);
        String engine = optionalValue(args, "--engine", "E3");
        List<String> tables = parseTables(optionalValue(args, "--tables", "empresas,estabelecimentos"));
        List<Integer> buckets = parseBuckets(args);
        boolean diffOnly = hasFlag(args, "--diff-only");
        boolean truncateChangelog = hasFlag(args, "--truncate-changelog");
        Duration idleGrace = Duration.ofSeconds(
                Integer.parseInt(optionalValue(args, "--idle-seconds", defaultIdleSeconds(buckets.size()))));

        if (!Files.isDirectory(canonicalRoot)) {
            System.err.printf("Canonical root not found: %s%n", canonicalRoot);
            System.err.println("Run IngestionPipelineCli first (or point --canonical-root at data/fixtures for a dry run).");
            System.exit(1);
        }

        if (diffOnly) {
            EventPipelineRunner.PipelineReport report = EventPipelineRunner.runDiffOnly(
                    canonicalRoot, competenciaDe, competenciaPara, tables, buckets, engine);
            String runId = "canonical-diff-" + System.currentTimeMillis();
            writePipelineReport(projectRoot, runId, engine, competenciaDe, competenciaPara, tables, buckets, report);
            printReport(report, engine, canonicalRoot, competenciaDe, competenciaPara, tables, buckets, false);
            return;
        }

        String runId = "canonical-pipeline-" + System.currentTimeMillis();
        String groupId = runId + "-consumer";
        KafkaTopicProvisioner.ensureNucleoTopics();
        if (truncateChangelog) {
            PipelineJdbc.truncateChangelog();
        }

        try (KafkaEventProducer producer = new KafkaEventProducer(KafkaEventProducer.localhostDefaults());
                ChangelogConsumer consumer = new ChangelogConsumer(
                        ChangelogConsumer.localhostDefaults(groupId), PipelineJdbc.URL, PipelineJdbc.USER, PipelineJdbc.PASSWORD)) {
            consumer.subscribeAllTopics();
            consumer.awaitAssignment(Duration.ofSeconds(30));
            consumer.seekToEnd();

            EventPipelineRunner.PipelineReport report = EventPipelineRunner.runWithKafka(
                    canonicalRoot,
                    competenciaDe,
                    competenciaPara,
                    tables,
                    buckets,
                    engine,
                    runId,
                    producer,
                    consumer,
                    idleGrace);
            writePipelineReport(projectRoot, runId, engine, competenciaDe, competenciaPara, tables, buckets, report);
            printReport(report, engine, canonicalRoot, competenciaDe, competenciaPara, tables, buckets, true);
            if (report.diffChanges() > 0 && report.changelogInserted() == 0) {
                System.err.println("Published events but changelog_inserted=0 (idempotent replay or consumer timeout?)");
                System.exit(1);
            }
        }
        System.out.println("Canonical pipeline completed (run_id=" + runId + ")");
    }

    private static String defaultIdleSeconds(int bucketCount) {
        if (bucketCount <= 1) {
            return "15";
        }
        if (bucketCount <= 16) {
            return "60";
        }
        return "300";
    }

    private static void writePipelineReport(
            Path projectRoot,
            String runId,
            String engine,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            List<Integer> buckets,
            EventPipelineRunner.PipelineReport report) {
        PipelineReportSnapshot snapshot = PipelineReportSnapshot.from(
                runId, engine, competenciaDe, competenciaPara, tables, buckets.size(), report);
        PipelineReportIo.write(PipelineReportIo.defaultPath(projectRoot), snapshot);
    }

    private static void printReport(
            EventPipelineRunner.PipelineReport report,
            String engine,
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            List<Integer> buckets,
            boolean kafka) {
        System.out.printf(
                "engine=%s canonical=%s competencia=%s->%s%n",
                engine, canonicalRoot, competenciaDe, competenciaPara);
        System.out.printf("tables=%s buckets=%d%n", tables, buckets.size());
        System.out.printf("diff_changes=%d events_published=%d%n", report.diffChanges(), report.eventsPublished());
        System.out.printf(
                "quarantine cancelled=%d immediate_confirms=%d%n",
                report.quarantine().cancelledByResurrection(),
                report.quarantine().immediatelyConfirmed());
        if (kafka) {
            System.out.printf("changelog_inserted=%d%n", report.changelogInserted());
        }
    }

    static List<Integer> parseBuckets(String[] args) {
        if (hasFlag(args, "--all-buckets")) {
            return EventPipelineRunner.allBuckets();
        }
        String value = optionalValue(args, "--bucket", "0");
        if (value.contains(",")) {
            return Arrays.stream(value.split(",")).map(String::trim).map(Integer::parseInt).toList();
        }
        return List.of(Integer.parseInt(value));
    }

    static List<String> parseTables(String value) {
        List<String> tables = Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("At least one table required");
        }
        return tables;
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
}
