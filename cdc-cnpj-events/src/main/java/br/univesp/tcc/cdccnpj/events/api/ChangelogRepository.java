package br.univesp.tcc.cdccnpj.events.api;

import br.univesp.tcc.cdccnpj.events.kafka.EventJsonMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ChangelogRepository {

    private static final TypeReference<Map<String, String>> MAP_STRING =
            new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_STRING = new TypeReference<>() {};

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;
    private final ObjectMapper objectMapper;

    public ChangelogRepository(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
        this.objectMapper = EventJsonMapper.create();
    }

    public EventPage findEvents(EventQuery query) throws SQLException {
        String where = buildWhere(query);
        long total = count(where, query);
        List<ChangelogEventRecord> items = fetchPage(where, query);
        return new EventPage(items, total, query.page(), query.size());
    }

    public List<ChangelogEventRecord> findByEntityKey(String entitySearch, int limit) throws SQLException {
        String normalized = entitySearch.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
        String sql =
                """
                SELECT event_id, schema_version, event_type, entity_key,
                       competencia_de, competencia_para, campos_alterados,
                       before_state, after_state, detected_at,
                       pipeline_estrategia, pipeline_run_id, consumed_at
                FROM changelog.events
                WHERE entity_key->>'cnpj' = ?
                   OR entity_key->>'cnpj_basico' = ?
                   OR entity_key->>'cnpj' LIKE ?
                ORDER BY consumed_at DESC
                LIMIT ?
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            statement.setString(3, normalized + "%");
            statement.setInt(4, limit);
            return mapRows(statement.executeQuery());
        }
    }

    public ChangelogStats stats() throws SQLException {
        String sql =
                """
                SELECT COUNT(*) AS total,
                       MIN(competencia_de) AS competencia_de,
                       MAX(competencia_para) AS competencia_para,
                       COUNT(DISTINCT event_type) AS distinct_types
                FROM changelog.events
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            rs.next();
            return new ChangelogStats(
                    rs.getLong("total"),
                    rs.getString("competencia_de"),
                    rs.getString("competencia_para"),
                    rs.getLong("distinct_types"));
        }
    }

    public List<EventTypeCount> countByEventType() throws SQLException {
        String sql =
                """
                SELECT event_type, COUNT(*) AS cnt
                FROM changelog.events
                GROUP BY event_type
                ORDER BY cnt DESC
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            List<EventTypeCount> counts = new ArrayList<>();
            while (rs.next()) {
                counts.add(new EventTypeCount(rs.getString("event_type"), rs.getLong("cnt")));
            }
            return counts;
        }
    }

    private long count(String where, EventQuery query) throws SQLException {
        String sql = "SELECT COUNT(*) FROM changelog.events" + where;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilters(statement, query, 1);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private List<ChangelogEventRecord> fetchPage(String where, EventQuery query) throws SQLException {
        String sql =
                """
                SELECT event_id, schema_version, event_type, entity_key,
                       competencia_de, competencia_para, campos_alterados,
                       before_state, after_state, detected_at,
                       pipeline_estrategia, pipeline_run_id, consumed_at
                FROM changelog.events
                """
                        + where
                        + " ORDER BY consumed_at DESC LIMIT ? OFFSET ?";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int next = bindFilters(statement, query, 1);
            statement.setInt(next, query.size());
            statement.setInt(next + 1, query.page() * query.size());
            return mapRows(statement.executeQuery());
        }
    }

    private String buildWhere(EventQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (query.eventType() != null && !query.eventType().isBlank()) {
            where.append(" AND event_type = ?");
        }
        if (query.competenciaDe() != null && !query.competenciaDe().isBlank()) {
            where.append(" AND competencia_de = ?");
        }
        if (query.competenciaPara() != null && !query.competenciaPara().isBlank()) {
            where.append(" AND competencia_para = ?");
        }
        if (query.search() != null && !query.search().isBlank()) {
            where.append(
                    " AND (entity_key->>'cnpj' LIKE ? OR entity_key->>'cnpj_basico' LIKE ? OR event_id::text LIKE ?)");
        }
        return where.toString();
    }

    private int bindFilters(PreparedStatement statement, EventQuery query, int index) throws SQLException {
        if (query.eventType() != null && !query.eventType().isBlank()) {
            statement.setString(index++, query.eventType());
        }
        if (query.competenciaDe() != null && !query.competenciaDe().isBlank()) {
            statement.setString(index++, query.competenciaDe());
        }
        if (query.competenciaPara() != null && !query.competenciaPara().isBlank()) {
            statement.setString(index++, query.competenciaPara());
        }
        if (query.search() != null && !query.search().isBlank()) {
            String pattern = "%" + query.search().replaceAll("[^0-9A-Za-z]", "").toUpperCase() + "%";
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
        }
        return index;
    }

    private List<ChangelogEventRecord> mapRows(ResultSet rs) throws SQLException {
        List<ChangelogEventRecord> items = new ArrayList<>();
        while (rs.next()) {
            items.add(mapRow(rs));
        }
        return items;
    }

    private ChangelogEventRecord mapRow(ResultSet rs) throws SQLException {
        try {
            return new ChangelogEventRecord(
                    rs.getString("event_id"),
                    rs.getString("schema_version"),
                    rs.getString("event_type"),
                    objectMapper.readValue(rs.getString("entity_key"), MAP_STRING),
                    rs.getString("competencia_de"),
                    rs.getString("competencia_para"),
                    objectMapper.readValue(rs.getString("campos_alterados"), LIST_STRING),
                    readNullableMap(rs.getString("before_state")),
                    readNullableMap(rs.getString("after_state")),
                    toInstant(rs.getTimestamp("detected_at")),
                    rs.getString("pipeline_estrategia"),
                    rs.getString("pipeline_run_id"),
                    toInstant(rs.getTimestamp("consumed_at")));
        } catch (Exception e) {
            throw new SQLException("Failed to map changelog row", e);
        }
    }

    private Map<String, String> readNullableMap(String json) throws Exception {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return Map.of();
        }
        return objectMapper.readValue(json, MAP_STRING);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public List<StrategyCount> countByStrategy() throws SQLException {
        String sql =
                """
                SELECT pipeline_estrategia, COUNT(*) AS cnt
                FROM changelog.events
                GROUP BY pipeline_estrategia
                ORDER BY cnt DESC
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            List<StrategyCount> counts = new ArrayList<>();
            while (rs.next()) {
                counts.add(new StrategyCount(rs.getString("pipeline_estrategia"), rs.getLong("cnt")));
            }
            return counts;
        }
    }

    public long changelogTableBytes() throws SQLException {
        String sql = "SELECT pg_total_relation_size('changelog.events')";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    }
}
