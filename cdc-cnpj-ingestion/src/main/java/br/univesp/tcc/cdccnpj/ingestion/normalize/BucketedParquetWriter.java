package br.univesp.tcc.cdccnpj.ingestion.normalize;

import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Escrita Parquet particionada por bucket (CONTRATOS.md §3.3).
 */
public final class BucketedParquetWriter implements AutoCloseable {

    private final Path canonicalRoot;
    private final String competencia;
    private final String tabela;
    private final org.apache.avro.Schema schema;
    private final Map<Integer, ParquetWriter<GenericRecord>> writers = new HashMap<>();

    public BucketedParquetWriter(Path canonicalRoot, String competencia, String tabela, org.apache.avro.Schema schema) {
        this.canonicalRoot = canonicalRoot;
        this.competencia = competencia;
        this.tabela = tabela;
        this.schema = schema;
    }

    public void write(GenericRecord record, int bucketId) {
        try {
            ParquetWriter<GenericRecord> writer = writers.computeIfAbsent(bucketId, this::openWriter);
            writer.write(record);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing bucket " + bucketId, e);
        }
    }

    @Override
    public void close() {
        for (ParquetWriter<GenericRecord> writer : writers.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed closing parquet writer", e);
            }
        }
        writers.clear();
    }

    private Path bucketPath(int bucketId) {
        return canonicalRoot
                .resolve("competencia=" + competencia)
                .resolve("tabela=" + tabela)
                .resolve("bucket=" + String.format("%03d", bucketId))
                .resolve("part-0.parquet");
    }

    private ParquetWriter<GenericRecord> openWriter(int bucketId) {
        try {
            Path parquetPath = bucketPath(bucketId);
            Files.createDirectories(parquetPath.getParent());
            return AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(parquetPath))
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed opening writer for bucket " + bucketId, e);
        }
    }
}
