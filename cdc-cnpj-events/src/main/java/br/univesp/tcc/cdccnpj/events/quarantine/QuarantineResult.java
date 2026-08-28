package br.univesp.tcc.cdccnpj.events.quarantine;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado da quarentena com métricas para análise (X4).
 */
public record QuarantineResult(
        List<ChangeRecord> toPublish,
        int inputDeletes,
        int publishedDeletes,
        long cancelledByResurrection,
        long immediatelyConfirmed,
        int stillPending) {

    public static QuarantineResult accept(List<ChangeRecord> changes, DeleteQuarantine quarantine) {
        int inputDeletes = countDeletes(changes);
        List<ChangeRecord> toPublish = quarantine.acceptForPublishing(changes);
        int publishedDeletes = countDeletes(toPublish);
        return new QuarantineResult(
                toPublish,
                inputDeletes,
                publishedDeletes,
                quarantine.cancelledByResurrection(),
                quarantine.immediatelyConfirmed(),
                quarantine.pendingCount());
    }

    public static QuarantineResult acceptAndFlush(List<ChangeRecord> changes) {
        return acceptAndFlush(changes, new DeleteQuarantine());
    }

    public static QuarantineResult acceptAndFlush(List<ChangeRecord> changes, DeleteQuarantine quarantine) {
        QuarantineResult accepted = accept(changes, quarantine);
        List<ChangeRecord> confirmed = quarantine.flushConfirmed();
        if (confirmed.isEmpty()) {
            return accepted;
        }
        List<ChangeRecord> combined = new ArrayList<>(accepted.toPublish());
        combined.addAll(confirmed);
        int publishedDeletes = accepted.publishedDeletes() + countDeletes(confirmed);
        return new QuarantineResult(
                List.copyOf(combined),
                accepted.inputDeletes(),
                publishedDeletes,
                accepted.cancelledByResurrection(),
                accepted.immediatelyConfirmed(),
                quarantine.pendingCount());
    }

    private static int countDeletes(List<ChangeRecord> changes) {
        return (int) changes.stream().filter(change -> change.changeType() == ChangeType.DELETE).count();
    }

    static int countDeletesPublic(List<ChangeRecord> changes) {
        return countDeletes(changes);
    }
}
