package br.univesp.tcc.cdccnpj.ingestion.municipio;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.parser.CsvLineParser;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Carrega tabmun.csv do Tesouro Nacional.
 *
 * <p>Layout esperado (separador {@code ;}): {@code CO_MUNICIPIO;NO_MUNICIPIO;SG_UF;CO_IBGE}.
 * A primeira linha é ignorada se parecer cabeçalho.
 */
public final class TabmunLoader {

    private TabmunLoader() {
    }

    public static TabmunIndex load(Path tabmunCsv) {
        try (InputStream inputStream = Files.newInputStream(tabmunCsv)) {
            return load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading tabmun.csv at " + tabmunCsv, e);
        }
    }

    public static TabmunIndex load(InputStream tabmunCsv) {
        Map<String, String> siafiToIbge = new HashMap<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(tabmunCsv, IngestionConstants.SOURCE_CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = CsvLineParser.parseFields(line);
                if (fields.size() < 4) {
                    continue;
                }
                if (isHeader(fields)) {
                    continue;
                }
                String siafi = TabmunIndex.padSiafi(fields.get(0));
                String ibge = TabmunIndex.normalizeIbge(fields.get(3));
                siafiToIbge.put(siafi, ibge);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed parsing tabmun.csv", e);
        }
        if (siafiToIbge.isEmpty()) {
            throw new ParseStageException("tabmun.csv produced empty SIAFI→IBGE index");
        }
        return new TabmunIndex(siafiToIbge);
    }

    private static boolean isHeader(List<String> fields) {
        String first = fields.getFirst().toLowerCase(Locale.ROOT);
        return first.contains("mun") || first.contains("cod") || first.contains("siafi");
    }
}
