package br.univesp.tcc.cdccnpj.bench.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Buckets experimentais n×16 (spec §9, {@code docs/experiment-buckets.txt}).
 */
public final class ExperimentBuckets {

    public static final List<Integer> DEFAULT_IDS =
            List.of(0, 16, 32, 48, 64, 80, 96, 112, 128, 144, 160, 176, 192, 208, 224, 240);

    private ExperimentBuckets() {
    }

    public static List<Integer> load(Path projectRoot) throws IOException {
        Path file = projectRoot.resolve("docs/experiment-buckets.txt");
        if (!Files.isRegularFile(file)) {
            return DEFAULT_IDS;
        }
        List<Integer> buckets = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            buckets.add(Integer.parseInt(trimmed));
        }
        return List.copyOf(buckets);
    }
}
