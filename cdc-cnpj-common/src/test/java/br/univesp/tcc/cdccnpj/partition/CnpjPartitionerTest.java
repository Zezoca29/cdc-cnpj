package br.univesp.tcc.cdccnpj.partition;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CnpjPartitionerTest {

    @ParameterizedTest
    @CsvSource({
            "00000000, 11",
            "12345678, 206",
            "12ABC345, 222",
            "87654321, 184",
            "00000001, 220",
            "99999999, 145",
            "ABCDEFGH, 82",
            "0000000A, 64"
    })
    void bucketMatchesFrozenVectors(String cnpjBasico, int expected) {
        assertEquals(expected, CnpjPartitioner.bucket(cnpjBasico));
        assertEquals(String.format("%03d", expected), CnpjPartitioner.bucketPadded(cnpjBasico));
    }

    @org.junit.jupiter.api.Test
    void rejectsInvalidBasico() {
        assertThrows(IllegalArgumentException.class, () -> CnpjPartitioner.bucket("123"));
    }
}
