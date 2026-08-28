package br.univesp.tcc.cdccnpj.events.api;

/**
 * Servidor HTTP leve para o dashboard consultar {@code changelog.events}.
 *
 * <p>Uso: {@code mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=...ChangelogApiCli}
 */
public final class ChangelogApiCli {

    private ChangelogApiCli() {}

    public static void main(String[] args) throws Exception {
        DashboardApiConfig config = DashboardApiConfig.load(args);
        ChangelogRepository repository =
                new ChangelogRepository(config.jdbcUrl(), config.jdbcUser(), config.jdbcPassword());
        DashboardArtifactsRepository artifacts = new DashboardArtifactsRepository(config.dataRoot());

        try (ChangelogApiServer server = new ChangelogApiServer(config, repository, artifacts)) {
            server.start();
            System.out.printf(
                    "Dashboard API listening on http://%s:%d (data=%s)%n",
                    config.bindHost(), config.port(), config.dataRoot());
            Thread.currentThread().join();
        }
    }
}
