package br.univesp.tcc.cdccnpj.ingestion.pipeline;

import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunDownloader;
import br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizationValidator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Orquestra A11: download + normalização + validação das competências 2026-06 e 2026-07.
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli}
 */
public final class IngestionPipelineCli {

    public static final List<String> NUCLEO_COMPETENCIAS = List.of("2026-06", "2026-07");

    private IngestionPipelineCli() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        boolean download = !hasFlag(args, "--skip-download");
        boolean validate = !hasFlag(args, "--skip-validate");
        Path tabmun = optionalPath(args, "--tabmun", projectRoot.resolve("data/tabmun/tabmun.csv"));
        if (!hasFlag(args, "--skip-tabmun")) {
            tabmun = new TabmunDownloader().ensure(tabmun);
            System.out.println("tabmun=" + tabmun.toAbsolutePath());
        }

        CompetenciaPipeline pipeline = new CompetenciaPipeline();
        boolean allSuccess = true;

        for (String competencia : competencias(args)) {
            System.out.printf("=== competencia %s ===%n", competencia);
            CompetenciaPipelineResult result = pipeline.run(projectRoot, competencia, download, validate, tabmun);
            printResult(result);
            allSuccess = allSuccess && result.success();
        }

        if (!allSuccess) {
            System.exit(1);
        }
        System.out.println("A11 pipeline completed successfully for " + competencias(args));
    }

    private static List<String> competencias(String[] args) {
        String single = optionalValue(args, "--competencia", null);
        if (single != null) {
            return List.of(single);
        }
        return NUCLEO_COMPETENCIAS;
    }

    private static void printResult(CompetenciaPipelineResult result) {
        if (result.downloadReport() != null) {
            System.out.printf(
                    "download downloaded=%d skipped=%d%n",
                    result.downloadReport().downloaded().size(),
                    result.downloadReport().skipped().size());
        }
        System.out.printf(
                "normalize linhas_lidas=%d rejeitadas=%d%n",
                result.normalizationReport().totalLinesRead(),
                result.normalizationReport().totalLinesRejected());
        for (NormalizationValidator.BucketValidationReport validation : result.validations()) {
            System.out.printf(
                    "validate %s buckets=%d/256 complete=%s%n",
                    validation.tabela(),
                    validation.bucketsPresent(),
                    validation.complete());
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Path optionalPath(String[] args, String flag, Path defaultPath) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return Paths.get(args[i + 1]);
            }
        }
        return defaultPath;
    }

    private static String optionalValue(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
