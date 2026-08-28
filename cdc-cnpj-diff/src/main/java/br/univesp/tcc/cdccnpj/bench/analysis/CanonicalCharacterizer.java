package br.univesp.tcc.cdccnpj.bench.analysis;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Caracterização de snapshots canônicos: contagens por tabela e distribuição por bucket (D4).
 */
public final class CanonicalCharacterizer {

    private static final List<String> PARTITIONED_TABLES = List.of("empresas", "estabelecimentos");
    private static final List<String> DOMAIN_TABLES =
            List.of("cnaes", "municipios", "motivos", "naturezas", "qualificacoes", "paises");

    private final CanonicalParquetReader reader = new CanonicalParquetReader();

    public List<TableCharacterization> characterize(Path canonicalRoot, String competencia) throws IOException {
        List<TableCharacterization> results = new ArrayList<>();
        for (String tabela : PARTITIONED_TABLES) {
            results.add(characterizePartitioned(canonicalRoot, competencia, tabela));
        }
        for (String tabela : DOMAIN_TABLES) {
            results.add(characterizeDomain(canonicalRoot, competencia, tabela));
        }
        return List.copyOf(results);
    }

    public double changeRate(
            Path canonicalRoot, String competenciaDe, String competenciaPara, String tabela, List<Integer> bucketIds) {
        long totalRecords = 0;
        long changedRecords = 0;
        for (int bucketId : bucketIds) {
            Path sideDe = CanonicalPaths.bucketParquet(canonicalRoot, competenciaDe, tabela, bucketId);
            Path sidePara = CanonicalPaths.bucketParquet(canonicalRoot, competenciaPara, tabela, bucketId);
            int countDe = reader.readBucket(sideDe).size();
            int countPara = reader.readBucket(sidePara).size();
            totalRecords += Math.max(countDe, countPara);
            changedRecords += Math.abs(countPara - countDe);
        }
        if (totalRecords == 0) {
            return 0.0;
        }
        return (double) changedRecords / totalRecords;
    }

    private TableCharacterization characterizePartitioned(Path canonicalRoot, String competencia, String tabela)
            throws IOException {
        Map<Integer, Long> perBucket = new TreeMap<>();
        long total = 0;
        for (int bucketId = 0; bucketId < CnpjPartitioner.BUCKET_COUNT; bucketId++) {
            Path parquet = CanonicalPaths.bucketParquet(canonicalRoot, competencia, tabela, bucketId);
            if (!Files.isRegularFile(parquet)) {
                continue;
            }
            long count = reader.readBucket(parquet).size();
            if (count > 0) {
                perBucket.put(bucketId, count);
                total += count;
            }
        }
        return new TableCharacterization(competencia, tabela, total, perBucket.size(), Map.copyOf(perBucket));
    }

    private TableCharacterization characterizeDomain(Path canonicalRoot, String competencia, String tabela)
            throws IOException {
        Path tableDir = canonicalRoot.resolve("competencia=" + competencia).resolve("tabela=" + tabela);
        long total = 0;
        if (Files.isDirectory(tableDir)) {
            try (Stream<Path> files = Files.walk(tableDir)) {
                total = files.filter(path -> path.getFileName().toString().endsWith(".parquet"))
                        .mapToLong(path -> reader.readBucket(path).size())
                        .sum();
            }
        }
        return new TableCharacterization(competencia, tabela, total, total > 0 ? 1 : 0, Map.of());
    }

    public static String formatReport(List<TableCharacterization> rows) {
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "%-14s %-18s %12s %8s%n", "competencia", "tabela", "records", "buckets"));
        for (TableCharacterization row : rows) {
            out.append(String.format(
                    Locale.ROOT,
                    "%-14s %-18s %12d %8d%n",
                    row.competencia(),
                    row.tabela(),
                    row.recordCount(),
                    row.bucketsPresent()));
        }
        return out.toString();
    }
}
