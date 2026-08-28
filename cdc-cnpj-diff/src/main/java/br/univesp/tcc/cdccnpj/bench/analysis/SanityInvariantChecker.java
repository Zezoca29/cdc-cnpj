package br.univesp.tcc.cdccnpj.bench.analysis;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Invariantes de sanidade com tolerância declarada (spec §10.3, D6).
 */
public final class SanityInvariantChecker {

    public static final long DEFAULT_TOLERANCE = 0;

    private final CanonicalParquetReader reader = new CanonicalParquetReader();

    public SanityInvariantReport check(
            DiffEngine engine,
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            List<Integer> bucketIds,
            long tolerance) {
        long inserts = 0;
        long deletes = 0;
        for (int bucketId : bucketIds) {
            List<ChangeRecord> changes = engine.diff(canonicalRoot, competenciaDe, competenciaPara, tabela, bucketId)
                    .toList();
            inserts += changes.stream().filter(change -> change.changeType() == ChangeType.INSERT).count();
            deletes += changes.stream().filter(change -> change.changeType() == ChangeType.DELETE).count();
        }

        long countDe = countRecords(canonicalRoot, competenciaDe, tabela, bucketIds);
        long countPara = countRecords(canonicalRoot, competenciaPara, tabela, bucketIds);
        long netCountDelta = countPara - countDe;
        long netEventDelta = inserts - deletes;

        List<SanityInvariantResult> results = new ArrayList<>();
        results.add(invariant("net_events_match_count_delta", netCountDelta, netEventDelta, tolerance));
        results.add(invariant("non_negative_inserts", 0, inserts, Long.MAX_VALUE));
        results.add(invariant("non_negative_deletes", 0, deletes, Long.MAX_VALUE));

        boolean allPassed = results.stream().allMatch(SanityInvariantResult::passed);
        return new SanityInvariantReport(competenciaDe, competenciaPara, tabela, List.copyOf(results), allPassed);
    }

    private long countRecords(Path canonicalRoot, String competencia, String tabela, List<Integer> bucketIds) {
        long total = 0;
        for (int bucketId : bucketIds) {
            Path parquet = CanonicalPaths.bucketParquet(canonicalRoot, competencia, tabela, bucketId);
            if (Files.isRegularFile(parquet)) {
                total += reader.readBucket(parquet).size();
            }
        }
        return total;
    }

    private static SanityInvariantResult invariant(String name, long expected, long actual, long tolerance) {
        long delta = Math.abs(expected - actual);
        boolean passed = delta <= tolerance;
        return new SanityInvariantResult(name, expected, actual, delta, passed);
    }
}
