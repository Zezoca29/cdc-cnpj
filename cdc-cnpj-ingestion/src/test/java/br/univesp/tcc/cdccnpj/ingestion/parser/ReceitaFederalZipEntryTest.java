package br.univesp.tcc.cdccnpj.ingestion.parser;

import org.junit.jupiter.api.Test;

import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceitaFederalZipEntryTest {

    @Test
    void acceptsReceitaFederalEntryNames() {
        assertTrue(ReceitaFederalZipEntry.isDataFile(entry("F.K03200$Z.D60509.CNAECSV")));
        assertTrue(ReceitaFederalZipEntry.isDataFile(entry("K3241.K03200Y0.D60509.EMPRECSV")));
        assertTrue(ReceitaFederalZipEntry.isDataFile(entry("K3241.K03200Y0.D60509.ESTABELE")));
        assertTrue(ReceitaFederalZipEntry.isDataFile(entry("F.K03200$Z.D60509.MUNICCSV")));
        assertTrue(ReceitaFederalZipEntry.isDataFile(entry("Cnaes.csv")));
    }

    @Test
    void rejectsDirectoriesAndMetadata() {
        assertFalse(ReceitaFederalZipEntry.isDataFile(dir("META-INF/")));
        assertFalse(ReceitaFederalZipEntry.isDataFile(entry("readme.txt")));
    }

    private static ZipEntry entry(String name) {
        return new ZipEntry(name);
    }

    private static ZipEntry dir(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setSize(0);
        return entry;
    }
}
