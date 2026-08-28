package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida que a normalização produziu os 256 buckets esperados (A11).
 */
public final class NormalizationValidator {

    private static final List<String> NUCLEO_PARTITIONED_TABLES = List.of("empresas", "estabelecimentos");

    public record BucketValidationReport(
            String competencia, String tabela, int bucketsPresent, List<Integer> missingBuckets, boolean complete) {
    }

    public List<BucketValidationReport> validate(Path canonicalRoot, String competencia) throws IOException {
        List<BucketValidationReport> reports = new ArrayList<>();
        for (String tabela : partitionedTablesForCompetencia(canonicalRoot, competencia)) {
            List<Integer> missing = new ArrayList<>();
            int present = 0;
            for (int bucketId = 0; bucketId < CnpjPartitioner.BUCKET_COUNT; bucketId++) {
                Path parquet = canonicalRoot
                        .resolve("competencia=" + competencia)
                        .resolve("tabela=" + tabela)
                        .resolve("bucket=" + String.format("%03d", bucketId))
                        .resolve("part-0.parquet");
                if (Files.isRegularFile(parquet)) {
                    present++;
                } else {
                    missing.add(bucketId);
                }
            }
            reports.add(new BucketValidationReport(
                    competencia, tabela, present, List.copyOf(missing), missing.isEmpty()));
        }
        return List.copyOf(reports);
    }

    public boolean allComplete(Path canonicalRoot, List<String> competencias) throws IOException {
        for (String competencia : competencias) {
            for (BucketValidationReport report : validate(canonicalRoot, competencia)) {
                if (!report.complete()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<String> partitionedTablesForCompetencia(Path canonicalRoot, String competencia)
            throws IOException {
        List<String> tables = new ArrayList<>(NUCLEO_PARTITIONED_TABLES);
        Path sociosDir = canonicalRoot.resolve("competencia=" + competencia).resolve("tabela=socios");
        if (Files.isDirectory(sociosDir)) {
            tables.add("socios");
        }
        Path simplesDir = canonicalRoot.resolve("competencia=" + competencia).resolve("tabela=simples");
        if (Files.isDirectory(simplesDir)) {
            tables.add("simples");
        }
        return List.copyOf(tables);
    }
}
