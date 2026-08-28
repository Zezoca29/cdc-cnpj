package br.univesp.tcc.cdccnpj.ingestion.municipio;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabmunLoaderTest {

    @Test
    void loadsSiafiToIbgeMappingFromFixture() {
        try (InputStream inputStream = getClass().getResourceAsStream("/tabmun-fixture.csv")) {
            TabmunIndex index = TabmunLoader.load(inputStream);

            assertEquals(Optional.of("3550308"), index.resolveIbge("7107"));
            assertEquals(Optional.of("3550308"), index.resolveIbge("07107"));
            assertEquals(Optional.of("5300108"), index.resolveIbge("9701"));
            assertTrue(index.resolveIbge("9999").isEmpty());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void padsSiafiAndIbgeCodes() {
        assertEquals("7107", TabmunIndex.padSiafi("7107"));
        assertEquals("0707", TabmunIndex.padSiafi("707"));
        assertEquals("3550308", TabmunIndex.normalizeIbge("3550308"));
        assertEquals("0530108", TabmunIndex.normalizeIbge("530108"));
    }
}
