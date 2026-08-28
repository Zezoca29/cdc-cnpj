package br.univesp.tcc.cdccnpj.events.api;

import br.univesp.tcc.cdccnpj.bench.harness.BenchmarkAnalyzer;
import br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCsvReader;
import br.univesp.tcc.cdccnpj.bench.harness.MeasurementResult;
import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Manifesto;
import br.univesp.tcc.cdccnpj.ingestion.quality.QualityReport;
import br.univesp.tcc.cdccnpj.ingestion.quality.QualityReportIo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public final class DashboardArtifactsRepository {

    private final Path dataRoot;

    public DashboardArtifactsRepository(Path projectRoot) {
        this.dataRoot = projectRoot.resolve("data");
    }

    public List<ManifestSummary> listManifests() throws IOException {
        Path manifestsDir = dataRoot.resolve("manifests");
        if (!Files.isDirectory(manifestsDir)) {
            return List.of();
        }
        List<ManifestSummary> summaries = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(manifestsDir)) {
            for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
                String competencia = parseCompetencia(dir.getFileName().toString());
                if (competencia == null) {
                    continue;
                }
                Path manifestPath = dir.resolve("manifesto.json");
                if (!Files.isRegularFile(manifestPath)) {
                    continue;
                }
                Manifesto manifest = ManifestIo.read(manifestPath);
                summaries.add(new ManifestSummary(
                        competencia,
                        manifest.geradoEm(),
                        manifest.origem(),
                        manifest.arquivos().size(),
                        Files.exists(dir.resolve("quality.json"))));
            }
        }
        return List.copyOf(summaries);
    }

    public Manifesto readManifest(String competencia) {
        return ManifestIo.read(manifestPath(competencia));
    }

    public QualityReport readQuality(String competencia) {
        return QualityReportIo.read(qualityPath(competencia));
    }

    public Optional<QualityReport> findQuality(String competencia) {
        Path path = qualityPath(competencia);
        return Files.isRegularFile(path) ? Optional.of(QualityReportIo.read(path)) : Optional.empty();
    }

    public Optional<PipelineReportIo.PipelineReportSnapshot> readPipelineLatest() {
        Path path = dataRoot.resolve("measurements/pipeline-latest.json");
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(PipelineReportIo.read(path));
    }

    public BenchmarkPayload readBenchmarks() throws IOException {
        Path measurements = dataRoot.resolve("measurements");
        Path csv = firstExisting(
                measurements.resolve("benchmark-definitive.csv"),
                measurements.resolve("benchmark.csv"));
        if (csv == null) {
            return BenchmarkPayload.empty();
        }
        List<MeasurementResult> raw = BenchmarkCsvReader.read(csv);
        List<BenchmarkAnalyzer.BenchmarkSummary> summaries = new BenchmarkAnalyzer().summarize(raw);
        return new BenchmarkPayload(csv.getFileName().toString(), summaries, raw.size());
    }

    public Optional<FullScaleSummary> readFullScale() throws IOException {
        Path csv = dataRoot.resolve("measurements/full-scale-256.csv");
        if (!Files.isRegularFile(csv)) {
            return Optional.empty();
        }
        List<String> lines = Files.readAllLines(csv);
        if (lines.size() < 2) {
            return Optional.empty();
        }
        String[] parts = lines.get(1).split(",", -1);
        return Optional.of(new FullScaleSummary(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                Integer.parseInt(parts[4]),
                Double.parseDouble(parts[5]),
                Long.parseLong(parts[6]),
                Long.parseLong(parts[7]),
                Long.parseLong(parts[8]),
                Long.parseLong(parts[9]),
                Boolean.parseBoolean(parts[10])));
    }

    public StorageSummary readStorageSummary(String competencia, long changelogTableBytes) throws IOException {
        long parquetBytes = directorySize(dataRoot.resolve("canonical/competencia=" + competencia));
        return new StorageSummary(
                formatBytes(parquetBytes),
                parquetBytes,
                formatBytes(changelogTableBytes),
                changelogTableBytes);
    }

    private long directorySize(Path root) throws IOException {
        if (!Files.exists(root)) {
            return 0;
        }
        final long[] total = {0};
        Files.walkFileTree(
                root,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        total[0] += attrs.size();
                        return FileVisitResult.CONTINUE;
                    }
                });
        return total[0];
    }

    private Path manifestPath(String competencia) {
        return dataRoot.resolve("manifests/competencia=" + competencia + "/manifesto.json");
    }

    private Path qualityPath(String competencia) {
        return dataRoot.resolve("manifests/competencia=" + competencia + "/quality.json");
    }

    private static String parseCompetencia(String dirName) {
        if (!dirName.startsWith("competencia=")) {
            return null;
        }
        return dirName.substring("competencia=".length());
    }

    private static Path firstExisting(Path... paths) {
        return Stream.of(paths).filter(Files::isRegularFile).findFirst().orElse(null);
    }

    static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "—";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        double value = bytes;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    public record ManifestSummary(
            String competencia, String geradoEm, String origem, int arquivoCount, boolean hasQuality) {}

    public record BenchmarkPayload(
            String sourceFile, List<BenchmarkAnalyzer.BenchmarkSummary> summaries, int rawRowCount) {
        static BenchmarkPayload empty() {
            return new BenchmarkPayload(null, List.of(), 0);
        }
    }

    public record FullScaleSummary(
            String runId,
            String engine,
            String competenciaDe,
            String competenciaPara,
            int bucketScale,
            double totalWallHours,
            long totalEvents,
            long peakHeapBytes,
            long peakRssBytes,
            long bytesRead,
            boolean h2Passed) {}

    public record StorageSummary(String parquetLabel, long parquetBytes, String changelogLabel, long changelogBytes) {}
}
