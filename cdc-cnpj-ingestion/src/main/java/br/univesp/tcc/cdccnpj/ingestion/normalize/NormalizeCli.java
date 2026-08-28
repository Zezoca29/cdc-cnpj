package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.ingestion.quality.QualityReportIo;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI para normalização completa de uma competência (A11).
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizeCli -Dexec.args="--competencia 2026-06"}
 */
public final class NormalizeCli {

    private NormalizeCli() {
    }

    public static void main(String[] args) throws Exception {
        String competencia = require(args, "--competencia");
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path rawDir = projectRoot.resolve("data/raw/competencia=" + competencia);
        Path manifestPath = projectRoot.resolve("data/manifests/competencia=" + competencia + "/manifesto.json");
        Path canonicalRoot = projectRoot.resolve("data/canonical");
        Path tabmun = optionalPath(args, "--tabmun", projectRoot.resolve("data/tabmun/tabmun.csv"));

        CompetenciaNormalizationReport report =
                new NormalizationRunner().normalize(rawDir, manifestPath, canonicalRoot, tabmun);

        System.out.printf(
                "competencia=%s arquivos=%d linhas_lidas=%d rejeitadas=%d municipio_ibge_nao_resolvido=%d%n",
                report.competencia(),
                report.reportsByArquivo().size(),
                report.totalLinesRead(),
                report.totalLinesRejected(),
                report.totalMunicipioIbgeNaoResolvido());

        if (hasFlag(args, "--validate")) {
            NormalizationValidator validator = new NormalizationValidator();
            var validations = validator.validate(canonicalRoot, competencia);
            for (NormalizationValidator.BucketValidationReport validation : validations) {
                System.out.printf(
                        "validate %s buckets=%d/256 complete=%s missing=%d%n",
                        validation.tabela(),
                        validation.bucketsPresent(),
                        validation.complete(),
                        validation.missingBuckets().size());
            }
            QualityReportIo.write(
                    QualityReportIo.defaultPath(projectRoot, competencia),
                    QualityReportIo.fromNormalization(report, validations));
        } else {
            QualityReportIo.write(
                    QualityReportIo.defaultPath(projectRoot, competencia),
                    QualityReportIo.fromNormalization(report, java.util.List.of()));
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

    private static String require(String[] args, String flag) {
        String value = optional(args, flag);
        if (value == null) {
            throw new IllegalArgumentException("Missing required flag " + flag);
        }
        return value;
    }

    private static String optional(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static Path optionalPath(String[] args, String flag, Path defaultPath) {
        String value = optional(args, flag);
        return value == null ? defaultPath : Paths.get(value);
    }
}
