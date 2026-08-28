package br.univesp.tcc.cdccnpj.events;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticFixtures;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.e1.PostgresDiffEngine;
import br.univesp.tcc.cdccnpj.events.kafka.ChangelogConsumer;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaTopicProvisioner;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;
import br.univesp.tcc.cdccnpj.events.quarantine.QuarantineEventPublisher;
import br.univesp.tcc.cdccnpj.events.quarantine.QuarantineResult;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EventsPipelineIT {

    private static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj";
    private static final String JDBC_USER = "cdc";
    private static final String JDBC_PASSWORD = "cdc";
    private static final String KAFKA_BOOTSTRAP = "localhost:9092";

    private static boolean infraAvailable;

    @BeforeAll
    static void checkInfra() {
        infraAvailable = postgresAvailable() && kafkaAvailable();
    }

    @Test
    void e1DiffToKafkaToChangelog(@TempDir Path fixturesRoot) throws Exception {
        assumeTrue(infraAvailable, "Postgres/Kafka not available — run docker compose up -d");

        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);

        PostgresDiffEngine engine = PostgresDiffEngine.localhostDefaults();
        List<ChangeRecord> empresaChanges = engine.diff(
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "empresas",
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET)
                .toList();
        List<ChangeRecord> estabChanges = engine.diff(
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "estabelecimentos",
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET)
                .toList();

        List<ChangeRecord> allChanges = new java.util.ArrayList<>(empresaChanges);
        allChanges.addAll(estabChanges);

        String runId = "events-pipeline-it-" + System.nanoTime();
        String groupId = runId + "-consumer";
        KafkaTopicProvisioner.ensureNucleoTopics(KAFKA_BOOTSTRAP);

        try (KafkaEventProducer producer = new KafkaEventProducer(KafkaEventProducer.localhostDefaults());
                ChangelogConsumer consumer = new ChangelogConsumer(
                        ChangelogConsumer.localhostDefaults(groupId), JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            consumer.subscribeAllTopics();
            waitForAssignment(consumer);
            consumer.seekToEnd();

            QuarantineResult published = QuarantineEventPublisher.publishAll(producer, allChanges, "E1", runId);
            producer.flush();
            assertEquals(9, published.toPublish().size());

            int inserted = consumer.consumeUntilIdle(Duration.ofSeconds(2), Duration.ofSeconds(6));
            assertEquals(9, inserted);
            assertEquals(9, countEventsForRun(runId));

            QuarantineEventPublisher.publishAll(producer, allChanges, "E1", runId);
            producer.flush();

            int duplicateReplay = consumer.consumeUntilIdle(Duration.ofSeconds(2), Duration.ofSeconds(6));
            assertEquals(0, duplicateReplay, "Idempotent consumer must ignore duplicate event_id");
            assertEquals(9, countEventsForRun(runId));
        }
    }

    private static void waitForAssignment(ChangelogConsumer consumer) {
        consumer.awaitAssignment(Duration.ofSeconds(30));
    }

    private static int countEventsForRun(String runId) throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
                var statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM changelog.events WHERE pipeline_run_id = ?")) {
            statement.setString(1, runId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static boolean postgresAvailable() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean kafkaAvailable() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        try (AdminClient admin = AdminClient.create(properties)) {
            admin.describeCluster().clusterId().get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
