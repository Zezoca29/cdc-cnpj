package br.univesp.tcc.cdccnpj.ingestion.municipio;

import br.univesp.tcc.cdccnpj.ingestion.download.DownloadTarget;
import br.univesp.tcc.cdccnpj.ingestion.download.HttpFileDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Baixa {@code tabmun.csv} do Tesouro Nacional (A8).
 *
 * <p>Fonte: Lista de municípios do SIAFI (CKAN STN).
 */
public final class TabmunDownloader {

    public static final String TABMUN_URL =
            "https://www.tesourotransparente.gov.br/ckan/dataset/abb968cb-3710-4f85-89cf-875c91b9c7f6/resource/eebb3bc6-9eea-4496-8bcf-304f33155282/download/tabmun.csv";

    private final HttpFileDownloader fileDownloader;

    public TabmunDownloader() {
        this(new HttpFileDownloader());
    }

    TabmunDownloader(HttpFileDownloader fileDownloader) {
        this.fileDownloader = fileDownloader;
    }

    public Path ensure(Path destination) throws IOException {
        if (isValid(destination)) {
            return destination;
        }
        Files.createDirectories(destination.getParent());
        fileDownloader.download(new DownloadTarget("tabmun.csv", TABMUN_URL, false), destination);
        if (!isValid(destination)) {
            throw new IOException("Downloaded tabmun.csv is missing or empty at " + destination);
        }
        return destination;
    }

    private static boolean isValid(Path destination) throws IOException {
        if (!Files.isRegularFile(destination) || Files.size(destination) == 0) {
            return false;
        }
        TabmunLoader.load(destination);
        return true;
    }
}
