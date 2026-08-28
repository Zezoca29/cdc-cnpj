package br.univesp.tcc.cdccnpj.ingestion.download;

public record DownloadTarget(String fileName, String url, boolean espelho) {
}
