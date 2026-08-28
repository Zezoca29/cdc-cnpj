package br.univesp.tcc.cdccnpj.ingestion.download;

import java.util.List;

/**
 * Espelho CDN da Receita Federal mantido pela Casa dos Dados (plano A da spec).
 */
public final class CasaDosDadosSource implements DownloadSource {

    public static final String BASE_URL = "https://dados-abertos-rf-cnpj.casadosdados.com.br/arquivos";

    @Override
    public List<DownloadTarget> targets(String snapshotDate, List<String> fileNames) {
        return fileNames.stream()
                .map(fileName -> new DownloadTarget(
                        fileName,
                        BASE_URL + "/" + snapshotDate + "/" + fileName,
                        true))
                .toList();
    }

    @Override
    public String origem() {
        return "casa_dos_dados";
    }

    @Override
    public String origemUrlBase() {
        return BASE_URL;
    }
}
