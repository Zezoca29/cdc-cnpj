package br.univesp.tcc.cdccnpj.ingestion.report;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;

import java.util.Map;

public record StageReport(
        long linesRead,
        long linesAccepted,
        long linesRejected,
        Map<String, Long> sentinelDates,
        long municipioIbgeNaoResolvido) {

    public void assertRejectionWithinLimit() {
        if (linesRead == 0) {
            return;
        }
        double rejectionRate = (double) linesRejected / linesRead;
        if (rejectionRate > IngestionConstants.MAX_REJECTION_RATE) {
            throw new ParseStageException(
                    "Rejection rate %.4f%% exceeds limit %.4f%% (%d/%d lines)"
                            .formatted(
                                    rejectionRate * 100,
                                    IngestionConstants.MAX_REJECTION_RATE * 100,
                                    linesRejected,
                                    linesRead));
        }
    }
}
