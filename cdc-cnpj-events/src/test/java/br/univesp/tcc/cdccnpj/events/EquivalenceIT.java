package br.univesp.tcc.cdccnpj.events;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticFixtures;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.e1.PostgresDiffEngine;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;
import br.univesp.tcc.cdccnpj.events.mapping.EventMapper;
import br.univesp.tcc.cdccnpj.events.model.CdcEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EquivalenceIT {

    private static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj";
    private static final String JDBC_USER = "cdc";
    private static final String JDBC_PASSWORD = "cdc";

    private static boolean postgresAvailable;

    @BeforeAll
    static void checkPostgres() {
        postgresAvailable = false;
        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            postgresAvailable = connection.isValid(2);
        } catch (Exception ignored) {
            // docker compose not running — skip IT
        }
    }

    @Test
    void e1AndE3ProduceSameEventIdsOnSyntheticFixtures(@TempDir Path fixturesRoot) throws Exception {
        assumeTrue(postgresAvailable, "Postgres not available at " + JDBC_URL);

        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);

        PostgresDiffEngine e1 = PostgresDiffEngine.localhostDefaults();
        HashDiffEngine e3 = new HashDiffEngine();
        String runId = "equivalence-it-" + System.nanoTime();

        Set<UUID> e1EventIds = collectEventIds(e1, fixturesRoot, runId);
        Set<UUID> e3EventIds = collectEventIds(e3, fixturesRoot, runId);

        assertEquals(e1EventIds, e3EventIds, "E1 and E3 must produce identical event_id sets");
    }

    private static Set<UUID> collectEventIds(DiffEngine engine, Path fixturesRoot, String runId) {
        List<ChangeRecord> changes = List.of("empresas", "estabelecimentos").stream()
                .flatMap(tabela -> engine.diff(
                                fixturesRoot,
                                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                                tabela,
                                SyntheticSnapshotGenerator.PRIMARY_BUCKET)
                        .toList()
                        .stream())
                .toList();

        return changes.stream()
                .map(change -> toEventId(change, runId))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static UUID toEventId(ChangeRecord change, String runId) {
        CdcEvent event = EventMapper.toEvent(change, "E1", runId);
        return UUID.fromString(event.eventId());
    }
}
