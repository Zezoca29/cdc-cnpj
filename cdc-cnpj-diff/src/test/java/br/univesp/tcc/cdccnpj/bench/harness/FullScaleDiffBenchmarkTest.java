package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullScaleDiffBenchmarkTest {

    @Test
    void runsDiffOnRequestedBucketScale(@TempDir Path fixturesRoot) throws Exception {
        new ParquetFixtureWriter()
                .writeFixtures(fixturesRoot, new SyntheticSnapshotGenerator().generate());

        int scale = 4;
        List<Integer> bucketIds = DiffMeasurementHarness.bucketIdsForScale(scale);
        MeasurementResult result = new DiffMeasurementHarness()
                .measure(
                        new DiffMeasurementHarness().createEngine(EngineKind.E3),
                        EngineKind.E3,
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "empresas",
                        bucketIds,
                        1,
                        false,
                        "test");

        assertEquals(scale, result.bucketScale());
        assertTrue(result.eventCount() >= 0);
    }

    @Test
    void aggregatesPerTableResults(@TempDir Path fixturesRoot) throws Exception {
        new ParquetFixtureWriter()
                .writeFixtures(fixturesRoot, new SyntheticSnapshotGenerator().generate());

        FullScaleDiffReport report = new FullScaleDiffBenchmark()
                .run(
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        EngineKind.E3,
                        List.of("empresas", "estabelecimentos"));

        assertEquals(2, report.perTableResults().size());
        assertEquals(256, report.perTableResults().getFirst().bucketScale());
        assertTrue(report.totalEventCount() > 0);
        assertTrue(H2Validator.validate(report).passed());
    }
}
