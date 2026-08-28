package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CanonicalSchemas;
import br.univesp.tcc.cdccnpj.ingestion.normalize.FlatParquetWriter;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parser streaming ZIP→Parquet das tabelas de domínio (CONTRATOS.md §3.5, R3).
 */
public final class DominioParser {

    private final Schema schema = CanonicalSchemas.dominio();

    public StageReport parseZip(Path zipPath, ParseRequest request, Path canonicalRoot) {
        DominioTabela tabela = DominioTabela.fromZipFileName(zipPath.getFileName().toString());
        try (ZipInputStream zipInputStream = new ZipInputStream(java.nio.file.Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
                    String arquivoOrigem = zipPath.getFileName() + "::" + entry.getName();
                    return parseCsv(
                            zipInputStream, tabela, request.withArquivoOrigem(arquivoOrigem), canonicalRoot);
                }
            }
            throw new ParseStageException("No CSV entry found in zip: " + zipPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading zip " + zipPath, e);
        }
    }

    public StageReport parseCsv(
            InputStream csvStream, DominioTabela tabela, ParseRequest request, Path canonicalRoot) {
        long linesRead = 0;
        long linesAccepted = 0;
        long linesRejected = 0;

        try (BufferedReader reader =
                        new BufferedReader(new InputStreamReader(csvStream, IngestionConstants.SOURCE_CHARSET));
                FlatParquetWriter writer =
                        new FlatParquetWriter(canonicalRoot, request.competencia(), tabela.tabela(), schema)) {
            String line;
            while ((line = reader.readLine()) != null) {
                linesRead++;
                if (line.isBlank()) {
                    linesRejected++;
                    continue;
                }
                try {
                    GenericRecord record = toRecord(line, linesRead, request);
                    writer.write(record);
                    linesAccepted++;
                } catch (IllegalArgumentException | ParseStageException ex) {
                    linesRejected++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed parsing dominio CSV for " + tabela.tabela(), e);
        }

        StageReport report = new StageReport(linesRead, linesAccepted, linesRejected, Map.of(), 0);
        report.assertRejectionWithinLimit();
        return report;
    }

    private GenericRecord toRecord(String line, long lineNumber, ParseRequest request) {
        List<String> fields = CsvLineParser.parseFields(line);
        if (fields.size() != IngestionConstants.DOMINIO_CSV_COLUMNS) {
            throw new ParseStageException(
                    "Line " + lineNumber + " has " + fields.size() + " columns, expected "
                            + IngestionConstants.DOMINIO_CSV_COLUMNS);
        }

        String codigo = requireNonBlank(fields.get(0), "codigo");

        GenericRecord record = new GenericData.Record(schema);
        record.put("competencia", request.competencia());
        record.put("arquivo_origem", request.arquivoOrigem());
        record.put("sha256_origem", request.sha256Origem());
        record.put("linha_origem", lineNumber);
        record.put("codigo", codigo);
        record.put("descricao", emptyToNull(fields.get(1)));
        return record;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ParseStageException("Missing required field: " + name);
        }
        return value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
