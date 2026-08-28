package br.univesp.tcc.cdccnpj.bench.harness;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * CLI do harness de medição (D2).
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCli}
 */
public final class BenchmarkCli {

    private BenchmarkCli() {
    }

    public static void main(String[] args) throws Exception {
        boolean definitive = hasFlag(args, "--protocol", "definitive");
        Path canonicalRoot = Paths.get(optional(args, "--canonical-root", "data/canonical"));
        String competenciaDe = optional(args, "--competencia-de", "2026-06");
        String competenciaPara = optional(args, "--competencia-para", "2026-07");
        Path output = Paths.get(optional(args, "--output", definitive
                ? "data/measurements/benchmark-definitive.csv"
                : "data/measurements/benchmark.csv"));
        List<EngineKind> engines = parseEngines(optional(
                args, "--engines", definitive ? "E1,E3" : "E3"));
        int repetitions = Integer.parseInt(optional(
                args, "--repetitions", Integer.toString(BenchmarkConstants.REPETITIONS_PER_CONFIG)));
        int warmupDiscarded = Integer.parseInt(optional(
                args, "--warmup-discard", Integer.toString(BenchmarkConstants.WARMUP_RUNS_DISCARDED)));
        int[] bucketScales = parseBucketScales(optional(
                args,
                "--bucket-scales",
                definitive ? "1,4,8,16" : "1"));
        List<String> tabelas = List.of(optional(args, "--tabelas", "empresas,estabelecimentos").split(","));

        BenchmarkMatrix matrix = new BenchmarkMatrix();
        List<MeasurementResult> results = matrix.run(
                canonicalRoot.toAbsolutePath(),
                competenciaDe,
                competenciaPara,
                tabelas,
                engines,
                bucketScales,
                repetitions,
                warmupDiscarded);

        BenchmarkCsvWriter.write(output.toAbsolutePath(), results);
        long measured = results.stream().filter(result -> !result.warmup()).count();
        System.out.printf(
                "Wrote %d rows (%d measured, %d warmup) to %s%n",
                results.size(), measured, results.size() - measured, output.toAbsolutePath());

        if (hasFlag(args, "--summarize") || definitive) {
            BenchmarkAnalyzer analyzer = new BenchmarkAnalyzer();
            System.out.println(BenchmarkAnalyzer.formatSummaries(analyzer.summarize(results)));
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

    private static boolean hasFlag(String[] args, String flag, String value) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i]) && value.equals(args[i + 1])) {
                return true;
            }
        }
        return false;
    }

    private static List<EngineKind> parseEngines(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).map(EngineKind::valueOf).toList();
    }

    private static int[] parseBucketScales(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
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
