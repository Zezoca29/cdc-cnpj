package br.univesp.tcc.cdccnpj.diff.canonical;

import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.LocalInputFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CanonicalParquetReader {

    public List<Map<String, String>> readBucket(Path parquetFile) {
        if (!Files.exists(parquetFile)) {
            return List.of();
        }
        List<Map<String, String>> rows = new ArrayList<>();
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(
                        new LocalInputFile(parquetFile))
                .build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                rows.add(toStringMap(record));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + parquetFile, e);
        }
        return rows;
    }

    private static Map<String, String> toStringMap(GenericRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        record.getSchema().getFields().forEach(field -> {
            Object value = record.get(field.name());
            map.put(field.name(), value == null ? null : String.valueOf(value));
        });
        return map;
    }
}
