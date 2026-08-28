package br.univesp.tcc.cdccnpj.bench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

final class FixtureManifestWriter {

    private FixtureManifestWriter() {
    }

    static void write(Path root, SyntheticFixtures fixtures) {
        String json = """
                {
                  "schema_version": "1.0",
                  "tipo": "synthetic-fixture",
                  "competencia_de": "%s",
                  "competencia_para": "%s",
                  "primary_bucket": "%03d",
                  "expected_change_count": %d,
                  "expected_changes": [%s]
                }
                """.formatted(
                SyntheticSnapshotGenerator.COMPETENCIA_DE,
                SyntheticSnapshotGenerator.COMPETENCIA_PARA,
                SyntheticSnapshotGenerator.PRIMARY_BUCKET,
                fixtures.expectedChanges().size(),
                fixtures.expectedChanges().stream()
                        .map(FixtureManifestWriter::toJson)
                        .collect(Collectors.joining(",\n    ")));
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve("manifesto.json"), json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String toJson(ExpectedChange change) {
        String fields = change.camposAlterados().stream()
                .map(f -> "\"" + f + "\"")
                .collect(Collectors.joining(", "));
        String entityKey = change.entityKey().entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\": \"" + e.getValue() + "\"")
                .collect(Collectors.joining(", "));
        return """
                {
                  "change_type": "%s",
                  "tabela": "%s",
                  "entity_key": {%s},
                  "campos_alterados": [%s]
                }"""
                .formatted(change.changeType(), change.tabela(), entityKey, fields);
    }
}
