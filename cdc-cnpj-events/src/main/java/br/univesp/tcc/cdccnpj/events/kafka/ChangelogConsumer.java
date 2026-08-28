package br.univesp.tcc.cdccnpj.events.kafka;

import br.univesp.tcc.cdccnpj.events.model.CdcEvent;
import br.univesp.tcc.cdccnpj.events.model.EventTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public final class ChangelogConsumer implements AutoCloseable {

    private static final String INSERT_SQL =
            """
            INSERT INTO changelog.events (
                event_id, schema_version, event_type, entity_key,
                competencia_de, competencia_para, campos_alterados,
                before_state, after_state, detected_at,
                pipeline_estrategia, pipeline_run_id
            ) VALUES (
                ?::uuid, ?, ?, ?::jsonb,
                ?, ?, ?::jsonb,
                ?::jsonb, ?::jsonb, ?::timestamptz,
                ?, ?
            )
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    public ChangelogConsumer(
            Properties consumerProperties, String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.consumer = new KafkaConsumer<>(consumerProperties);
        this.objectMapper = EventJsonMapper.create();
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    public static Properties localhostDefaults(String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return properties;
    }

    public void subscribeAllTopics() {
        consumer.subscribe(List.of(
                EventTopics.EMPRESAS,
                EventTopics.ESTABELECIMENTOS,
                EventTopics.DOMINIO,
                EventTopics.SOCIOS,
                EventTopics.SIMPLES));
    }

    public void awaitAssignment(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            consumer.poll(Duration.ofMillis(250));
            if (!consumer.assignment().isEmpty()) {
                return;
            }
        }
        throw new IllegalStateException("Consumer has no partition assignment after " + timeout);
    }

    public void subscribe(Collection<String> topics) {
        consumer.subscribe(topics);
    }

    public boolean hasAssignment() {
        return !consumer.assignment().isEmpty();
    }

    public void seekToEnd() {
        Set<TopicPartition> assignment = consumer.assignment();
        if (assignment.isEmpty()) {
            throw new IllegalStateException("Consumer has no partition assignment");
        }
        consumer.seekToEnd(assignment);
        consumer.poll(Duration.ZERO);
    }

    public int consumeOnce(Duration timeout) {
        ConsumerRecords<String, String> records = consumer.poll(timeout);
        int inserted = 0;
        for (ConsumerRecord<String, String> record : records) {
            if (persist(record.value())) {
                inserted++;
            }
        }
        if (!records.isEmpty()) {
            consumer.commitSync();
        }
        return inserted;
    }

    public int consumeUntilIdle(Duration pollTimeout, Duration idleGrace) {
        int totalInserted = 0;
        Instant lastRecordAt = Instant.now();
        while (Duration.between(lastRecordAt, Instant.now()).compareTo(idleGrace) < 0) {
            ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
            if (records.isEmpty()) {
                continue;
            }
            lastRecordAt = Instant.now();
            for (ConsumerRecord<String, String> record : records) {
                if (persist(record.value())) {
                    totalInserted++;
                }
            }
            consumer.commitSync();
        }
        return totalInserted;
    }

    private boolean persist(String json) {
        try {
            CdcEvent event = objectMapper.readValue(json, CdcEvent.class);
            return insertEvent(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist changelog event", e);
        }
    }

    private boolean insertEvent(CdcEvent event) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setObject(1, UUID.fromString(event.eventId()));
            statement.setString(2, event.schemaVersion());
            statement.setString(3, event.eventType());
            statement.setString(4, objectMapper.writeValueAsString(event.entityKey()));
            statement.setString(5, event.competenciaDe());
            statement.setString(6, event.competenciaPara());
            statement.setString(7, objectMapper.writeValueAsString(event.camposAlterados()));
            statement.setString(8, jsonOrNull(event.before()));
            statement.setString(9, jsonOrNull(event.after()));
            statement.setString(10, event.detectedAt());
            statement.setString(11, event.pipeline().estrategia());
            statement.setString(12, event.pipeline().runId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bind changelog event " + event.eventId(), e);
        }
    }

    private String jsonOrNull(java.util.Map<String, String> state) throws Exception {
        if (state == null || state.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(state);
    }

    @Override
    public void close() {
        consumer.close();
    }
}
