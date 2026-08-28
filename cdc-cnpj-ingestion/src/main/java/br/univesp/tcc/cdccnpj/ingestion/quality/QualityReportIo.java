package br.univesp.tcc.cdccnpj.ingestion.quality;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.normalize.CompetenciaNormalizationReport;
import br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizationValidator;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QualityReportIo {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private QualityReportIo() {}

    public static Path defaultPath(Path projectRoot, String competencia) {
        return projectRoot.resolve("data/manifests/competencia=" + competencia + "/quality.json");
    }

    public static QualityReport fromNormalization(
            CompetenciaNormalizationReport normalization,
            List<NormalizationValidator.BucketValidationReport> validations) {
        Map<String, QualityReport.ArquivoQuality> byArquivo = new LinkedHashMap<>();
        for (Map.Entry<String, StageReport> entry : normalization.reportsByArquivo().entrySet()) {
            StageReport report = entry.getValue();
            double rate = report.linesRead() == 0 ? 0 : (double) report.linesRejected() / report.linesRead();
            byArquivo.put(
                    entry.getKey(),
                    new QualityReport.ArquivoQuality(
                            report.linesRead(),
                            report.linesAccepted(),
                            report.linesRejected(),
                            rate,
                            report.sentinelDates(),
                            report.municipioIbgeNaoResolvido()));
        }

        long totalRead = normalization.totalLinesRead();
        long totalRejected = normalization.totalLinesRejected();
        double totalRate = totalRead == 0 ? 0 : (double) totalRejected / totalRead;

        List<QualityReport.BucketQuality> buckets = validations.stream()
                .map(v -> new QualityReport.BucketQuality(
                        v.tabela(), v.bucketsPresent(), v.complete(), v.missingBuckets()))
                .toList();

        return new QualityReport(
                IngestionConstants.SCHEMA_VERSION,
                normalization.competencia(),
                Instant.now().toString(),
                IngestionConstants.MAX_REJECTION_RATE,
                totalRead,
                totalRejected,
                totalRate,
                normalization.totalMunicipioIbgeNaoResolvido(),
                Map.copyOf(byArquivo),
                buckets);
    }

    public static void write(Path qualityPath, QualityReport report) {
        try {
            Files.createDirectories(qualityPath.getParent());
            MAPPER.writeValue(qualityPath.toFile(), report);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing quality report " + qualityPath, e);
        }
    }

    public static QualityReport read(Path qualityPath) {
        try {
            return MAPPER.readValue(Files.readString(qualityPath), QualityReport.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading quality report " + qualityPath, e);
        }
    }
}
