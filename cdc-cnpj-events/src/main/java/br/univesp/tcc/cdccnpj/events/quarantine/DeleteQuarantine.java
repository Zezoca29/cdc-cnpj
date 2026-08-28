package br.univesp.tcc.cdccnpj.events.quarantine;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Retém deletes antes da publicação Kafka; cancela em ressurreições (ADR-006, P3).
 */
public final class DeleteQuarantine {

    private final Map<String, ChangeRecord> pendingDeletes = new LinkedHashMap<>();
    private long cancelledByResurrection;
    private long immediatelyConfirmed;

    public long cancelledByResurrection() {
        return cancelledByResurrection;
    }

    public long immediatelyConfirmed() {
        return immediatelyConfirmed;
    }

    public int pendingCount() {
        return pendingDeletes.size();
    }

    /**
     * Processa mudanças sem confirmar deletes pendentes — chame {@link #flushConfirmed()} ao final do pipeline.
     */
    public List<ChangeRecord> acceptForPublishing(List<ChangeRecord> changes) {
        List<ChangeRecord> insertsAndUpdates = new ArrayList<>();
        List<ChangeRecord> deletes = new ArrayList<>();
        for (ChangeRecord change : changes) {
            if (change.changeType() == ChangeType.DELETE) {
                deletes.add(change);
            } else {
                insertsAndUpdates.add(change);
            }
        }

        List<ChangeRecord> toPublish = new ArrayList<>();
        for (ChangeRecord change : insertsAndUpdates) {
            if (change.changeType() == ChangeType.INSERT && pendingDeletes.remove(quarantineKey(change)) != null) {
                cancelledByResurrection++;
            }
            toPublish.add(change);
        }

        for (ChangeRecord delete : deletes) {
            if (shouldConfirmImmediately(delete)) {
                immediatelyConfirmed++;
                toPublish.add(delete);
            } else {
                pendingDeletes.put(quarantineKey(delete), delete);
            }
        }

        return List.copyOf(toPublish);
    }

    public List<ChangeRecord> flushConfirmed() {
        if (pendingDeletes.isEmpty()) {
            return List.of();
        }
        List<ChangeRecord> confirmed = List.copyOf(pendingDeletes.values());
        pendingDeletes.clear();
        return confirmed;
    }

    static boolean shouldConfirmImmediately(ChangeRecord delete) {
        if (!"estabelecimentos".equals(delete.tabela())) {
            return false;
        }
        Map<String, String> before = delete.before();
        return before != null && "08".equals(before.get("situacao_cadastral"));
    }

    static String quarantineKey(ChangeRecord change) {
        String entityKeyCanonical = change.entityKey().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return change.tabela() + "|" + entityKeyCanonical;
    }
}
