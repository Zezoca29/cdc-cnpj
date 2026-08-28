package br.univesp.tcc.cdccnpj.bench.harness;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * CLI da Expansão X1 — diff em 256 buckets com validação H2.
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.FullScaleCli}
 */
public final class FullScaleCli {

    private FullScaleCli() {
    }

    public static void main(String[] args) throws Exception {
        Path canonicalRoot = Paths.get(optional(args, "--canonical-root", "data/canonical")).toAbsolutePath();
        String competenciaDe = optional(args, "--competencia-de", "2026-06");
        String competenciaPara = optional(args, "--competencia-para", "2026-07");
        EngineKind engine = EngineKind.valueOf(optional(args, "--engine", "E3"));
        Path output = Paths.get(optional(args, "--output", "data/measurements/full-scale-256.csv"));
        List<String> tabelas = List.of(optional(args, "--tabelas", "empresas,estabelecimentos").split(","));

        FullScaleDiffReport report = new FullScaleDiffBenchmark()
                .run(canonicalRoot, competenciaDe, competenciaPara, engine, tabelas);
        H2ValidationResult h2 = H2Validator.validate(report);

        FullScaleReportWriter.write(output.toAbsolutePath(), report, h2);
        System.out.println(h2.summary());
        System.out.printf(
                "Wrote %s and detail CSV (peak_heap=%d bytes, events=%d)%n",
                output.toAbsolutePath(), report.peakHeapBytes(), report.totalEventCount());

        if (!h2.passed()) {
            System.exit(1);
        }
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
