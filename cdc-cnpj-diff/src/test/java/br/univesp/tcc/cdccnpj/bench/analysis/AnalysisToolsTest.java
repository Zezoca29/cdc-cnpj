package br.univesp.tcc.cdccnpj.bench.analysis;

import br.univesp.tcc.cdccnpj.bench.ParquetFixtureWriter;
import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.bench.harness.BenchmarkAnalyzer;
import br.univesp.tcc.cdccnpj.bench.harness.EngineKind;
import br.univesp.tcc.cdccnpj.bench.harness.MeasurementResult;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisToolsTest {

    @Test
    void sanityInvariantsHoldOnSyntheticFixtures(@TempDir Path fixturesRoot) throws Exception {
        new ParquetFixtureWriter()
                .writeFixtures(fixturesRoot, new SyntheticSnapshotGenerator().generate());

        List<Integer> buckets = List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET);
        SanityInvariantReport report = new SanityInvariantChecker()
                .check(
                        new HashDiffEngine(),
                        fixturesRoot,
                        SyntheticSnapshotGenerator.COMPETENCIA_DE,
                        SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                        "empresas",
                        buckets,
                        0);

        assertTrue(report.allPassed());
    }

    @Test
    void characterizerCountsSyntheticRows(@TempDir Path fixturesRoot) throws Exception {
        new ParquetFixtureWriter()
                .writeFixtures(fixturesRoot, new SyntheticSnapshotGenerator().generate());

        TableCharacterization empresas = new CanonicalCharacterizer()
                .characterize(fixturesRoot, SyntheticSnapshotGenerator.COMPETENCIA_DE).stream()
                .filter(row -> row.tabela().equals("empresas"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, empresas.recordCount());
        assertEquals(1, empresas.bucketsPresent());
    }

    @Test
    void representativenessPassesForUniformDistribution() {
        java.util.Map<Integer, Long> perBucket = new java.util.HashMap<>();
        for (int i = 0; i < 256; i++) {
            perBucket.put(i, 10L);
        }
        RepresentativenessReport report = new BucketRepresentativenessChecker()
                .check(perBucket, ExperimentBuckets.DEFAULT_IDS, BucketRepresentativenessChecker.DEFAULT_TOLERANCE);
        assertTrue(report.withinTolerance());
    }

    @Test
    void externalSamplerWritesCsv(@TempDir Path fixturesRoot, @TempDir Path outputDir) throws Exception {
        new ParquetFixtureWriter()
                .writeFixtures(fixturesRoot, new SyntheticSnapshotGenerator().generate());

        ExternalValidationSampler sampler = new ExternalValidationSampler();
        List<ExternalValidationSampler.SampledEvent> events = sampler.sample(
                new HashDiffEngine(),
                fixturesRoot,
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                List.of("empresas"),
                List.of(SyntheticSnapshotGenerator.PRIMARY_BUCKET),
                5,
                42L);
        Path output = outputDir.resolve("sample.csv");
        sampler.writeCsv(output, 42L, events);

        assertFalse(events.isEmpty());
        assertTrue(Files.readString(output).contains("seed,event_id"));
    }

    @Test
    void analyzerComputesMedianAndSpeedup() {
        List<MeasurementResult> results = List.of(
                measurement(EngineKind.E1, 100),
                measurement(EngineKind.E1, 200),
                measurement(EngineKind.E3, 50),
                measurement(EngineKind.E3, 70));

        List<BenchmarkAnalyzer.BenchmarkSummary> summaries = new BenchmarkAnalyzer().summarize(results);
        BenchmarkAnalyzer.BenchmarkSummary e3 = summaries.stream()
                .filter(summary -> summary.engine().equals("E3"))
                .findFirst()
                .orElseThrow();

        assertEquals(60.0, e3.medianWallMs(), 0.01);
        assertEquals(2.5, e3.speedupVsE1(), 0.01);
    }

    private static MeasurementResult measurement(EngineKind engine, long wallMs) {
        return new MeasurementResult(
                Instant.parse("2026-08-27T00:00:00Z"),
                "run",
                engine,
                "empresas",
                1,
                1,
                false,
                wallMs * 1_000_000,
                wallMs * 500_000,
                1024,
                2048,
                4096,
                3,
                3.0);
    }
}
