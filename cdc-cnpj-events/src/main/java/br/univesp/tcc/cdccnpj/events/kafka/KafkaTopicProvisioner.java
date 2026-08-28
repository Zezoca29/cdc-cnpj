package br.univesp.tcc.cdccnpj.events.kafka;

import br.univesp.tcc.cdccnpj.events.model.EventTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Garante que os tópicos do núcleo existem antes do consumer subscrever.
 */
public final class KafkaTopicProvisioner {

    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    private KafkaTopicProvisioner() {
    }

    public static void ensureNucleoTopics() {
        ensureNucleoTopics("localhost:9092");
    }

    public static void ensureNucleoTopics(String bootstrapServers) {
        ensureTopics(
                bootstrapServers,
                List.of(
                        EventTopics.EMPRESAS,
                        EventTopics.ESTABELECIMENTOS,
                        EventTopics.DOMINIO,
                        EventTopics.SOCIOS,
                        EventTopics.SIMPLES));
    }

    public static void ensureTopics(String bootstrapServers, Collection<String> topics) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(properties)) {
            Set<String> existing = admin.listTopics().names().get();
            List<NewTopic> missing = new ArrayList<>();
            for (String topic : topics) {
                if (!existing.contains(topic)) {
                    missing.add(new NewTopic(topic, PARTITIONS, REPLICATION_FACTOR));
                }
            }
            if (!missing.isEmpty()) {
                admin.createTopics(missing).all().get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating Kafka topics", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to create Kafka topics", e.getCause());
        }
    }
}
