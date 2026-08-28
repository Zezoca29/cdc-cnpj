package br.univesp.tcc.cdccnpj.ingestion.contract;

import br.univesp.tcc.cdccnpj.ingestion.IngestionConstants;
import br.univesp.tcc.cdccnpj.ingestion.parser.CsvLineParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.DominioParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.DominioTabela;
import br.univesp.tcc.cdccnpj.ingestion.parser.EmpresasParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.EstabelecimentosParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.ParseRequest;
import br.univesp.tcc.cdccnpj.ingestion.parser.SimplesParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.SociosParser;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LayoutContractTest {

    private static final String EMPRESA_VALID = "12345678;RAZAO;2062;49;1000,00;01;";
    private static final String ESTABELECIMENTO_VALID =
            "12345678;0001;95;1;LOJA;02;20190101;00;;105;20180601;6201500;;RUA;RUA A;100;;CENTRO;01310100;SP;7107;11;30000000;;;;;contato@test.org;;";
    private static final String DOMINIO_VALID = "6201500;Descricao";
    private static final String SOCIO_VALID = "12345678;1;JOAO DA SILVA;***111111**;49;20190101;105;;;;5";
    private static final String SIMPLES_VALID = "12345678;S;20190101;00000000;N;00000000;00000000";

    @Test
    void empresaLayoutHasSevenColumns() {
        assertEquals(IngestionConstants.EMPRESAS_CSV_COLUMNS, CsvLineParser.parseFields(EMPRESA_VALID).size());
    }

    @Test
    void estabelecimentoLayoutHasThirtyColumns() {
        assertEquals(
                IngestionConstants.ESTABELECIMENTOS_CSV_COLUMNS,
                CsvLineParser.parseFields(ESTABELECIMENTO_VALID).size());
    }

    @Test
    void dominioLayoutHasTwoColumns() {
        assertEquals(IngestionConstants.DOMINIO_CSV_COLUMNS, CsvLineParser.parseFields(DOMINIO_VALID).size());
    }

    @Test
    void socioLayoutHasElevenColumns() {
        assertEquals(IngestionConstants.SOCIOS_CSV_COLUMNS, CsvLineParser.parseFields(SOCIO_VALID).size());
    }

    @Test
    void simplesLayoutHasSevenColumns() {
        assertEquals(IngestionConstants.SIMPLES_CSV_COLUMNS, CsvLineParser.parseFields(SIMPLES_VALID).size());
    }

    @Test
    void empresaParserRejectsWrongColumnCount(@TempDir Path canonicalRoot) {
        EmpresasParser parser = new EmpresasParser();
        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(
                        stream("12345678;curto"),
                        request(),
                        canonicalRoot));
    }

    @Test
    void estabelecimentoParserRejectsWrongColumnCount(@TempDir Path canonicalRoot) {
        EstabelecimentosParser parser = new EstabelecimentosParser();
        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(stream("12345678;0001;95;curto"), request(), canonicalRoot));
    }

    @Test
    void dominioParserRejectsWrongColumnCount(@TempDir Path canonicalRoot) {
        DominioParser parser = new DominioParser();
        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(
                        stream("6201500;ok;extra"), DominioTabela.CNAES, request(), canonicalRoot));
    }

    @Test
    void socioParserRejectsWrongColumnCount(@TempDir Path canonicalRoot) {
        SociosParser parser = new SociosParser();
        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(stream("12345678;1;curto"), request(), canonicalRoot));
    }

    @Test
    void simplesParserRejectsWrongColumnCount(@TempDir Path canonicalRoot) {
        SimplesParser parser = new SimplesParser();
        assertThrows(
                ParseStageException.class,
                () -> parser.parseCsv(stream("12345678;S;curto"), request(), canonicalRoot));
    }

    private static ParseRequest request() {
        return new ParseRequest("2026-06", "contract-test.csv", "sha-test");
    }

    private static ByteArrayInputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.ISO_8859_1));
    }
}
