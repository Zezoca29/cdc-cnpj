package br.univesp.tcc.cdccnpj.events.pipeline;

import br.univesp.tcc.cdccnpj.bench.SyntheticSnapshotGenerator;
import br.univesp.tcc.cdccnpj.diff.e3.HashDiffEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticPipelineRunnerTest {

    @Test
    void diffPrimaryBucketProducesNineChanges(@TempDir Path fixturesRoot) {
        SyntheticPipelineRunner.writeFixtures(fixturesRoot);

        var report = SyntheticPipelineRunner.runDiffOnly(fixturesRoot, "E3");

        assertEquals(9, report.diffChanges());
        assertEquals(9, report.eventsPublished());
        assertEquals(1, report.quarantine().immediatelyConfirmed());
    }

    @Test
    void usesPrimaryBucketZero() {
        assertEquals(0, SyntheticSnapshotGenerator.PRIMARY_BUCKET);
    }
}
