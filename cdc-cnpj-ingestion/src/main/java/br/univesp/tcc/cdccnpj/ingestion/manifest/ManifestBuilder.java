package br.univesp.tcc.cdccnpj.ingestion.manifest;

import br.univesp.tcc.cdccnpj.ingestion.parser.DominioTabela;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Gera manifesto.json a partir de arquivos ZIP em {@code data/raw/} (CONTRATOS.md §6).
 */
public final class ManifestBuilder {

    private ManifestBuilder() {
    }

    public static Manifesto fromRawDirectory(
            Path rawDirectory, String competencia, String origem, String origemUrlBase) {
        List<ManifestArquivo> arquivos = new ArrayList<>();
        try (Stream<Path> paths = Files.list(rawDirectory)) {
            paths.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> arquivos.add(toArquivo(path)));
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed listing " + rawDirectory, e);
        }
        return new Manifesto(
                "1.0",
                competencia,
                Instant.now().toString(),
                origem,
                origemUrlBase,
                List.copyOf(arquivos));
    }

    private static ManifestArquivo toArquivo(Path zipPath) {
        String nome = zipPath.getFileName().toString();
        String tabela = resolveTabela(nome);
        int parte = extractPart(nome);
        long tamanho = sizeOf(zipPath);
        String sha256 = Sha256Hasher.hashFile(zipPath);
        return new ManifestArquivo(nome, tabela, parte, tamanho, sha256, "", Instant.now().toString(), true);
    }

    private static String resolveTabela(String zipName) {
        String base = zipName.toLowerCase(Locale.ROOT);
        if (base.startsWith("estabelecimentos")) {
            return "estabelecimentos";
        }
        if (base.startsWith("empresas")) {
            return "empresas";
        }
        if (base.startsWith("socios")) {
            return "socios";
        }
        if (base.startsWith("simples")) {
            return "simples";
        }
        return DominioTabela.fromZipFileName(zipName).tabela();
    }

    private static int extractPart(String zipName) {
        int dot = zipName.lastIndexOf('.');
        String withoutExt = dot > 0 ? zipName.substring(0, dot) : zipName;
        int index = withoutExt.length() - 1;
        while (index >= 0 && Character.isDigit(withoutExt.charAt(index))) {
            index--;
        }
        if (index == withoutExt.length() - 1) {
            return 0;
        }
        return Integer.parseInt(withoutExt.substring(index + 1));
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed reading size of " + path, e);
        }
    }
}
