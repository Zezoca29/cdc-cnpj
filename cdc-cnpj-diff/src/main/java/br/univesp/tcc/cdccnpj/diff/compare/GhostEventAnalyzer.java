package br.univesp.tcc.cdccnpj.diff.compare;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.socio.SocioIdentityResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Conta pares fantasma socio.excluido + socio.incluido do diff ingênuo (spec §17, ADR-004).
 */
public final class GhostEventAnalyzer {

    public record GhostEventReport(
            int ghostPairs,
            int naiveDeletes,
            int naiveInserts,
            double ghostRateAmongPairs) {
    }

    private GhostEventAnalyzer() {
    }

    public static GhostEventReport analyze(List<ChangeRecord> naiveChanges) {
        List<ChangeRecord> deletes = naiveChanges.stream()
                .filter(change -> change.changeType() == ChangeType.DELETE)
                .toList();
        List<ChangeRecord> inserts = naiveChanges.stream()
                .filter(change -> change.changeType() == ChangeType.INSERT)
                .toList();

        Set<Integer> matchedInserts = new HashSet<>();
        int ghostPairs = 0;

        for (int deleteIndex = 0; deleteIndex < deletes.size(); deleteIndex++) {
            ChangeRecord delete = deletes.get(deleteIndex);
            String cnpjBasico = delete.entityKey().get("cnpj_basico");
            for (int insertIndex = 0; insertIndex < inserts.size(); insertIndex++) {
                if (matchedInserts.contains(insertIndex)) {
                    continue;
                }
                ChangeRecord insert = inserts.get(insertIndex);
                if (!cnpjBasico.equals(insert.entityKey().get("cnpj_basico"))) {
                    continue;
                }
                if (SocioIdentityResolver.sameIdentity(delete.before(), insert.after())) {
                    ghostPairs++;
                    matchedInserts.add(insertIndex);
                    break;
                }
            }
        }

        double ghostRate = ghostPairs == 0
                ? 0.0
                : (double) ghostPairs / Math.max(1, Math.min(deletes.size(), inserts.size()));
        return new GhostEventReport(ghostPairs, deletes.size(), inserts.size(), ghostRate);
    }

    public static List<ChangeRecord> filterSocios(List<ChangeRecord> changes) {
        List<ChangeRecord> socios = new ArrayList<>();
        for (ChangeRecord change : changes) {
            if ("socios".equals(change.tabela())) {
                socios.add(change);
            }
        }
        return List.copyOf(socios);
    }
}
