package br.univesp.tcc.cdccnpj.diff.e3;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticFixtures;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashDiffEngineIT {

    @Test
    void diffSyntheticFixturesBucketZero(@TempDir Path fixturesRoot) throws Exception {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);

        HashDiffEngine engine = new HashDiffEngine();

        List<ChangeRecord> empresaChanges = engine.diff(
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "empresas",
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET)
                .toList();
        List<ChangeRecord> estabChanges = engine.diff(
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "estabelecimentos",
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET)
                .toList();

        assertEquals(3, empresaChanges.size());
        assertEquals(6, estabChanges.size());

        long empresaDeletes = empresaChanges.stream().filter(c -> c.changeType() == ChangeType.DELETE).count();
        long empresaInserts = empresaChanges.stream().filter(c -> c.changeType() == ChangeType.INSERT).count();
        long empresaUpdates = empresaChanges.stream().filter(c -> c.changeType() == ChangeType.UPDATE).count();
        assertEquals(1, empresaDeletes);
        assertEquals(1, empresaInserts);
        assertEquals(1, empresaUpdates);

        Map<String, ChangeRecord> capitalUpdate = empresaChanges.stream()
                .filter(c -> c.changeType() == ChangeType.UPDATE)
                .collect(Collectors.toMap(c -> c.entityKey().get("cnpj_basico"), c -> c));
        assertEquals(List.of("capital_social"), capitalUpdate.values().iterator().next().camposAlterados());
    }
}
