package br.univesp.tcc.cdccnpj.events.pipeline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

final class PipelineJdbc {

    static final String URL = "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj";
    static final String USER = "cdc";
    static final String PASSWORD = "cdc";

    private PipelineJdbc() {
    }

    static void truncateChangelog() throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE changelog.events");
        }
    }
}
