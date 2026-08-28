package br.univesp.tcc.cdccnpj.diff.compare;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.socio.SocioIdentityResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GhostEventAnalyzerTest {

    @Test
    void countsGhostPairWhenMaskedCpfChangesButIdentityMatches() {
        Map<String, String> beforeState = socioRow("12345678", "1", "João da Silva", "***111111**");
        Map<String, String> afterState = socioRow("12345678", "1", "JOAO DA SILVA", "***999222**");
        String identityKey = SocioIdentityResolver.identityKeyFromRow(beforeState);

        ChangeRecord delete = new ChangeRecord(
                ChangeType.DELETE,
                "socios",
                0,
                Map.of("cnpj_basico", "12345678", "identificador_socio", "1", "cpf_cnpj_socio", "***111111**"),
                beforeState,
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");
        ChangeRecord insert = new ChangeRecord(
                ChangeType.INSERT,
                "socios",
                0,
                Map.of("cnpj_basico", "12345678", "identificador_socio", "1", "cpf_cnpj_socio", "***999222**"),
                Map.of(),
                afterState,
                List.of(),
                "2026-06",
                "2026-07");

        GhostEventAnalyzer.GhostEventReport report =
                GhostEventAnalyzer.analyze(List.of(delete, insert));

        assertEquals(1, report.ghostPairs());
        assertEquals(1, report.naiveDeletes());
        assertEquals(1, report.naiveInserts());
        assertEquals(1.0, report.ghostRateAmongPairs());
        assertEquals(identityKey, SocioIdentityResolver.identityKeyFromRow(afterState));
    }

    @Test
    void noGhostWhenDifferentPeople() {
        ChangeRecord delete = new ChangeRecord(
                ChangeType.DELETE,
                "socios",
                0,
                Map.of("cnpj_basico", "12345678", "identificador_socio", "1", "cpf_cnpj_socio", "***111111**"),
                socioRow("12345678", "1", "Maria Souza", "***111111**"),
                Map.of(),
                List.of(),
                "2026-06",
                "2026-07");
        ChangeRecord insert = new ChangeRecord(
                ChangeType.INSERT,
                "socios",
                0,
                Map.of("cnpj_basico", "12345678", "identificador_socio", "1", "cpf_cnpj_socio", "***222222**"),
                Map.of(),
                socioRow("12345678", "1", "Jose Alves", "***222222**"),
                List.of(),
                "2026-06",
                "2026-07");

        GhostEventAnalyzer.GhostEventReport report =
                GhostEventAnalyzer.analyze(List.of(delete, insert));

        assertEquals(0, report.ghostPairs());
    }

    private static Map<String, String> socioRow(
            String cnpjBasico, String identificador, String nome, String cpf) {
        return Map.of(
                "cnpj_basico", cnpjBasico,
                "identificador_socio", identificador,
                "nome_socio", nome,
                "cpf_cnpj_socio", cpf);
    }
}
