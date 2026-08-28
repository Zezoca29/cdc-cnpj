package br.univesp.tcc.cdccnpj.cnpj;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CnpjValidatorTest {

    @Test
    void validNumericCnpj() {
        assertTrue(CnpjValidator.isValid("11222333000181"));
    }

    @ParameterizedTest
    @CsvSource({
            "11222333, 0001, 81, 11222333000181"
    })
    void composeAndValidate(String basico, String ordem, String dv, String full) {
        assertEquals(full, CnpjValidator.compose(basico, ordem, dv));
        assertTrue(CnpjValidator.isValid(full));
    }

    @Test
    void mixedFormatCheckDigitUsesAsciiMinus48() {
        int letterValue = CnpjValidator.charValue('A');
        assertEquals(17, letterValue);
        String base12 = "12ABC3450001";
        String dv = CnpjValidator.computeCheckDigits(base12);
        assertEquals(2, dv.length());
        assertTrue(CnpjValidator.isValid(base12 + dv));
    }

    @Test
    void rejectsWrongLength() {
        assertFalse(CnpjValidator.isValid("123"));
    }
}
