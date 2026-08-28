package br.univesp.tcc.cdccnpj.ingestion.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ManifestIo {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ManifestIo() {
    }

    public static Manifesto read(Path manifestPath) {
        try {
            return MAPPER.readValue(Files.readString(manifestPath), Manifesto.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading manifest " + manifestPath, e);
        }
    }

    public static void write(Path manifestPath, Manifesto manifesto) {
        try {
            Files.createDirectories(manifestPath.getParent());
            MAPPER.writeValue(manifestPath.toFile(), manifesto);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing manifest " + manifestPath, e);
        }
    }
}
