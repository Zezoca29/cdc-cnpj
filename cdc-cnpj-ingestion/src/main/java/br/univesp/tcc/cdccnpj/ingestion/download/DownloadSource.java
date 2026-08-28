package br.univesp.tcc.cdccnpj.ingestion.download;

import java.util.List;

public interface DownloadSource {

    List<DownloadTarget> targets(String snapshotDate, List<String> fileNames);

    String origem();

    String origemUrlBase();
}
