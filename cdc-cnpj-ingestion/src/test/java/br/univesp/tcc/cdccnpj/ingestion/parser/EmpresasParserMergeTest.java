package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.ingestion.normalize.BucketedParquetWriter;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CanonicalSchemas;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmpresasParserMergeTest {

    private static final String LINE_A = "12345678;EMPRESA A;2062;49;1000,00;01;";
    private static final String LINE_B = "87654321;EMPRESA B;2062;49;2000,00;01;";

    @Test
    void mergesMultipleZipPartsIntoSameWriter(@TempDir Path tempDir, @TempDir Path canonicalRoot) throws Exception {
        Path zipA = writeZip(tempDir.resolve("Empresas0.zip"), "Empresas0.csv", LINE_A);
        Path zipB = writeZip(tempDir.resolve("Empresas1.zip"), "Empresas1.csv", LINE_B);

        EmpresasParser parser = new EmpresasParser();
        try (BucketedParquetWriter writer =
                new BucketedParquetWriter(canonicalRoot, "2026-06", "empresas", CanonicalSchemas.empresas())) {
            parser.parseZip(zipA, new ParseRequest("2026-06", "Empresas0.zip", "sha-a"), writer);
            parser.parseZip(zipB, new ParseRequest("2026-06", "Empresas1.zip", "sha-b"), writer);
        }

        int bucketA = CnpjPartitioner.bucket("12345678");
        int bucketB = CnpjPartitioner.bucket("87654321");
        List<Map<String, String>> rowsA = readBucket(canonicalRoot, bucketA);
        List<Map<String, String>> rowsB = readBucket(canonicalRoot, bucketB);

        assertEquals(1, rowsA.size());
        assertEquals(1, rowsB.size());
        assertEquals("EMPRESA A", rowsA.getFirst().get("razao_social"));
        assertEquals("EMPRESA B", rowsB.getFirst().get("razao_social"));
    }

    private static List<Map<String, String>> readBucket(Path canonicalRoot, int bucket) {
        Path parquet = canonicalRoot
                .resolve("competencia=2026-06/tabela=empresas/bucket=" + String.format("%03d", bucket) + "/part-0.parquet");
        return new CanonicalParquetReader().readBucket(parquet);
    }

    private static Path writeZip(Path zipPath, String entryName, String line) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(line.getBytes(StandardCharsets.ISO_8859_1));
            zip.closeEntry();
        }
        return zipPath;
    }
}
