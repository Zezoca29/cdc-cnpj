package br.univesp.tcc.cdccnpj.ingestion.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvLineParserTest {

    @Test
    void parsesSimpleSemicolonSeparatedFields() {
        assertEquals(
                List.of("12345678", "0001", "95", "1", "LOJA"),
                CsvLineParser.parseFields("12345678;0001;95;1;LOJA"));
    }

    @Test
    void parsesQuotedFieldWithEmbeddedSemicolon() {
        assertEquals(
                List.of("12345678", "0001", "95", "1", "LOJA;TESTE", "02"),
                CsvLineParser.parseFields("12345678;0001;95;1;\"LOJA;TESTE\";02"));
    }

    @Test
    void parsesEscapedQuotesInsideQuotedField() {
        assertEquals(List.of("A\"B"), CsvLineParser.parseFields("\"A\"\"B\""));
    }

    @Test
    void parsesEmptyTrailingFields() {
        assertEquals(List.of("a", "b", ""), CsvLineParser.parseFields("a;b;"));
    }

    @Test
    void parsesLatin1BytesAsUtf8String() {
        byte[] latin1 = "CAF\u00c9".getBytes(StandardCharsets.ISO_8859_1);
        String line = new String(latin1, StandardCharsets.ISO_8859_1) + ";X";
        assertEquals(List.of("CAF\u00c9", "X"), CsvLineParser.parseFields(line));
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CsvLineParser.parseFields("\"abc"));
    }
}
