package br.univesp.tcc.cdccnpj.events.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Configuração da API do dashboard via variáveis de ambiente e flags CLI. */
public record DashboardApiConfig(
        String bindHost,
        int port,
        String jdbcUrl,
        String jdbcUser,
        String jdbcPassword,
        Path projectRoot,
        Path dataRoot,
        String corsOrigin) {

    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj";
    private static final String DEFAULT_JDBC_USER = "cdc";
    private static final String DEFAULT_JDBC_PASSWORD = "cdc";

    public static DashboardApiConfig load(String[] args) {
        int port = intEnv("CDC_API_PORT", 8080);
        String bindHost = env("CDC_API_BIND", "127.0.0.1");
        String jdbcUrl = env("CDC_JDBC_URL", DEFAULT_JDBC_URL);
        String jdbcUser = env("CDC_JDBC_USER", DEFAULT_JDBC_USER);
        String jdbcPassword = env("CDC_JDBC_PASSWORD", DEFAULT_JDBC_PASSWORD);
        Path projectRoot = Paths.get(env("CDC_PROJECT_ROOT", ".")).toAbsolutePath().normalize();
        Path dataRoot = resolveDataRoot(projectRoot);
        String corsOrigin = env("CDC_CORS_ORIGIN", null);

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--bind".equals(args[i]) && i + 1 < args.length) {
                bindHost = args[++i];
            } else if ("--project-root".equals(args[i]) && i + 1 < args.length) {
                projectRoot = Paths.get(args[++i]).toAbsolutePath().normalize();
                dataRoot = resolveDataRoot(projectRoot);
            } else if ("--data-root".equals(args[i]) && i + 1 < args.length) {
                dataRoot = Paths.get(args[++i]).toAbsolutePath().normalize();
            }
        }

        return new DashboardApiConfig(bindHost, port, jdbcUrl, jdbcUser, jdbcPassword, projectRoot, dataRoot, corsOrigin);
    }

    private static Path resolveDataRoot(Path projectRoot) {
        String override = env("CDC_DATA_ROOT", null);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        return projectRoot.resolve("data");
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int intEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}
