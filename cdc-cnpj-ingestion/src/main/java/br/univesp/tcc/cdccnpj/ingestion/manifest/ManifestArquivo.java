package br.univesp.tcc.cdccnpj.ingestion.manifest;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ManifestArquivo(
        String nome,
        String tabela,
        int parte,
        long tamanhoBytes,
        String sha256,
        String url,
        String baixadoEm,
        boolean espelho) {
}
