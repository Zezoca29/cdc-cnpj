package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestBuilder;
import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Manifesto;
import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizationRunnerTest {

    private static final String EMPRESA_LINE = "12345678;RAZAO;2062;49;1000,00;01;";
    private static final String ESTAB_LINE =
            "12345678;0001;95;1;LOJA;02;20190101;00;;105;20180601;6201500;;RUA;RUA A;100;;CENTRO;01310100;SP;7107;11;30000000;;;;;contato@test.org;;";
    private static final String CNAE_LINE = "6201500;Atividade teste";

    @Test
    void normalizesCompetenciaFromManifest(@TempDir Path rawDir, @TempDir Path manifestDir, @TempDir Path canonicalRoot)
            throws Exception {
        writeZip(rawDir.resolve("Empresas0.zip"), "Empresas0.csv", EMPRESA_LINE);
        writeZip(rawDir.resolve("Estabelecimentos0.zip"), "Estabelecimentos0.csv", ESTAB_LINE);
        writeZip(rawDir.resolve("Cnaes.zip"), "Cnaes.csv", CNAE_LINE);

        Manifesto manifesto = ManifestBuilder.fromRawDirectory(rawDir, "2026-06", "casa_dos_dados", "https://example");
        Path manifestPath = manifestDir.resolve("manifesto.json");
        ManifestIo.write(manifestPath, manifesto);

        Path tabmun = manifestDir.resolve("tabmun.csv");
        Files.writeString(
                tabmun,
                """
                CO_MUNICIPIO;NO_MUNICIPIO;SG_UF;CO_IBGE
                7107;SAO PAULO;SP;3550308
                """);

        CompetenciaNormalizationReport report =
                new NormalizationRunner().normalize(rawDir, manifestPath, canonicalRoot, tabmun);

        assertEquals(3, report.reportsByArquivo().size());
        assertEquals(3, report.totalLinesRead());
        assertEquals(0, report.totalLinesRejected());
        assertEquals(0, report.totalMunicipioIbgeNaoResolvido());

        int bucket = CnpjPartitioner.bucket("12345678");
        Path empresaParquet = canonicalRoot
                .resolve("competencia=2026-06/tabela=empresas/bucket=" + String.format("%03d", bucket) + "/part-0.parquet");
        Path estabParquet = canonicalRoot
                .resolve("competencia=2026-06/tabela=estabelecimentos/bucket=" + String.format("%03d", bucket) + "/part-0.parquet");
        Path cnaeParquet = canonicalRoot.resolve("competencia=2026-06/tabela=cnaes/part-0.parquet");

        assertTrue(Files.exists(empresaParquet));
        assertTrue(Files.exists(estabParquet));
        assertTrue(Files.exists(cnaeParquet));

        List<Map<String, String>> estabRows = new CanonicalParquetReader().readBucket(estabParquet);
        assertEquals("3550308", estabRows.getFirst().get("municipio_ibge"));
    }

    private static void writeZip(Path zipPath, String entryName, String csvLine) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(csvLine.getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }
    }
}
