package br.univesp.tcc.cdccnpj.bench;

import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntheticSnapshotGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDocumentedExpectedChanges() {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        assertEquals(9, fixtures.expectedChanges().size());
        assertEquals(3, fixtures.empresasDe().size());
        assertEquals(3, fixtures.empresasPara().size());
        assertEquals(6, fixtures.estabelecimentosDe().size());
        assertEquals(6, fixtures.estabelecimentosPara().size());
    }

    @Test
    void primaryRecordsLandInBucketZero() {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        fixtures.estabelecimentosDe().forEach(estab ->
                assertEquals(
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET,
                        CnpjPartitioner.bucket(estab.cnpjBasico()),
                        estab.cnpjBasico()));
        fixtures.empresasDe().forEach(emp ->
                assertEquals(
                        SyntheticSnapshotGenerator.PRIMARY_BUCKET,
                        CnpjPartitioner.bucket(emp.cnpjBasico()),
                        emp.cnpjBasico()));
    }

    @Test
    void writesParquetFixturesToCanonicalLayout(@TempDir Path output) throws Exception {
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(output, fixtures);
        FixtureManifestWriter.write(output, fixtures);

        Path bucketDir = output
                .resolve("competencia=" + SyntheticSnapshotGenerator.COMPETENCIA_DE)
                .resolve("tabela=estabelecimentos")
                .resolve("bucket=000");
        Path parquet = bucketDir.resolve("part-0.parquet");
        assertTrue(Files.exists(parquet), "expected parquet at " + parquet);
        assertTrue(Files.size(parquet) > 0);
        assertTrue(Files.exists(output.resolve("manifesto.json")));
    }

    @Test
    void bucketFactoryFindsDistinctBasicos() {
        String first = BucketBasicoFactory.nthBasicoForBucket(0, 0);
        String second = BucketBasicoFactory.nthBasicoForBucket(0, 1);
        assertFalse(first.equals(second));
        assertEquals(0, CnpjPartitioner.bucket(first));
        assertEquals(0, CnpjPartitioner.bucket(second));
    }
}
