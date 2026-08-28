package br.univesp.tcc.cdccnpj.events.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalPipelineCliTest {

    @Test
    void parseSingleBucket() {
        assertEquals(0, CanonicalPipelineCli.parseBuckets(new String[] {"--bucket", "0"}).getFirst());
    }

    @Test
    void parseMultipleBuckets() {
        assertEquals(
                java.util.List.of(0, 1, 2),
                CanonicalPipelineCli.parseBuckets(new String[] {"--bucket", "0,1,2"}));
    }

    @Test
    void parseAllBuckets() {
        assertEquals(256, CanonicalPipelineCli.parseBuckets(new String[] {"--all-buckets"}).size());
    }
}
