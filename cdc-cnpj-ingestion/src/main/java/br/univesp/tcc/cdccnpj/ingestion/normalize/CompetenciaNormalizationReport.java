package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;

import java.util.Map;

public record CompetenciaNormalizationReport(String competencia, Map<String, StageReport> reportsByArquivo) {

    public long totalLinesRead() {
        return reportsByArquivo.values().stream().mapToLong(StageReport::linesRead).sum();
    }

    public long totalLinesRejected() {
        return reportsByArquivo.values().stream().mapToLong(StageReport::linesRejected).sum();
    }

    public long totalMunicipioIbgeNaoResolvido() {
        return reportsByArquivo.values().stream().mapToLong(StageReport::municipioIbgeNaoResolvido).sum();
    }
}
