package br.univesp.tcc.cdccnpj.diff.compare;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.diff.canonical.TableDefinitions;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RecordDiffSupport {

    private RecordDiffSupport() {
    }

    public static Map<String, String> projectComparable(Map<String, String> row, List<String> columns) {
        Map<String, String> projected = new LinkedHashMap<>();
        for (String column : columns) {
            if (!TableDefinitions.isProvenance(column)) {
                projected.put(column, row.get(column));
            }
        }
        return projected;
    }

    public static List<String> changedFields(
            List<String> columns, List<String> keyColumns, Map<String, String> before, Map<String, String> after) {
        if (before == null || after == null) {
            return List.of();
        }
        List<String> changed = new ArrayList<>();
        for (String column : columns) {
            if (keyColumns.contains(column) || TableDefinitions.isProvenance(column)) {
                continue;
            }
            if (!nullSafeEquals(before.get(column), after.get(column))) {
                changed.add(column);
            }
        }
        changed.sort(String::compareTo);
        return List.copyOf(changed);
    }

    public static long contentFingerprint(
            Map<String, String> row, List<String> columns, List<String> keyColumns) {
        var hasher = Hashing.murmur3_128().newHasher();
        for (String column : columns) {
            if (keyColumns.contains(column) || TableDefinitions.isProvenance(column)) {
                continue;
            }
            hasher.putString(column, StandardCharsets.UTF_8).putByte((byte) '=');
            String value = row.get(column);
            if (value != null) {
                hasher.putString(value, StandardCharsets.UTF_8);
            }
            hasher.putByte((byte) '|');
        }
        return hasher.hash().asLong();
    }

    public static ChangeRecord toChangeRecord(
            ChangeType changeType,
            String tabela,
            int bucketId,
            String competenciaDe,
            String competenciaPara,
            List<String> columns,
            List<String> keyColumns,
            Map<String, String> before,
            Map<String, String> after) {
        Map<String, String> sourceRow = changeType == ChangeType.DELETE ? before : after;
        return new ChangeRecord(
                changeType,
                tabela,
                bucketId,
                TableDefinitions.entityKeyFromColumns(keyColumns, sourceRow),
                before == null ? Map.of() : projectComparable(before, columns),
                after == null ? Map.of() : projectComparable(after, columns),
                changedFields(columns, keyColumns, before, after),
                competenciaDe,
                competenciaPara);
    }

    public static Comparator<ChangeRecord> changeRecordOrder(List<String> keyColumns) {
        return Comparator.comparing((ChangeRecord record) -> record.changeType().name())
                .thenComparing(record -> keyColumns.stream()
                        .map(key -> record.entityKey().getOrDefault(key, ""))
                        .collect(Collectors.joining("|")));
    }

    private static boolean nullSafeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
