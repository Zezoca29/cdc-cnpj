package br.univesp.tcc.cdccnpj.diff.e1;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;
import br.univesp.tcc.cdccnpj.diff.canonical.TableDefinitions;
import br.univesp.tcc.cdccnpj.diff.compare.RecordDiffSupport;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Baseline relacional: FULL OUTER JOIN por bucket no PostgreSQL (CONTRATOS.md §5, E1).
 */
public final class PostgresDiffEngine implements DiffEngine {

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;
    private final CanonicalParquetReader parquetReader = new CanonicalParquetReader();

    public PostgresDiffEngine(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    public static PostgresDiffEngine localhostDefaults() {
        return new PostgresDiffEngine(
                "jdbc:postgresql://127.0.0.1:5434/cdc_cnpj", "cdc", "cdc");
    }

    @Override
    public Stream<ChangeRecord> diff(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            int bucketId) {
        List<String> columns = TableDefinitions.comparableColumns(tabela);
        List<String> keyColumns = TableDefinitions.naturalKeyColumns(tabela);

        Path sideDe = CanonicalPaths.bucketParquet(canonicalRoot, competenciaDe, tabela, bucketId);
        Path sidePara = CanonicalPaths.bucketParquet(canonicalRoot, competenciaPara, tabela, bucketId);

        List<Map<String, String>> rowsDe = parquetReader.readBucket(sideDe);
        List<Map<String, String>> rowsPara = parquetReader.readBucket(sidePara);

        String suffix = Long.toHexString(System.nanoTime());
        String tableDe = "diff_a_" + suffix;
        String tablePara = "diff_b_" + suffix;

        try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
            connection.setAutoCommit(false);
            createTempTable(connection, tableDe, columns, keyColumns);
            createTempTable(connection, tablePara, columns, keyColumns);
            loadRows(connection, tableDe, columns, rowsDe);
            loadRows(connection, tablePara, columns, rowsPara);

            List<ChangeRecord> changes = queryChanges(
                    connection, tableDe, tablePara, tabela, bucketId, competenciaDe, competenciaPara, columns, keyColumns);
            connection.commit();
            return changes.stream().sorted(RecordDiffSupport.changeRecordOrder(keyColumns));
        } catch (SQLException e) {
            throw new IllegalStateException("E1 diff failed for " + tabela + " bucket " + bucketId, e);
        }
    }

    private void createTempTable(
            Connection connection, String tableName, List<String> columns, List<String> keyColumns)
            throws SQLException {
        StringBuilder ddl = new StringBuilder("CREATE TEMP TABLE ")
                .append(tableName)
                .append(" (");
        ddl.append(keyColumns.stream()
                .map(k -> k + " VARCHAR(14) NOT NULL")
                .collect(Collectors.joining(", ")));
        for (String column : columns) {
            if (keyColumns.contains(column)) {
                continue;
            }
            ddl.append(", ").append(column).append(" TEXT");
        }
        ddl.append(", PRIMARY KEY (")
                .append(String.join(", ", keyColumns))
                .append(")) ON COMMIT DROP");
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl.toString());
        }
    }

    private void loadRows(
            Connection connection, String tableName, List<String> columns, List<Map<String, String>> rows)
            throws SQLException {
        if (rows.isEmpty()) {
            return;
        }
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map<String, String> row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setString(i + 1, row.get(columns.get(i)));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<ChangeRecord> queryChanges(
            Connection connection,
            String tableDe,
            String tablePara,
            String tabela,
            int bucketId,
            String competenciaDe,
            String competenciaPara,
            List<String> columns,
            List<String> keyColumns) throws SQLException {
        String joinCondition = keyColumns.stream()
                .map(k -> "a." + k + " = b." + k)
                .collect(Collectors.joining(" AND "));
        String distinctClause = columns.stream()
                .filter(c -> !keyColumns.contains(c))
                .map(c -> "a." + c + " IS DISTINCT FROM b." + c)
                .collect(Collectors.joining(" OR "));
        String keySelect = keyColumns.stream().map(k -> "COALESCE(a." + k + ", b." + k + ") AS " + k)
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT
                  CASE
                    WHEN a.%1$s IS NULL THEN 'INSERT'
                    WHEN b.%1$s IS NULL THEN 'DELETE'
                    ELSE 'UPDATE'
                  END AS change_type,
                  %2$s
                FROM %3$s a
                FULL OUTER JOIN %4$s b ON %5$s
                WHERE a.%1$s IS NULL OR b.%1$s IS NULL OR (%6$s)
                """
                .formatted(
                        keyColumns.getFirst(),
                        keySelect,
                        tableDe,
                        tablePara,
                        joinCondition,
                        distinctClause.isEmpty() ? "FALSE" : distinctClause);

        List<ChangeRecord> result = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                ChangeType changeType = ChangeType.valueOf(rs.getString("change_type"));
                Map<String, String> key = new LinkedHashMap<>();
                for (String keyColumn : keyColumns) {
                    key.put(keyColumn, rs.getString(keyColumn));
                }
                Map<String, String> before = fetchRow(connection, tableDe, keyColumns, key, columns);
                Map<String, String> after = fetchRow(connection, tablePara, keyColumns, key, columns);
                List<String> changedFields =
                        RecordDiffSupport.changedFields(columns, keyColumns, before, after);
                result.add(new ChangeRecord(
                        changeType,
                        tabela,
                        bucketId,
                        TableDefinitions.entityKey(tabela, changeType == ChangeType.DELETE ? before : after),
                        before == null ? Map.of() : RecordDiffSupport.projectComparable(before, columns),
                        after == null ? Map.of() : RecordDiffSupport.projectComparable(after, columns),
                        changedFields,
                        competenciaDe,
                        competenciaPara));
            }
        }
        return result;
    }

    private Map<String, String> fetchRow(
            Connection connection,
            String tableName,
            List<String> keyColumns,
            Map<String, String> key,
            List<String> columns) throws SQLException {
        String where = keyColumns.stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = "SELECT " + String.join(", ", columns) + " FROM " + tableName + " WHERE " + where;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            for (String keyColumn : keyColumns) {
                ps.setString(idx++, key.get(keyColumn));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, rs.getString(column));
                }
                return row;
            }
        }
    }
}
