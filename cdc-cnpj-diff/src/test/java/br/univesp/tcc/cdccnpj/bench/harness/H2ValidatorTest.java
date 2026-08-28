package br.univesp.tcc.cdccnpj.bench.harness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2ValidatorTest {

    @Test
    void passesWhenUnder24Hours() {
        H2ValidationResult result = H2Validator.validate(sampleReport(2 * 3_600_000_000_000L));
        assertTrue(result.passed());
        assertTrue(result.summary().contains("PASSED"));
    }

    @Test
    void failsWhenOver24Hours() {
        H2ValidationResult result = H2Validator.validate(sampleReport(25 * 3_600_000_000_000L));
        assertFalse(result.passed());
        assertTrue(result.summary().contains("FAILED"));
    }

    private static FullScaleDiffReport sampleReport(long wallNanos) {
        return new FullScaleDiffReport(
                "run-test",
                EngineKind.E3,
                "2026-06",
                "2026-07",
                wallNanos,
                wallNanos / 2,
                1024,
                2048,
                4096,
                100,
                java.util.List.of());
    }
}
