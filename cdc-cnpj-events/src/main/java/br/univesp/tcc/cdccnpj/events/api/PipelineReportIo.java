package br.univesp.tcc.cdccnpj.events.api;

import br.univesp.tcc.cdccnpj.events.kafka.EventJsonMapper;
import br.univesp.tcc.cdccnpj.events.pipeline.EventPipelineRunner;
import br.univesp.tcc.cdccnpj.events.quarantine.QuarantineResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class PipelineReportIo {

    public record PipelineReportSnapshot(
            String runId,
            String engine,
            String competenciaDe,
            String competenciaPara,
            List<String> tables,
            int bucketCount,
            int diffChanges,
            int eventsPublished,
            int changelogInserted,
            int inputDeletes,
            int publishedDeletes,
            long cancelledByResurrection,
            long immediatelyConfirmed,
            int stillPending,
            String completedAt) {

        public static PipelineReportSnapshot from(
                String runId,
                String engine,
                String competenciaDe,
                String competenciaPara,
                List<String> tables,
                int bucketCount,
                EventPipelineRunner.PipelineReport report) {
            QuarantineResult quarantine = report.quarantine();
            return new PipelineReportSnapshot(
                    runId,
                    engine,
                    competenciaDe,
                    competenciaPara,
                    List.copyOf(tables),
                    bucketCount,
                    report.diffChanges(),
                    report.eventsPublished(),
                    report.changelogInserted(),
                    quarantine.inputDeletes(),
                    quarantine.publishedDeletes(),
                    quarantine.cancelledByResurrection(),
                    quarantine.immediatelyConfirmed(),
                    quarantine.stillPending(),
                    Instant.now().toString());
        }
    }

    private static final ObjectMapper MAPPER =
            EventJsonMapper.create().enable(SerializationFeature.INDENT_OUTPUT);

    private PipelineReportIo() {}

    public static Path defaultPath(Path projectRoot) {
        return projectRoot.resolve("data/measurements/pipeline-latest.json");
    }

    public static void write(Path path, PipelineReportSnapshot snapshot) {
        try {
            Files.createDirectories(path.getParent());
            MAPPER.writeValue(path.toFile(), snapshot);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing pipeline report " + path, e);
        }
    }

    public static PipelineReportSnapshot read(Path path) {
        try {
            return MAPPER.readValue(Files.readString(path), PipelineReportSnapshot.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading pipeline report " + path, e);
        }
    }
}
