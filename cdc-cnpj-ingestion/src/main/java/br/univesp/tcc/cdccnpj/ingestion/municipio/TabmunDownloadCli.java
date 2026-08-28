package br.univesp.tcc.cdccnpj.ingestion.municipio;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI para baixar {@code data/tabmun/tabmun.csv}.
 *
 * <p>Ex.: {@code mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunDownloadCli}
 */
public final class TabmunDownloadCli {

    private TabmunDownloadCli() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path destination = projectRoot.resolve("data/tabmun/tabmun.csv");
        for (int i = 0; i < args.length - 1; i++) {
            if ("--output".equals(args[i])) {
                destination = Paths.get(args[i + 1]);
            }
        }

        Path resolved = new TabmunDownloader().ensure(destination);
        System.out.printf("tabmun ready at %s%n", resolved.toAbsolutePath());
    }
}
