package br.univesp.tcc.cdccnpj.ingestion.manifest;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Manifesto(
        String schemaVersion,
        String competencia,
        String geradoEm,
        String origem,
        String origemUrlBase,
        List<ManifestArquivo> arquivos) {
}
