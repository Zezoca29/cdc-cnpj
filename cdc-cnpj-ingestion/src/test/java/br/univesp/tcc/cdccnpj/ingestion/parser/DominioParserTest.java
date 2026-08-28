package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
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

class DominioParserTest {

    private static final String VALID_LINE = "6201500;Desenvolvimento de programas de computador sob encomenda";

    private static final String QUOTED_LINE = "6201501;\"Atividade; especializada\"";

    @Test
    void parsesCsvToFlatParquet(@TempDir Path canonicalRoot) {
        DominioParser parser = new DominioParser();
        StageReport report = parser.parseCsv(
                new ByteArrayInputStream(VALID_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                DominioTabela.CNAES,
                new ParseRequest("2026-06", "Cnaes.zip::Cnaes.csv", "sha-test"),
                canonicalRoot);

        assertEquals(1, report.linesAccepted());

        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=cnaes")
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals(1, rows.size());
        assertEquals("6201500", rows.getFirst().get("codigo"));
        assertEquals("Desenvolvimento de programas de computador sob encomenda", rows.getFirst().get("descricao"));
    }

    @Test
    void parsesQuotedSemicolonInDescricao(@TempDir Path canonicalRoot) {
        new DominioParser()
                .parseCsv(
                        new ByteArrayInputStream(QUOTED_LINE.getBytes(StandardCharsets.ISO_8859_1)),
                        DominioTabela.CNAES,
                        new ParseRequest("2026-06", "test.csv", "sha-test"),
                        canonicalRoot);

        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=cnaes")
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals("Atividade; especializada", rows.getFirst().get("descricao"));
    }

    @Test
    void resolvesTabelaFromZipFileName(@TempDir Path tempDir, @TempDir Path canonicalRoot) throws IOException {
        Path zipPath = tempDir.resolve("Motivos.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("Motivos.csv"));
            zipOutputStream.write("01;Motivo teste".getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }

        new DominioParser()
                .parseZip(zipPath, new ParseRequest("2026-06", "pending", "sha-test"), canonicalRoot);

        Path parquet = canonicalRoot
                .resolve("competencia=2026-06")
                .resolve("tabela=motivos")
                .resolve("part-0.parquet");

        List<Map<String, String>> rows = new CanonicalParquetReader().readBucket(parquet);
        assertEquals("01", rows.getFirst().get("codigo"));
    }

    @Test
    void mapsAllDominioZipNames() {
        assertEquals(DominioTabela.CNAES, DominioTabela.fromZipFileName("Cnaes.zip"));
        assertEquals(DominioTabela.MUNICIPIOS, DominioTabela.fromZipFileName("Municipios.zip"));
        assertEquals(DominioTabela.NATUREZAS, DominioTabela.fromZipFileName("Naturezas.zip"));
        assertEquals(DominioTabela.QUALIFICACOES, DominioTabela.fromZipFileName("Qualificacoes.zip"));
        assertEquals(DominioTabela.PAISES, DominioTabela.fromZipFileName("Paises.zip"));
    }

    @Test
    void failsWhenRejectionRateExceedsLimit(@TempDir Path canonicalRoot) {
        String csv = VALID_LINE + "\n" + "only-one-column\n";
        DominioParser parser = new DominioParser();

        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.ISO_8859_1)),
                        DominioTabela.CNAES,
                        new ParseRequest("2026-06", "test.csv", "sha-test"),
                        canonicalRoot));
    }
}
