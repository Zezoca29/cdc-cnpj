package br.univesp.tcc.cdccnpj.ingestion.quality;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record QualityReport(
        String schemaVersion,
        String competencia,
        String geradoEm,
        double rejectionThreshold,
        long totalLinesRead,
        long totalLinesRejected,
        double rejectionRate,
        long municipioIbgeNaoResolvido,
        Map<String, ArquivoQuality> byArquivo,
        List<BucketQuality> buckets) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ArquivoQuality(
            long linesRead,
            long linesAccepted,
            long linesRejected,
            double rejectionRate,
            Map<String, Long> sentinelDates,
            long municipioIbgeNaoResolvido) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BucketQuality(String tabela, int bucketsPresent, boolean complete, List<Integer> missingBuckets) {}
}
