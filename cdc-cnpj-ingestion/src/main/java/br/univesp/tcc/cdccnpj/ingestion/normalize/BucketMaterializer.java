package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.apache.avro.Schema;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Garante os 256 buckets Parquet por tabela particionada (ADR-002).
 */
public final class BucketMaterializer {

    private static final Map<Schema, byte[]> EMPTY_TEMPLATES = new ConcurrentHashMap<>();

    private BucketMaterializer() {
    }

    public static void materializeMissing(Path canonicalRoot, String competencia, String tabela, Schema schema) {
        byte[] template = emptyTemplate(schema);
        for (int bucketId = 0; bucketId < CnpjPartitioner.BUCKET_COUNT; bucketId++) {
            Path parquetPath = bucketPath(canonicalRoot, competencia, tabela, bucketId);
            if (Files.exists(parquetPath)) {
                continue;
            }
            try {
                Files.createDirectories(parquetPath.getParent());
                Files.write(parquetPath, template);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed materializing bucket " + bucketId, e);
            }
        }
    }

    private static byte[] emptyTemplate(Schema schema) {
        return EMPTY_TEMPLATES.computeIfAbsent(schema, BucketMaterializer::writeEmptyTemplate);
    }

    private static byte[] writeEmptyTemplate(Schema schema) {
        try {
            Path tempFile = Files.createTempFile("empty-bucket-", ".parquet");
            Files.delete(tempFile);
            try (ParquetWriter<org.apache.avro.generic.GenericRecord> writer =
                    AvroParquetWriter.<org.apache.avro.generic.GenericRecord>builder(
                                    new LocalOutputFile(tempFile))
                            .withSchema(schema)
                            .withCompressionCodec(CompressionCodecName.SNAPPY)
                            .build()) {
                // empty bucket
            }
            byte[] bytes = Files.readAllBytes(tempFile);
            Files.deleteIfExists(tempFile);
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed creating empty parquet template", e);
        }
    }

    private static Path bucketPath(Path canonicalRoot, String competencia, String tabela, int bucketId) {
        return canonicalRoot
                .resolve("competencia=" + competencia)
                .resolve("tabela=" + tabela)
                .resolve("bucket=" + String.format("%03d", bucketId))
                .resolve("part-0.parquet");
    }
}
