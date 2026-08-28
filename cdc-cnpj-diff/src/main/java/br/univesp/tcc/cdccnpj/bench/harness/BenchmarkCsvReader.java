package br.univesp.tcc.cdccnpj.bench.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BenchmarkCsvReader {

    private BenchmarkCsvReader() {
    }

    public static List<MeasurementResult> read(Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return List.of();
        }
        List<MeasurementResult> results = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            results.add(new MeasurementResult(
                    Instant.parse(parts[0]),
                    parts[1],
                    EngineKind.valueOf(parts[2]),
                    parts[3],
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5]),
                    Boolean.parseBoolean(parts[6]),
                    (long) (Double.parseDouble(parts[7]) * 1_000_000),
                    (long) (Double.parseDouble(parts[8]) * 1_000_000),
                    Long.parseLong(parts[9]),
                    Long.parseLong(parts[10]),
                    Long.parseLong(parts[11]),
                    Long.parseLong(parts[12]),
                    Double.parseDouble(parts[13])));
        }
        return List.copyOf(results);
    }
}
