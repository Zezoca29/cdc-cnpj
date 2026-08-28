package br.univesp.tcc.cdccnpj.ingestion.parser;

public record ParseRequest(String competencia, String arquivoOrigem, String sha256Origem) {

    public ParseRequest withArquivoOrigem(String arquivoOrigem) {
        return new ParseRequest(competencia, arquivoOrigem, sha256Origem);
    }
}
