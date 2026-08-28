package br.univesp.tcc.cdccnpj.diff.compare;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;
import br.univesp.tcc.cdccnpj.diff.canonical.TableDefinitions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Diff de sócios com chave ingênua (cpf mascarado) — baseline para medir eventos fantasma (P2).
 */
public final class SocioNaiveDiff {

    private final CanonicalParquetReader parquetReader = new CanonicalParquetReader();

    public List<ChangeRecord> diffBucket(
            Path canonicalRoot, String competenciaDe, String competenciaPara, int bucketId) {
        String tabela = "socios";
        List<String> columns = TableDefinitions.comparableColumns(tabela);
        List<String> keyColumns = TableDefinitions.naiveNaturalKeyColumns(tabela);

        Path sideDe = CanonicalPaths.bucketParquet(canonicalRoot, competenciaDe, tabela, bucketId);
        Path sidePara = CanonicalPaths.bucketParquet(canonicalRoot, competenciaPara, tabela, bucketId);

        Map<String, Map<String, String>> indexDe = indexRows(parquetReader.readBucket(sideDe), keyColumns);
        Map<String, Map<String, String>> indexPara = indexRows(parquetReader.readBucket(sidePara), keyColumns);

        TreeSet<String> allKeys = new TreeSet<>();
        allKeys.addAll(indexDe.keySet());
        allKeys.addAll(indexPara.keySet());

        List<ChangeRecord> changes = new ArrayList<>();
        for (String keyToken : allKeys) {
            Map<String, String> before = indexDe.get(keyToken);
            Map<String, String> after = indexPara.get(keyToken);

            if (before == null) {
                changes.add(RecordDiffSupport.toChangeRecord(
                        ChangeType.INSERT,
                        tabela,
                        bucketId,
                        competenciaDe,
                        competenciaPara,
                        columns,
                        keyColumns,
                        null,
                        after));
            } else if (after == null) {
                changes.add(RecordDiffSupport.toChangeRecord(
                        ChangeType.DELETE,
                        tabela,
                        bucketId,
                        competenciaDe,
                        competenciaPara,
                        columns,
                        keyColumns,
                        before,
                        null));
            } else if (RecordDiffSupport.contentFingerprint(before, columns, keyColumns)
                    != RecordDiffSupport.contentFingerprint(after, columns, keyColumns)) {
                changes.add(RecordDiffSupport.toChangeRecord(
                        ChangeType.UPDATE,
                        tabela,
                        bucketId,
                        competenciaDe,
                        competenciaPara,
                        columns,
                        keyColumns,
                        before,
                        after));
            }
        }

        return changes.stream().sorted(RecordDiffSupport.changeRecordOrder(keyColumns)).toList();
    }

    private static Map<String, Map<String, String>> indexRows(
            List<Map<String, String>> rows, List<String> keyColumns) {
        Map<String, Map<String, String>> index = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            index.put(TableDefinitions.entityKeyTokenFromColumns(keyColumns, row), row);
        }
        return index;
    }
}
