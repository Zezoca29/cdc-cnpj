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

/**
 * Escrita Parquet sem bucket para tabelas de domínio (CONTRATOS.md §3.5).
 */
public final class FlatParquetWriter implements AutoCloseable {

    private final ParquetWriter<GenericRecord> writer;

    public FlatParquetWriter(
            Path canonicalRoot, String competencia, String tabela, org.apache.avro.Schema schema) {
        try {
            Path directory = canonicalRoot.resolve("competencia=" + competencia).resolve("tabela=" + tabela);
            Files.createDirectories(directory);
            Path parquetPath = directory.resolve("part-0.parquet");
            writer = AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(parquetPath))
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed opening parquet writer for " + tabela, e);
        }
    }

    public void write(GenericRecord record) {
        try {
            writer.write(record);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing record", e);
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed closing parquet writer", e);
        }
    }
}
