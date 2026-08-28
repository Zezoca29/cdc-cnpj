package br.univesp.tcc.cdccnpj.bench.harness;

/**
 * Valida hipótese H2: ciclo de diff em escala completa &lt; 24 h (spec §4, X1).
 */
public final class H2Validator {

    private H2Validator() {
    }

    public static H2ValidationResult validate(FullScaleDiffReport report) {
        double hours = report.totalWallTimeHours();
        boolean passed = hours < BenchmarkConstants.H2_ACCEPTANCE_HOURS;
        String summary = passed
                ? String.format(
                        "H2 PASSED: %.2f h < %.0f h (%d events, engine=%s)",
                        hours, (double) BenchmarkConstants.H2_ACCEPTANCE_HOURS, report.totalEventCount(), report.engine())
                : String.format(
                        "H2 FAILED: %.2f h >= %.0f h (%d events, engine=%s)",
                        hours, (double) BenchmarkConstants.H2_ACCEPTANCE_HOURS, report.totalEventCount(), report.engine());
        return new H2ValidationResult(
                passed, hours, BenchmarkConstants.H2_ACCEPTANCE_HOURS, report.totalEventCount(), summary);
    }
}
