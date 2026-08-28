package br.univesp.tcc.cdccnpj.ingestion.manifest;

import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256Hasher {

    private Sha256Hasher() {
    }

    public static String hashFile(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return hashStream(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed hashing " + file, e);
        }
    }

    public static String hashStream(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static void assertMatches(Path file, ManifestArquivo arquivo) {
        if (!Files.exists(file)) {
            throw new ParseStageException("Missing raw file: " + arquivo.nome());
        }
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading size of " + file, e);
        }
        if (size != arquivo.tamanhoBytes()) {
            throw new ParseStageException(
                    "Size mismatch for " + arquivo.nome() + ": expected " + arquivo.tamanhoBytes() + ", got " + size);
        }
        String actual = hashFile(file);
        if (!actual.equalsIgnoreCase(arquivo.sha256())) {
            throw new ParseStageException(
                    "SHA-256 mismatch for " + arquivo.nome() + ": expected " + arquivo.sha256() + ", got " + actual);
        }
    }
}
