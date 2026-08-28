package br.univesp.tcc.cdccnpj.diff.e3;

import br.univesp.tcc.cdccnpj.diff.ChangeRecord;
import br.univesp.tcc.cdccnpj.diff.ChangeType;
import br.univesp.tcc.cdccnpj.diff.DiffEngine;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalParquetReader;
import br.univesp.tcc.cdccnpj.diff.canonical.CanonicalPaths;
import br.univesp.tcc.cdccnpj.diff.canonical.TableDefinitions;
import br.univesp.tcc.cdccnpj.diff.compare.RecordDiffSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Diff particionado por hash de conteúdo em memória (spec §8, E3).
 */
public final class HashDiffEngine implements DiffEngine {

    private final CanonicalParquetReader parquetReader = new CanonicalParquetReader();

    @Override
    public Stream<ChangeRecord> diff(
            Path canonicalRoot,
            String competenciaDe,
            String competenciaPara,
            String tabela,
            int bucketId) {
        List<String> columns = TableDefinitions.comparableColumns(tabela);
        List<String> keyColumns = TableDefinitions.naturalKeyColumns(tabela);

        Path sideDe = CanonicalPaths.bucketParquet(canonicalRoot, competenciaDe, tabela, bucketId);
        Path sidePara = CanonicalPaths.bucketParquet(canonicalRoot, competenciaPara, tabela, bucketId);

        Map<String, Map<String, String>> indexDe = indexRows(parquetReader.readBucket(sideDe), tabela);
        Map<String, Map<String, String>> indexPara = indexRows(parquetReader.readBucket(sidePara), tabela);

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

        return changes.stream().sorted(RecordDiffSupport.changeRecordOrder(keyColumns));
    }

    private static Map<String, Map<String, String>> indexRows(List<Map<String, String>> rows, String tabela) {
        Map<String, Map<String, String>> index = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            index.put(TableDefinitions.entityKeyToken(tabela, row), row);
        }
        return index;
    }
}
