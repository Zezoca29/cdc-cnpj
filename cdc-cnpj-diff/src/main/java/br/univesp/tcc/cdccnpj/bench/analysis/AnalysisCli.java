package br.univesp.tcc.cdccnpj.bench.analysis;

import br.univesp.tcc.cdccnpj.bench.harness.BenchmarkAnalyzer;
import br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCsvReader;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * CLI unificado de análise: caracterização, invariantes, representatividade e amostragem (D4–D7).
 */
public final class AnalysisCli {

    private AnalysisCli() {
    }

    public static void main(String[] args) throws Exception {
        String mode = require(args, "--mode");
        Path canonicalRoot = Paths.get(optional(args, "--canonical-root", "data/fixtures")).toAbsolutePath();
        String competenciaDe = optional(args, "--competencia-de", "2026-06");
        String competenciaPara = optional(args, "--competencia-para", "2026-07");
        Path projectRoot = Paths.get("").toAbsolutePath();
        List<Integer> buckets = ExperimentBuckets.load(projectRoot);

        switch (mode) {
            case "characterize" -> runCharacterize(canonicalRoot, competenciaDe, competenciaPara, buckets);
            case "sanity" -> runSanity(canonicalRoot, competenciaDe, competenciaPara, buckets);
            case "representativeness" -> runRepresentativeness(canonicalRoot, competenciaDe, buckets);
            case "sample" -> runSample(canonicalRoot, competenciaDe, competenciaPara, buckets, args);
            case "summarize" -> runSummarize(args);
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    private static void runCharacterize(
            Path canonicalRoot, String competenciaDe, String competenciaPara, List<Integer> buckets)
            throws Exception {
        CanonicalCharacterizer characterizer = new CanonicalCharacterizer();
        System.out.println("=== D4: caracterização ===");
        System.out.println(CanonicalCharacterizer.formatReport(characterizer.characterize(canonicalRoot, competenciaDe)));
        System.out.println(CanonicalCharacterizer.formatReport(characterizer.characterize(canonicalRoot, competenciaPara)));
        for (String tabela : List.of("empresas", "estabelecimentos")) {
            double rate = characterizer.changeRate(canonicalRoot, competenciaDe, competenciaPara, tabela, buckets);
            System.out.printf("change_rate %s=%.4f%n", tabela, rate);
        }
    }

    private static void runSanity(
            Path canonicalRoot, String competenciaDe, String competenciaPara, List<Integer> buckets) {
        SanityInvariantChecker checker = new SanityInvariantChecker();
        HashDiffEngine engine = new HashDiffEngine();
        System.out.println("=== D6: invariantes de sanidade ===");
        for (String tabela : List.of("empresas", "estabelecimentos")) {
            SanityInvariantReport report =
                    checker.check(engine, canonicalRoot, competenciaDe, competenciaPara, tabela, buckets, 0);
            System.out.printf(
                    "%s passed=%s results=%s%n", tabela, report.allPassed(), report.results());
        }
    }

    private static void runRepresentativeness(Path canonicalRoot, String competencia, List<Integer> buckets)
            throws Exception {
        CanonicalCharacterizer characterizer = new CanonicalCharacterizer();
        BucketRepresentativenessChecker checker = new BucketRepresentativenessChecker();
        System.out.println("=== D7: representatividade 16 vs 256 ===");
        for (String tabela : List.of("empresas", "estabelecimentos")) {
            TableCharacterization stats = characterizer.characterize(canonicalRoot, competencia).stream()
                    .filter(row -> row.tabela().equals(tabela))
                    .findFirst()
                    .orElseThrow();
            RepresentativenessReport report =
                    checker.check(stats.recordsPerBucket(), buckets, BucketRepresentativenessChecker.DEFAULT_TOLERANCE);
            System.out.printf(
                    "%s within_tolerance=%s mean_dev=%.3f share_dev=%.3f%n",
                    tabela, report.withinTolerance(), report.relativeMeanDeviation(), report.shareDeviation());
        }
    }

    private static void runSample(
            Path canonicalRoot, String competenciaDe, String competenciaPara, List<Integer> buckets, String[] args)
            throws Exception {
        long seed = Long.parseLong(optional(args, "--seed", "20260827"));
        int sampleSize = Integer.parseInt(optional(args, "--sample-size", "200"));
        Path output = Paths.get(optional(args, "--output", "data/measurements/external-validation-sample.csv"));
        ExternalValidationSampler sampler = new ExternalValidationSampler();
        List<ExternalValidationSampler.SampledEvent> events = sampler.sample(
                new HashDiffEngine(),
                canonicalRoot,
                competenciaDe,
                competenciaPara,
                List.of("empresas", "estabelecimentos"),
                buckets,
                sampleSize,
                seed);
        ExternalValidationSampler.writeCsv(output.toAbsolutePath(), seed, events);
        System.out.printf("=== D5: amostra externa seed=%d events=%d output=%s%n", seed, events.size(), output);
    }

    private static void runSummarize(String[] args) throws Exception {
        Path input = Paths.get(optional(args, "--input", "data/measurements/benchmark.csv"));
        BenchmarkAnalyzer analyzer = new BenchmarkAnalyzer();
        System.out.println("=== D3: resumo experimental ===");
        System.out.println(BenchmarkAnalyzer.formatSummaries(analyzer.summarize(BenchmarkCsvReader.read(input))));
    }

    private static String require(String[] args, String flag) {
        String value = optional(args, flag, null);
        if (value == null) {
            throw new IllegalArgumentException("Missing required flag " + flag);
        }
        return value;
    }

    private static String optional(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
