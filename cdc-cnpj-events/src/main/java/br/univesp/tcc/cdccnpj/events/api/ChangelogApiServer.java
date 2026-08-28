package br.univesp.tcc.cdccnpj.events.api;

import br.univesp.tcc.cdccnpj.events.kafka.EventJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChangelogApiServer implements AutoCloseable {

    private final HttpServer server;
    private final ChangelogRepository repository;
    private final DashboardArtifactsRepository artifacts;
    private final ObjectMapper objectMapper;
    private final String corsOrigin;

    public ChangelogApiServer(
            DashboardApiConfig config, ChangelogRepository repository, DashboardArtifactsRepository artifacts)
            throws IOException {
        this.repository = repository;
        this.artifacts = artifacts;
        this.objectMapper = EventJsonMapper.create();
        this.corsOrigin = config.corsOrigin();
        this.server = HttpServer.create(new InetSocketAddress(config.bindHost(), config.port()), 0);
        this.server.createContext("/api/health", this::health);
        this.server.createContext("/api/stats", this::stats);
        this.server.createContext("/api/events", this::events);
        this.server.createContext("/api/events/by-entity", this::eventsByEntity);
        this.server.createContext("/api/events/by-type", this::eventsByType);
        this.server.createContext("/api/manifests", this::manifests);
        this.server.createContext("/api/quality", this::quality);
        this.server.createContext("/api/benchmarks", this::benchmarks);
        this.server.createContext("/api/pipeline/latest", this::pipelineLatest);
        this.server.createContext("/api/storage", this::storage);
    }

    public void start() {
        server.start();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void health(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        sendJson(exchange, 200, Map.of("status", "ok"));
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        exchange.sendResponseHeaders(204, -1);
        return true;
    }

    private void stats(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            ChangelogStats stats = repository.stats();
            List<EventTypeCount> byType = repository.countByEventType();
            Map<String, Object> body = new HashMap<>();
            body.put("totalEvents", stats.totalEvents());
            body.put("competenciaDe", stats.competenciaDe());
            body.put("competenciaPara", stats.competenciaPara());
            body.put("distinctEventTypes", stats.distinctEventTypes());
            body.put("byEventType", byType);
            body.put("byStrategy", repository.countByStrategy());
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void events(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            Map<String, String> params = queryParams(exchange.getRequestURI());
            EventQuery query = new EventQuery(
                    params.get("event_type"),
                    params.get("competencia_de"),
                    params.get("competencia_para"),
                    params.get("search"),
                    parseInt(params.get("page"), 0),
                    parseInt(params.get("size"), 50));
            EventPage page = repository.findEvents(query);
            sendJson(exchange, 200, page);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void eventsByEntity(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            Map<String, String> params = queryParams(exchange.getRequestURI());
            String entity = params.get("q");
            if (entity == null || entity.isBlank()) {
                sendError(exchange, 400, "Missing query parameter: q");
                return;
            }
            int limit = parseInt(params.get("limit"), 100);
            List<ChangelogEventRecord> items = repository.findByEntityKey(entity, limit);
            sendJson(exchange, 200, Map.of("items", items, "total", items.size()));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void eventsByType(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            List<EventTypeCount> counts = repository.countByEventType();
            sendJson(exchange, 200, Map.of("items", counts));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void manifests(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            Map<String, String> params = queryParams(exchange.getRequestURI());
            String competencia = params.get("competencia");
            if (competencia != null && !competencia.isBlank()) {
                sendJson(exchange, 200, artifacts.readManifest(competencia));
                return;
            }
            sendJson(exchange, 200, Map.of("items", artifacts.listManifests()));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void quality(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            Map<String, String> params = queryParams(exchange.getRequestURI());
            String competencia = params.get("competencia");
            if (competencia == null || competencia.isBlank()) {
                sendError(exchange, 400, "Missing query parameter: competencia");
                return;
            }
            sendJson(exchange, 200, artifacts.readQuality(competencia));
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void benchmarks(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            DashboardArtifactsRepository.BenchmarkPayload benchmarks = artifacts.readBenchmarks();
            Map<String, Object> body = new HashMap<>();
            body.put("sourceFile", benchmarks.sourceFile());
            body.put("summaries", benchmarks.summaries());
            body.put("rawRowCount", benchmarks.rawRowCount());
            artifacts.readFullScale().ifPresent(fullScale -> body.put("fullScale", fullScale));
            sendJson(exchange, 200, body);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void pipelineLatest(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            artifacts
                    .readPipelineLatest()
                    .ifPresentOrElse(
                            snapshot -> {
                                try {
                                    sendJson(exchange, 200, snapshot);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            },
                            () -> {
                                try {
                                    sendJson(exchange, 200, Map.of());
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        } catch (UncheckedIOException e) {
            sendError(exchange, 500, e.getCause().getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void storage(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            Map<String, String> params = queryParams(exchange.getRequestURI());
            String competencia = params.get("competencia");
            if (competencia == null || competencia.isBlank()) {
                ChangelogStats stats = repository.stats();
                competencia = stats.competenciaPara() != null ? stats.competenciaPara() : "2026-07";
            }
            long changelogBytes = repository.changelogTableBytes();
            DashboardArtifactsRepository.StorageSummary storage =
                    artifacts.readStorageSummary(competencia, changelogBytes);
            sendJson(exchange, 200, storage);
        } catch (Exception e) {
            sendError(exchange, 500, e.getMessage());
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = objectMapper.writeValueAsBytes(body);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCors(headers);
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, String> body = Map.of("error", message == null ? "Unknown error" : message);
        sendJson(exchange, status, body);
    }

    private static void addCors(Headers headers, String corsOrigin) {
        String origin = corsOrigin == null || corsOrigin.isBlank() ? "*" : corsOrigin;
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void addCors(Headers headers) {
        addCors(headers, corsOrigin);
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                params.put(
                        decode(pair.substring(0, eq)),
                        decode(pair.substring(eq + 1)));
            } else if (!pair.isBlank()) {
                params.put(decode(pair), "");
            }
        }
        return params;
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
