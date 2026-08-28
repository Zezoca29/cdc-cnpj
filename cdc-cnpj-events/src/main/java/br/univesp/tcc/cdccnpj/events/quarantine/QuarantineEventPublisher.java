package br.univesp.tcc.cdccnpj.events.quarantine;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.events.kafka.KafkaEventProducer;
import br.univesp.tcc.cdccnpj.events.mapping.EventMapper;
import br.univesp.tcc.cdccnpj.events.model.CdcEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Publica eventos aplicando quarentena de deletes (ADR-006).
 */
public final class QuarantineEventPublisher {

    private QuarantineEventPublisher() {
    }

    public static QuarantineResult publish(
            KafkaEventProducer producer, List<ChangeRecord> changes, String estrategia, String runId) {
        return publishAll(producer, changes, estrategia, runId);
    }

    public static QuarantineResult publishAll(
            KafkaEventProducer producer, List<ChangeRecord> changes, String estrategia, String runId) {
        DeleteQuarantine quarantine = new DeleteQuarantine();
        QuarantineResult accepted = QuarantineResult.accept(changes, quarantine);
        publishRecords(producer, accepted.toPublish(), estrategia, runId);

        List<ChangeRecord> confirmed = quarantine.flushConfirmed();
        publishRecords(producer, confirmed, estrategia, runId);

        List<ChangeRecord> published = new ArrayList<>(accepted.toPublish());
        published.addAll(confirmed);
        return new QuarantineResult(
                List.copyOf(published),
                accepted.inputDeletes(),
                accepted.publishedDeletes() + QuarantineResult.countDeletesPublic(confirmed),
                accepted.cancelledByResurrection(),
                accepted.immediatelyConfirmed(),
                quarantine.pendingCount());
    }

    private static void publishRecords(
            KafkaEventProducer producer, List<ChangeRecord> changes, String estrategia, String runId) {
        for (ChangeRecord change : changes) {
            CdcEvent event = EventMapper.toEvent(change, estrategia, runId);
            producer.publish(change, event);
        }
    }
}
