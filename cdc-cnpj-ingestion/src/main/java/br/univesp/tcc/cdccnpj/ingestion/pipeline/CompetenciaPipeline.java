package br.univesp.tcc.cdccnpj.ingestion.pipeline;

import br.univesp.tcc.cdccnpj.ingestion.download.CompetenciaDownloader;
import br.univesp.tcc.cdccnpj.ingestion.download.DownloadReport;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CompetenciaNormalizationReport;
import br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizationRunner;
import br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizationValidator;
import br.univesp.tcc.cdccnpj.ingestion.quality.QualityReport;
import br.univesp.tcc.cdccnpj.ingestion.quality.QualityReportIo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Pipeline ponta a ponta por competência: download → normalização → validação (A11).
 */
public final class CompetenciaPipeline {

    private final CompetenciaDownloader downloader = new CompetenciaDownloader();
    private final NormalizationRunner normalizationRunner = new NormalizationRunner();
    private final NormalizationValidator validator = new NormalizationValidator();

    public CompetenciaPipelineResult run(
            Path projectRoot, String competencia, boolean download, boolean validate, Path tabmunCsv)
            throws Exception {
        Path rawDir = projectRoot.resolve("data/raw/competencia=" + competencia);
        Path manifestPath = projectRoot.resolve("data/manifests/competencia=" + competencia + "/manifesto.json");
        Path canonicalRoot = projectRoot.resolve("data/canonical");

        DownloadReport downloadReport = null;
        if (download) {
            String snapshotDate = CompetenciaDownloader.resolveSnapshotDate(competencia);
            downloadReport = downloader.downloadCompetencia(competencia, snapshotDate, rawDir, manifestPath);
        } else if (!Files.isRegularFile(manifestPath)) {
            throw new IllegalStateException("Manifest missing for " + competencia + " — run with download enabled");
        }

        CompetenciaNormalizationReport normalizationReport =
                normalizationRunner.normalize(rawDir, manifestPath, canonicalRoot, tabmunCsv);

        List<NormalizationValidator.BucketValidationReport> validations = List.of();
        boolean success = normalizationReport.totalLinesRejected() == 0;
        if (validate) {
            validations = validator.validate(canonicalRoot, competencia);
            success = success && validations.stream().allMatch(NormalizationValidator.BucketValidationReport::complete);
        }

        QualityReport qualityReport = QualityReportIo.fromNormalization(normalizationReport, validations);
        QualityReportIo.write(QualityReportIo.defaultPath(projectRoot, competencia), qualityReport);

        return new CompetenciaPipelineResult(competencia, downloadReport, normalizationReport, validations, success);
    }
}
