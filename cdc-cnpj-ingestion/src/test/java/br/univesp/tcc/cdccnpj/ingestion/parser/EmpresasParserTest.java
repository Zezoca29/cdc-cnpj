package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmpresasParserTest {

    private static final String VALID_LINE = "12345678;EMPRESA TESTE LTDA;2062;49;1000,00;01;";

    private static final String QUOTED_LINE = "12345678;\"EMPRESA;TESTE\";2062;49;1000,00;01;";

    @Test
    void parsesCsvToCanonicalParquet(@TempDir Path canonicalRoot) {
        EmpresasParser parser = new EmpresasParser();
        StageReport report = parser.parseCsv(
                new ByteArrayInputStream(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                new ParseRequest("2026-06", "test.csv", "sha-test"),
                canonicalRoot);

        assertEquals(1, report.linesRead());
        assertEquals(1, report.linesAccepted());
        assertEquals(0, report.linesRejected());

        int bucket = CnpjPartitioner.bucket("12345678");
        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=empresas")
                .resolve("bucket=" + String.format("%03d", bucket))
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals(1, rows.size());
        Map<String, String> row = rows.getFirst();
        assertEquals("12345678", row.get("cnpj_basico"));
        assertEquals("EMPRESA TESTE LTDA", row.get("razao_social"));
        assertEquals("1000,00", row.get("capital_social"));
        assertEquals("01", row.get("porte"));
    }

    @Test
    void parsesQuotedSemicolonInRazaoSocial(@TempDir Path canonicalRoot) {
        new EmpresasParser()
                .parseCsv(
                        new ByteArrayInputStream(QUOTED_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                        new ParseRequest("2026-06", "test.csv", "sha-test"),
                        canonicalRoot);

        int bucket = CnpjPartitioner.bucket("12345678");
        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=empresas")
                .resolve("bucket=" + String.format("%03d", bucket))
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals("EMPRESA;TESTE", rows.getFirst().get("razao_social"));
    }

    @Test
    void parsesZipWithoutMaterializingCsv(@TempDir Path tempDir, @TempDir Path canonicalRoot) throws IOException {
        Path zipPath = tempDir.resolve("Empresas0.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("Empresas0.csv"));
            zipOutputStream.write(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }

        StageReport report = new EmpresasParser()
                .parseZip(zipPath, new ParseRequest("2026-06", "pending", "sha-test"), canonicalRoot);

        assertEquals(1, report.linesAccepted());
    }

    @Test
    void failsWhenRejectionRateExceedsLimit(@TempDir Path canonicalRoot) {
        String csv = VALID_LINE + "\n" + "bad;line\n";
        EmpresasParser parser = new EmpresasParser();

        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.ISO_8859_1)),
                        new ParseRequest("2026-06", "test.csv", "sha-test"),
                        canonicalRoot));
    }
}
