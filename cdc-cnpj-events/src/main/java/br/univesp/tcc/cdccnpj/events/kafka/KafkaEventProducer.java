package br.univesp.tcc.cdccnpj.events.kafka;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.events.mapping.EventMapper;
import br.univesp.tcc.cdccnpj.events.model.CdcEvent;
import br.univesp.tcc.cdccnpj.events.model.EventTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public final class KafkaEventProducer implements AutoCloseable {

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaEventProducer(Properties producerProperties) {
        this.producer = new KafkaProducer<>(producerProperties);
        this.objectMapper = EventJsonMapper.create();
    }

    public static Properties localhostDefaults() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return properties;
    }

    public void publish(ChangeRecord change, CdcEvent event) {
        String topic = EventTopics.topicForTable(change.tabela());
        String key = EventMapper.partitionKey(change);
        try {
            String payload = objectMapper.writeValueAsString(event);
            producer.send(new ProducerRecord<>(topic, key, payload)).get();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to publish event " + event.eventId(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event " + event.eventId(), e);
        }
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        producer.close();
    }
}
