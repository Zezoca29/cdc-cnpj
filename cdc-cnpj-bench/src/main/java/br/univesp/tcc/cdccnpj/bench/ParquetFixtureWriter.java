package br.univesp.tcc.cdccnpj.bench;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Escreve fixtures sintéticas no layout canônico (CONTRATOS.md §3.3).
 */
public final class ParquetFixtureWriter {

    private static final String FIXTURE_SHA256 = "fixture-synthetic-v1";
    private static final String FIXTURE_ORIGEM = "synthetic-generator";

    public void writeFixtures(java.nio.file.Path root, SyntheticFixtures fixtures) {
        writeEmpresas(root, SyntheticSnapshotGenerator.COMPETENCIA_DE, fixtures.empresasDe());
        writeEmpresas(root, SyntheticSnapshotGenerator.COMPETENCIA_PARA, fixtures.empresasPara());
        writeEstabelecimentos(root, SyntheticSnapshotGenerator.COMPETENCIA_DE, fixtures.estabelecimentosDe());
        writeEstabelecimentos(root, SyntheticSnapshotGenerator.COMPETENCIA_PARA, fixtures.estabelecimentosPara());
    }

    public void writeEmpresas(java.nio.file.Path root, String competencia, List<SyntheticEmpresa> rows) {
        Map<Integer, List<SyntheticEmpresa>> byBucket = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> br.univesp.tcc.cdccnpj.partition.CnpjPartitioner.bucket(e.cnpjBasico())));
        byBucket.forEach((bucket, bucketRows) -> {
            java.nio.file.Path dir = root
                    .resolve("competencia=" + competencia)
                    .resolve("tabela=empresas")
                    .resolve("bucket=" + String.format("%03d", bucket));
            writeGenericRecords(dir, empresasSchema(), bucketRows.stream()
                    .map(row -> toEmpresaRecord(competencia, row))
                    .toList());
        });
    }

    public void writeEstabelecimentos(
            java.nio.file.Path root, String competencia, List<SyntheticEstabelecimento> rows) {
        Map<Integer, List<SyntheticEstabelecimento>> byBucket = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> br.univesp.tcc.cdccnpj.partition.CnpjPartitioner.bucket(e.cnpjBasico())));
        byBucket.forEach((bucket, bucketRows) -> {
            java.nio.file.Path dir = root
                    .resolve("competencia=" + competencia)
                    .resolve("tabela=estabelecimentos")
                    .resolve("bucket=" + String.format("%03d", bucket));
            writeGenericRecords(dir, estabelecimentosSchema(), bucketRows.stream()
                    .map(row -> toEstabelecimentoRecord(competencia, row))
                    .toList());
        });
    }

    private GenericRecord toEmpresaRecord(String competencia, SyntheticEmpresa row) {
        GenericRecord record = new GenericData.Record(empresasSchema());
        int line = 1;
        record.put("competencia", competencia);
        record.put("arquivo_origem", FIXTURE_ORIGEM);
        record.put("sha256_origem", FIXTURE_SHA256);
        record.put("linha_origem", (long) line);
        row.asMap().forEach(record::put);
        return record;
    }

    private GenericRecord toEstabelecimentoRecord(String competencia, SyntheticEstabelecimento row) {
        GenericRecord record = new GenericData.Record(estabelecimentosSchema());
        record.put("competencia", competencia);
        record.put("arquivo_origem", FIXTURE_ORIGEM);
        record.put("sha256_origem", FIXTURE_SHA256);
        record.put("linha_origem", 1L);
        row.asMap().forEach(record::put);
        return record;
    }

    private void writeGenericRecords(
            java.nio.file.Path dir, Schema schema, List<GenericRecord> records) {
        try {
            Files.createDirectories(dir);
            java.nio.file.Path parquetPath = dir.resolve("part-0.parquet");
            try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(
                            new LocalOutputFile(parquetPath))
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .build()) {
                for (GenericRecord record : records) {
                    writer.write(record);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Schema empresasSchema() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "EmpresaCanonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "razao_social", "type": ["null", "string"], "default": null},
                    {"name": "natureza_juridica", "type": ["null", "string"], "default": null},
                    {"name": "qualificacao_responsavel", "type": ["null", "string"], "default": null},
                    {"name": "capital_social", "type": ["null", "string"], "default": null},
                    {"name": "porte", "type": ["null", "string"], "default": null},
                    {"name": "ente_federativo_responsavel", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }

    static Schema estabelecimentosSchema() {
        return new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "EstabelecimentoCanonical",
                  "fields": [
                    {"name": "competencia", "type": "string"},
                    {"name": "arquivo_origem", "type": "string"},
                    {"name": "sha256_origem", "type": "string"},
                    {"name": "linha_origem", "type": "long"},
                    {"name": "cnpj_basico", "type": "string"},
                    {"name": "cnpj_ordem", "type": "string"},
                    {"name": "cnpj_dv", "type": "string"},
                    {"name": "cnpj", "type": "string"},
                    {"name": "identificador_matriz_filial", "type": ["null", "string"], "default": null},
                    {"name": "nome_fantasia", "type": ["null", "string"], "default": null},
                    {"name": "situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "data_situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "motivo_situacao_cadastral", "type": ["null", "string"], "default": null},
                    {"name": "nome_cidade_exterior", "type": ["null", "string"], "default": null},
                    {"name": "pais", "type": ["null", "string"], "default": null},
                    {"name": "data_inicio_atividade", "type": ["null", "string"], "default": null},
                    {"name": "cnae_fiscal_principal", "type": ["null", "string"], "default": null},
                    {"name": "cnae_fiscal_secundaria", "type": ["null", "string"], "default": null},
                    {"name": "tipo_logradouro", "type": ["null", "string"], "default": null},
                    {"name": "logradouro", "type": ["null", "string"], "default": null},
                    {"name": "numero", "type": ["null", "string"], "default": null},
                    {"name": "complemento", "type": ["null", "string"], "default": null},
                    {"name": "bairro", "type": ["null", "string"], "default": null},
                    {"name": "cep", "type": ["null", "string"], "default": null},
                    {"name": "uf", "type": ["null", "string"], "default": null},
                    {"name": "municipio_receita", "type": ["null", "string"], "default": null},
                    {"name": "municipio_ibge", "type": ["null", "string"], "default": null},
                    {"name": "ddd_1", "type": ["null", "string"], "default": null},
                    {"name": "telefone_1", "type": ["null", "string"], "default": null},
                    {"name": "ddd_2", "type": ["null", "string"], "default": null},
                    {"name": "telefone_2", "type": ["null", "string"], "default": null},
                    {"name": "ddd_fax", "type": ["null", "string"], "default": null},
                    {"name": "fax", "type": ["null", "string"], "default": null},
                    {"name": "correio_eletronico", "type": ["null", "string"], "default": null},
                    {"name": "situacao_especial", "type": ["null", "string"], "default": null},
                    {"name": "data_situacao_especial", "type": ["null", "string"], "default": null}
                  ]
                }
                """);
    }
}
