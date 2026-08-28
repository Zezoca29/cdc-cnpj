package br.univesp.tcc.cdccnpj.ingestion.normalize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizationValidatorTest {

    @Test
    void incompleteWithPartialBuckets(@TempDir Path canonicalRoot) throws Exception {
        writeBucket(canonicalRoot, "2026-06", "empresas", 0);
        writeBucket(canonicalRoot, "2026-06", "estabelecimentos", 0);

        NormalizationValidator validator = new NormalizationValidator();
        assertFalse(validator.allComplete(canonicalRoot, java.util.List.of("2026-06")));
    }

    @Test
    void reportsMissingBuckets(@TempDir Path canonicalRoot) throws Exception {
        writeBucket(canonicalRoot, "2026-06", "empresas", 0);

        NormalizationValidator.BucketValidationReport report = new NormalizationValidator()
                .validate(canonicalRoot, "2026-06")
                .stream()
                .filter(row -> row.tabela().equals("empresas"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, report.bucketsPresent());
        assertFalse(report.complete());
    }

    private static void writeBucket(Path canonicalRoot, String competencia, String tabela, int bucket)
            throws Exception {
        Path parquet = canonicalRoot
                .resolve("competencia=" + competencia)
                .resolve("tabela=" + tabela)
                .resolve("bucket=" + String.format("%03d", bucket))
                .resolve("part-0.parquet");
        Files.createDirectories(parquet.getParent());
        Files.write(parquet, new byte[] {1});
    }
}
