package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunIndex;
import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunLoader;
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

class EstabelecimentosParserTest {

    private static final String VALID_LINE =
            "12345678;0001;95;1;LOJA TESTE;02;20190101;00;;105;20180601;6201500;;RUA;RUA A;100;;CENTRO;01310100;SP;7107;11;30000000;;;;;contato@test.org;;";

    private static final String QUOTED_LINE =
            "12345678;0001;95;1;\"LOJA;TESTE\";02;20190101;00;;105;20180601;6201500;;RUA;RUA A;100;;CENTRO;01310100;SP;7107;11;30000000;;;;;contato@test.org;;";

    @Test
    void parsesCsvToCanonicalParquet(@TempDir Path canonicalRoot) throws Exception {
        TabmunIndex tabmun;
        try (var inputStream = getClass().getResourceAsStream("/tabmun-fixture.csv")) {
            tabmun = TabmunLoader.load(inputStream);
        }
        EstabelecimentosParser parser = new EstabelecimentosParser(tabmun);
        StageReport report = parser.parseCsv(
                new ByteArrayInputStream(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                new ParseRequest("2026-06", "test.csv", "sha-test"),
                canonicalRoot);

        assertEquals(1, report.linesRead());
        assertEquals(1, report.linesAccepted());
        assertEquals(0, report.linesRejected());
        assertEquals(0, report.municipioIbgeNaoResolvido());

        int bucket = CnpjPartitioner.bucket("12345678");
        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=estabelecimentos")
                .resolve("bucket=" + String.format("%03d", bucket))
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals(1, rows.size());
        Map<String, String> row = rows.getFirst();
        assertEquals("12345678", row.get("cnpj_basico"));
        assertEquals("12345678000195", row.get("cnpj"));
        assertEquals("LOJA TESTE", row.get("nome_fantasia"));
        assertEquals("2019-01-01", row.get("data_situacao_cadastral"));
        assertEquals("7107", row.get("municipio_receita"));
        assertEquals("3550308", row.get("municipio_ibge"));
    }

    @Test
    void countsUnresolvedMunicipioWhenTabmunMissing(@TempDir Path canonicalRoot) {
        EstabelecimentosParser parser = new EstabelecimentosParser();
        StageReport report = parser.parseCsv(
                new ByteArrayInputStream(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                new ParseRequest("2026-06", "test.csv", "sha-test"),
                canonicalRoot);

        assertEquals(1, report.municipioIbgeNaoResolvido());
    }

    @Test
    void parsesQuotedSemicolonInFantasia(@TempDir Path canonicalRoot) {
        EstabelecimentosParser parser = new EstabelecimentosParser();
        parser.parseCsv(
                new ByteArrayInputStream(QUOTED_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                new ParseRequest("2026-06", "test.csv", "sha-test"),
                canonicalRoot);

        int bucket = CnpjPartitioner.bucket("12345678");
        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=estabelecimentos")
                .resolve("bucket=" + String.format("%03d", bucket))
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals("LOJA;TESTE", rows.getFirst().get("nome_fantasia"));
    }

    @Test
    void parsesZipWithoutMaterializingCsv(@TempDir Path tempDir, @TempDir Path canonicalRoot) throws IOException {
        Path zipPath = tempDir.resolve("Estabelecimentos0.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("Estabelecimentos0.csv"));
            zipOutputStream.write(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }

        StageReport report = new EstabelecimentosParser()
                .parseZip(
                        zipPath,
                        new ParseRequest("2026-06", "pending", "sha-test"),
                        canonicalRoot);

        assertEquals(1, report.linesAccepted());
    }

    @Test
    void failsWhenRejectionRateExceedsLimit(@TempDir Path canonicalRoot) {
        String csv = VALID_LINE + "\n" + "bad;line\n";
        EstabelecimentosParser parser = new EstabelecimentosParser();

        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.ISO_8859_1)),
                        new ParseRequest("2026-06", "test.csv", "sha-test"),
                        canonicalRoot));
    }
}
