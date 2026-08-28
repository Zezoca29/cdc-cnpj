package br.univesp.tcc.cdccnpj.ingestion.normalize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BucketMaterializerTest {

    @Test
    void materializesAll256BucketsAfterNormalization(
            @TempDir Path rawDir, @TempDir Path manifestDir, @TempDir Path canonicalRoot) throws Exception {
        writeZip(rawDir.resolve("Empresas0.zip"), "Empresas0.csv", "12345678;EMPRESA;2062;49;1000,00;01;");

        var manifesto = br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestBuilder.fromRawDirectory(
                rawDir, "2026-06", "test", "https://example");
        var manifestPath = manifestDir.resolve("manifesto.json");
        br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo.write(manifestPath, manifesto);

        new NormalizationRunner().normalize(rawDir, manifestPath, canonicalRoot, null);

        NormalizationValidator.BucketValidationReport report = new NormalizationValidator()
                .validate(canonicalRoot, "2026-06")
                .stream()
                .filter(row -> row.tabela().equals("empresas"))
                .findFirst()
                .orElseThrow();

        assertEquals(256, report.bucketsPresent());
        assertTrue(report.complete());
    }

    private static void writeZip(Path zipPath, String entryName, String csvLine) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(csvLine.getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }
    }
}
