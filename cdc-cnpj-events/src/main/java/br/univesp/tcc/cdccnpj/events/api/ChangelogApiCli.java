package br.univesp.tcc.cdccnpj.events.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servidor HTTP leve para o dashboard consultar {@code changelog.events}.
 *
 * <p>Uso: {@code mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=...ChangelogApiCli}
 */
public final class ChangelogApiCli {

    private static final String JDBC_URL = "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj";
    private static final String JDBC_USER = "cdc";
    private static final String JDBC_PASSWORD = "cdc";

    private ChangelogApiCli() {}

    public static void main(String[] args) throws Exception {
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }

        Path projectRoot = Paths.get("").toAbsolutePath();
        ChangelogRepository repository = new ChangelogRepository(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        DashboardArtifactsRepository artifacts = new DashboardArtifactsRepository(projectRoot);

        try (ChangelogApiServer server = new ChangelogApiServer(port, repository, artifacts)) {
            server.start();
            System.out.println("Changelog API listening on http://127.0.0.1:" + port);
            Thread.currentThread().join();
        }
    }
}
