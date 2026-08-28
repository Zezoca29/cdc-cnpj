package br.univesp.tcc.cdccnpj.ingestion.pipeline;

import br.univesp.tcc.cdccnpj.ingestion.download.DownloadReport;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CompetenciaNormalizationReport;
import br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizationValidator;

import java.util.List;

public record CompetenciaPipelineResult(
        String competencia,
        DownloadReport downloadReport,
        CompetenciaNormalizationReport normalizationReport,
        List<NormalizationValidator.BucketValidationReport> validations,
        boolean success) {
}
