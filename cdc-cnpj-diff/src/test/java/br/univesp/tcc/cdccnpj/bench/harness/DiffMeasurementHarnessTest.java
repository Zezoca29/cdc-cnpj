package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticFixtures;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffMeasurementHarnessTest {

    @Test
    void measuresE3OnSyntheticFixtures(@TempDir Path fixturesRoot) throws Exception {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);

        DiffMeasurementHarness harness = new DiffMeasurementHarness();
        MeasurementResult result = harness.measure(
                harness.createEngine(EngineKind.E3),
                EngineKind.E3,
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                "empresas",
                List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET),
                1,
                false,
                "test-run");

        assertEquals(3, result.eventCount());
        assertTrue(result.wallTimeNanos() > 0);
        assertTrue(result.bytesRead() > 0);
        assertTrue(result.throughputEventsPerSecond() > 0);
    }

    @Test
    void writesCsvWithWarmupFlag(@TempDir Path outputDir) throws Exception {
        MeasurementResult warmup = sampleResult(true);
        MeasurementResult measured = sampleResult(false);
        Path csv = outputDir.resolve("bench.csv");

        BenchmarkCsvWriter.write(csv, List.of(warmup, measured));

        assertTrue(Files.exists(csv));
        String content = Files.readString(csv);
        assertTrue(content.contains("warmup"));
        assertTrue(content.contains(",true,"));
        assertTrue(content.contains(",false,"));
    }

    @Test
    void matrixRunsConfiguredRepetitions(@TempDir Path fixturesRoot) throws Exception {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(fixturesRoot, fixtures);

        BenchmarkMatrix matrix = new BenchmarkMatrix();
        List<MeasurementResult> results = matrix.run(
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                List.of("empresas"),
                List.of(EngineKind.E3),
                new int[] {1},
                3,
                1);

        assertEquals(3, results.size());
        assertEquals(1, results.stream().filter(MeasurementResult::warmup).count());
        assertEquals(2, results.stream().filter(result -> !result.warmup()).count());
    }

    private static MeasurementResult sampleResult(boolean warmup) {
        return new MeasurementResult(
                java.time.Instant.parse("2026-08-27T00:00:00Z"),
                "run-1",
                EngineKind.E3,
                "empresas",
                1,
                warmup ? 1 : 2,
                warmup,
                1_000_000,
                500_000,
                1024,
                2048,
                4096,
                3,
                3.0);
    }
}
