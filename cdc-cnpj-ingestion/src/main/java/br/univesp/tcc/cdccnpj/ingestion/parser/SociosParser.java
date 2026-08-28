package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.normalize.BucketedParquetWriter;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CanonicalSchemas;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import br.univesp.tcc.cdccnpj.socio.SocioIdentityResolver;
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
 * Parser streaming ZIP→Parquet de sócios (ADR-004, Expansão X2).
 */
public final class SociosParser {

    private final Schema schema = CanonicalSchemas.socios();

    public StageReport parseZip(Path zipPath, ParseRequest request, Path canonicalRoot) {
        try (BucketedParquetWriter writer =
                new BucketedParquetWriter(canonicalRoot, request.competencia(), "socios", schema)) {
            return parseZip(zipPath, request, writer);
        }
    }

    public StageReport parseZip(Path zipPath, ParseRequest request, BucketedParquetWriter writer) {
        try (ZipInputStream zipInputStream = new ZipInputStream(java.nio.file.Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (ReceitaFederalZipEntry.isDataFile(entry)) {
                    String arquivoOrigem = zipPath.getFileName() + "::" + entry.getName();
                    return parseCsv(zipInputStream, request.withArquivoOrigem(arquivoOrigem), writer);
                }
            }
            throw new ParseStageException("No CSV entry found in zip: " + zipPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading zip " + zipPath, e);
        }
    }

    public StageReport parseCsv(InputStream csvStream, ParseRequest request, Path canonicalRoot) {
        try (BucketedParquetWriter writer =
                new BucketedParquetWriter(canonicalRoot, request.competencia(), "socios", schema)) {
            return parseCsv(csvStream, request, writer);
        }
    }

    public StageReport parseCsv(InputStream csvStream, ParseRequest request, BucketedParquetWriter writer) {
        long linesRead = 0;
        long linesAccepted = 0;
        long linesRejected = 0;

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(csvStream, IngestionConstants.SOURCE_CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                linesRead++;
                if (line.isBlank()) {
                    linesRejected++;
                    continue;
                }
                try {
                    GenericRecord record = toRecord(line, linesRead, request);
                    int bucket = CnpjPartitioner.bucket(record.get("cnpj_basico").toString());
                    writer.write(record, bucket);
                    linesAccepted++;
                } catch (IllegalArgumentException | ParseStageException ex) {
                    linesRejected++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed parsing socios CSV", e);
        }

        StageReport report = new StageReport(linesRead, linesAccepted, linesRejected, Map.of(), 0);
        report.assertRejectionWithinLimit();
        return report;
    }

    private GenericRecord toRecord(String line, long lineNumber, ParseRequest request) {
        List<String> fields = CsvLineParser.parseFields(line);
        if (fields.size() != IngestionConstants.SOCIOS_CSV_COLUMNS) {
            throw new ParseStageException(
                    "Line " + lineNumber + " has " + fields.size() + " columns, expected "
                            + IngestionConstants.SOCIOS_CSV_COLUMNS);
        }

        String cnpjBasico = requireField(fields.get(0), "cnpj_basico", 8);
        String identificadorSocio = emptyToNull(fields.get(1));
        String nomeSocio = emptyToNull(fields.get(2));
        String cpfCnpjSocio = emptyToNull(fields.get(3));
        String identityKey = SocioIdentityResolver.identityKey(
                cnpjBasico, identificadorSocio, nomeSocio, cpfCnpjSocio);

        GenericRecord record = new GenericData.Record(schema);
        record.put("competencia", request.competencia());
        record.put("arquivo_origem", request.arquivoOrigem());
        record.put("sha256_origem", request.sha256Origem());
        record.put("linha_origem", lineNumber);
        record.put("cnpj_basico", cnpjBasico);
        record.put("socio_identity_key", identityKey);
        record.put("identificador_socio", identificadorSocio);
        record.put("nome_socio", nomeSocio);
        record.put("cpf_cnpj_socio", cpfCnpjSocio);
        record.put("qualificacao_socio", emptyToNull(fields.get(4)));
        record.put("data_entrada_sociedade", emptyToNull(fields.get(5)));
        record.put("pais", emptyToNull(fields.get(6)));
        record.put("representante_legal", emptyToNull(fields.get(7)));
        record.put("nome_representante", emptyToNull(fields.get(8)));
        record.put("qualificacao_representante_legal", emptyToNull(fields.get(9)));
        record.put("faixa_etaria", emptyToNull(fields.get(10)));
        return record;
    }

    private static String requireField(String value, String name, int length) {
        if (value == null || value.isBlank()) {
            throw new ParseStageException("Missing required field: " + name);
        }
        if (value.length() != length) {
            throw new ParseStageException(name + " must have " + length + " characters");
        }
        return value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
