package br.univesp.tcc.cdccnpj.bench.harness;

import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class IoBytesEstimator {

    private IoBytesEstimator() {
    }

    static long estimateBytesRead(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            List<Integer> bucketIds) {
        long total = 0;
        for (int bucketId : bucketIds) {
            total += fileSizeIfExists(CanonicalPaths.bucketParquet(canonicalRoot, competenciaDe, tabela, bucketId));
            total += fileSizeIfExists(CanonicalPaths.bucketParquet(canonicalRoot, competenciaPara, tabela, bucketId));
        }
        return total;
    }

    private static long fileSizeIfExists(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }
        } catch (IOException ignored) {
            // treat missing/unreadable as zero — diff will fail loudly if required
        }
        return 0;
    }
}
