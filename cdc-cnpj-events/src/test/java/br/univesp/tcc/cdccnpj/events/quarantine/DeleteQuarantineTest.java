package br.univesp.tcc.cdccnpj.events.quarantine;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteQuarantineTest {

    @Test
    void defersEmpresaDeleteUntilFlush() {
        DeleteQuarantine quarantine = new DeleteQuarantine();
        ChangeRecord delete = empresaDelete("12345678");

        List<ChangeRecord> accepted = quarantine.acceptForPublishing(List.of(delete));

        assertTrue(accepted.isEmpty());
        assertEquals(1, quarantine.pendingCount());

        List<ChangeRecord> confirmed = quarantine.flushConfirmed();
        assertEquals(1, confirmed.size());
        assertEquals(ChangeType.DELETE, confirmed.getFirst().changeType());
    }

    @Test
    void acceptAndFlushPublishesDeferredDeletes() {
        ChangeRecord delete = empresaDelete("12345678");
        QuarantineResult result = QuarantineResult.acceptAndFlush(List.of(delete));

        assertEquals(1, result.toPublish().size());
        assertEquals(1, result.inputDeletes());
        assertEquals(1, result.publishedDeletes());
        assertEquals(0, result.stillPending());
    }

    @Test
    void insertCancelsQuarantinedDelete() {
        DeleteQuarantine quarantine = new DeleteQuarantine();
        ChangeRecord delete = empresaDelete("12345678");
        ChangeRecord insert = empresaInsert("12345678");

        quarantine.acceptForPublishing(List.of(delete));
        QuarantineResult secondPass = QuarantineResult.accept(List.of(insert), quarantine);

        assertEquals(1, secondPass.toPublish().size());
        assertEquals(ChangeType.INSERT, secondPass.toPublish().getFirst().changeType());
        assertEquals(1, secondPass.cancelledByResurrection());
        assertEquals(0, quarantine.pendingCount());
    }

    @Test
    void estabelecimentoSituacao08ConfirmsImmediately() {
        ChangeRecord delete = new ChangeRecord(
                ChangeType.DELETE,
                "estabelecimentos",
                0,
                Map.of("cnpj_basico", "12345678", "cnpj_ordem", "0001", "cnpj_dv", "95"),
                Map.of("situacao_cadastral", "08"),
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");

        QuarantineResult result = QuarantineResult.acceptAndFlush(List.of(delete));

        assertEquals(1, result.toPublish().size());
        assertEquals(1, result.immediatelyConfirmed());
        assertEquals(1, result.publishedDeletes());
    }

    @Test
    void estabelecimentoDeleteWithoutSituacao08IsQuarantinedUntilFlush() {
        ChangeRecord delete = new ChangeRecord(
                ChangeType.DELETE,
                "estabelecimentos",
                0,
                Map.of("cnpj_basico", "12345678", "cnpj_ordem", "0001", "cnpj_dv", "95"),
                Map.of("situacao_cadastral", "02"),
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");

        QuarantineResult accepted = QuarantineResult.accept(List.of(delete), new DeleteQuarantine());

        assertTrue(accepted.toPublish().isEmpty());
        assertEquals(0, accepted.immediatelyConfirmed());
        assertEquals(1, accepted.stillPending());
    }

    private static ChangeRecord empresaDelete(String cnpjBasico) {
        return new ChangeRecord(
                ChangeType.DELETE,
                "empresas",
                0,
                Map.of("cnpj_basico", cnpjBasico),
                Map.of("razao_social", "ACME"),
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");
    }

    private static ChangeRecord empresaInsert(String cnpjBasico) {
        return new ChangeRecord(
                ChangeType.INSERT,
                "empresas",
                0,
                Map.of("cnpj_basico", cnpjBasico),
                Map.of(),
                Map.of("razao_social", "ACME"),
                List.of(),
                "2026-06",
                "2026-07");
    }
}
