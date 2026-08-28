package br.univesp.tcc.cdccnpj.ingestion.parser;

import br.univesp.tcc.cdccnpj.cnpj.CnpjValidator;
import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunIndex;
import br.univesp.tcc.cdccnpj.ingestion.normalize.BucketedParquetWriter;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CanonicalSchemas;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parser streaming ZIP→Parquet de estabelecimentos (CONTRATOS.md §3.5, R3).
 */
public final class EstabelecimentosParser {

    private final Schema schema = CanonicalSchemas.estabelecimentos();
    private final TabmunIndex tabmunIndex;

    public EstabelecimentosParser() {
        this(TabmunIndex.empty());
    }

    public EstabelecimentosParser(TabmunIndex tabmunIndex) {
        this.tabmunIndex = tabmunIndex;
    }

    public StageReport parseZip(Path zipPath, ParseRequest request, Path canonicalRoot) {
        try (BucketedParquetWriter writer = new BucketedParquetWriter(
                canonicalRoot, request.competencia(), "estabelecimentos", schema)) {
            return parseZip(zipPath, request, writer);
        }
    }

    public StageReport parseZip(Path zipPath, ParseRequest request, BucketedParquetWriter writer) {
        try (ZipInputStream zipInputStream = new ZipInputStream(java.nio.file.Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
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
        try (BucketedParquetWriter writer = new BucketedParquetWriter(
                canonicalRoot, request.competencia(), "estabelecimentos", schema)) {
            return parseCsv(csvStream, request, writer);
        }
    }

    public StageReport parseCsv(InputStream csvStream, ParseRequest request, BucketedParquetWriter writer) {
        long linesRead = 0;
        long linesAccepted = 0;
        long linesRejected = 0;
        long municipioIbgeNaoResolvido = 0;
        Map<String, Long> sentinelDates = new HashMap<>();

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
                    GenericRecord record = toRecord(line, linesRead, request, sentinelDates);
                    if (record.get("municipio_receita") != null && record.get("municipio_ibge") == null) {
                        municipioIbgeNaoResolvido++;
                    }
                    int bucket = CnpjPartitioner.bucket(record.get("cnpj_basico").toString());
                    writer.write(record, bucket);
                    linesAccepted++;
                } catch (IllegalArgumentException | ParseStageException ex) {
                    linesRejected++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed parsing estabelecimentos CSV", e);
        }

        StageReport report = new StageReport(
                linesRead, linesAccepted, linesRejected, Map.copyOf(sentinelDates), municipioIbgeNaoResolvido);
        report.assertRejectionWithinLimit();
        return report;
    }

    private GenericRecord toRecord(
            String line, long lineNumber, ParseRequest request, Map<String, Long> sentinelDates) {
        List<String> fields = CsvLineParser.parseFields(line);
        if (fields.size() != IngestionConstants.ESTABELECIMENTOS_CSV_COLUMNS) {
            throw new ParseStageException(
                    "Line " + lineNumber + " has " + fields.size() + " columns, expected "
                            + IngestionConstants.ESTABELECIMENTOS_CSV_COLUMNS);
        }

        String cnpjBasico = requireField(fields.get(0), "cnpj_basico", 8);
        String cnpjOrdem = requireField(fields.get(1), "cnpj_ordem", 4);
        String cnpjDv = requireField(fields.get(2), "cnpj_dv", 2);
        String cnpj = CnpjValidator.compose(cnpjBasico, cnpjOrdem, cnpjDv);

        GenericRecord record = new GenericData.Record(schema);
        record.put("competencia", request.competencia());
        record.put("arquivo_origem", request.arquivoOrigem());
        record.put("sha256_origem", request.sha256Origem());
        record.put("linha_origem", lineNumber);
        record.put("cnpj_basico", cnpjBasico);
        record.put("cnpj_ordem", cnpjOrdem);
        record.put("cnpj_dv", cnpjDv);
        record.put("cnpj", cnpj);
        record.put("identificador_matriz_filial", emptyToNull(fields.get(3)));
        record.put("nome_fantasia", emptyToNull(fields.get(4)));
        record.put("situacao_cadastral", emptyToNull(fields.get(5)));
        record.put(
                "data_situacao_cadastral",
                ReceitaDateParser.parse(fields.get(6), "data_situacao_cadastral", sentinelDates));
        record.put("motivo_situacao_cadastral", emptyToNull(fields.get(7)));
        record.put("nome_cidade_exterior", emptyToNull(fields.get(8)));
        record.put("pais", emptyToNull(fields.get(9)));
        record.put(
                "data_inicio_atividade",
                ReceitaDateParser.parse(fields.get(10), "data_inicio_atividade", sentinelDates));
        record.put("cnae_fiscal_principal", emptyToNull(fields.get(11)));
        record.put("cnae_fiscal_secundaria", emptyToNull(fields.get(12)));
        record.put("tipo_logradouro", emptyToNull(fields.get(13)));
        record.put("logradouro", emptyToNull(fields.get(14)));
        record.put("numero", emptyToNull(fields.get(15)));
        record.put("complemento", emptyToNull(fields.get(16)));
        record.put("bairro", emptyToNull(fields.get(17)));
        record.put("cep", emptyToNull(fields.get(18)));
        record.put("uf", emptyToNull(fields.get(19)));
        String municipioReceita = emptyToNull(fields.get(20));
        record.put("municipio_receita", municipioReceita);
        record.put("municipio_ibge", tabmunIndex.resolveIbge(municipioReceita).orElse(null));
        record.put("ddd_1", emptyToNull(fields.get(21)));
        record.put("telefone_1", emptyToNull(fields.get(22)));
        record.put("ddd_2", emptyToNull(fields.get(23)));
        record.put("telefone_2", emptyToNull(fields.get(24)));
        record.put("ddd_fax", emptyToNull(fields.get(25)));
        record.put("fax", emptyToNull(fields.get(26)));
        record.put("correio_eletronico", emptyToNull(fields.get(27)));
        record.put("situacao_especial", emptyToNull(fields.get(28)));
        record.put(
                "data_situacao_especial",
                ReceitaDateParser.parse(fields.get(29), "data_situacao_especial", sentinelDates));
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
