package br.univesp.tcc.cdccnpj.bench;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gera fixtures Parquet em {@code data/fixtures/} (gitignored exceto manifesto).
 *
 * <p>Executar na raiz do repositório:
 * {@code mvn -pl cdc-cnpj-bench exec:java}
 */
public final class FixtureGeneratorCli {

    public static void main(String[] args) {
        Path root = Paths.get("data", "fixtures");
        SyntheticFixtures fixtures = new SyntheticSnapshotGenerator().generate();
        new ParquetFixtureWriter().writeFixtures(root, fixtures);
        FixtureManifestWriter.write(root, fixtures);
        System.out.println("Fixtures written to " + root.toAbsolutePath());
        System.out.println("Expected changes: " + fixtures.expectedChanges().size());
    }
}
