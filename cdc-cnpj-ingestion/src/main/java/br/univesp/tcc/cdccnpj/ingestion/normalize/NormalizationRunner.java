package br.univesp.tcc.cdccnpj.ingestion.normalize;

import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestArquivo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.ManifestIo;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Manifesto;
import br.univesp.tcc.cdccnpj.ingestion.manifest.Sha256Hasher;
import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunIndex;
import br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunLoader;
import br.univesp.tcc.cdccnpj.ingestion.parser.DominioParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.EmpresasParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.EstabelecimentosParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.ParseRequest;
import br.univesp.tcc.cdccnpj.ingestion.parser.SimplesParser;
import br.univesp.tcc.cdccnpj.ingestion.parser.SociosParser;
import br.univesp.tcc.cdccnpj.ingestion.report.ParseStageException;
import br.univesp.tcc.cdccnpj.ingestion.report.StageReport;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra normalização ZIP→Parquet para uma competência a partir do manifesto (A10/A11).
 */
public final class NormalizationRunner {

    private final EmpresasParser empresasParser = new EmpresasParser();
    private final SociosParser sociosParser = new SociosParser();
    private final SimplesParser simplesParser = new SimplesParser();
    private final DominioParser dominioParser = new DominioParser();

    public CompetenciaNormalizationReport normalize(
            Path rawDirectory, Path manifestPath, Path canonicalRoot, Path tabmunCsv) {
        Manifesto manifesto = ManifestIo.read(manifestPath);
        TabmunIndex tabmunIndex = tabmunCsv == null ? TabmunIndex.empty() : TabmunLoader.load(tabmunCsv);
        EstabelecimentosParser estabelecimentosParser = new EstabelecimentosParser(tabmunIndex);

        Map<String, StageReport> reports = new LinkedHashMap<>();
        for (ManifestArquivo arquivo : manifesto.arquivos()) {
            if (isPartitionedTable(arquivo.tabela())) {
                continue;
            }
            reports.put(arquivo.nome(), parseDominio(arquivo, rawDirectory, manifesto, canonicalRoot));
        }
        reports.putAll(parsePartitionedTable("empresas", manifesto, rawDirectory, canonicalRoot, estabelecimentosParser));
        reports.putAll(
                parsePartitionedTable("estabelecimentos", manifesto, rawDirectory, canonicalRoot, estabelecimentosParser));
        reports.putAll(parsePartitionedTable("socios", manifesto, rawDirectory, canonicalRoot, estabelecimentosParser));
        reports.putAll(parsePartitionedTable("simples", manifesto, rawDirectory, canonicalRoot, estabelecimentosParser));

        return new CompetenciaNormalizationReport(manifesto.competencia(), Map.copyOf(reports));
    }

    private Map<String, StageReport> parsePartitionedTable(
            String tabela,
            Manifesto manifesto,
            Path rawDirectory,
            Path canonicalRoot,
            EstabelecimentosParser estabelecimentosParser) {
        List<ManifestArquivo> arquivos = manifesto.arquivos().stream()
                .filter(arquivo -> tabela.equals(arquivo.tabela()))
                .toList();
        if (arquivos.isEmpty()) {
            return Map.of();
        }

        Map<String, StageReport> reports = new LinkedHashMap<>();
        try (BucketedParquetWriter writer =
                new BucketedParquetWriter(canonicalRoot, manifesto.competencia(), tabela, schemaFor(tabela))) {
            for (ManifestArquivo arquivo : arquivos) {
                Path zipPath = rawDirectory.resolve(arquivo.nome());
                Sha256Hasher.assertMatches(zipPath, arquivo);
                ParseRequest request = new ParseRequest(manifesto.competencia(), arquivo.nome(), arquivo.sha256());
                StageReport report = parsePartitionedZip(tabela, zipPath, request, writer, estabelecimentosParser);
                reports.put(arquivo.nome(), report);
            }
        }
        BucketMaterializer.materializeMissing(canonicalRoot, manifesto.competencia(), tabela, schemaFor(tabela));
        return reports;
    }

    private StageReport parsePartitionedZip(
            String tabela,
            Path zipPath,
            ParseRequest request,
            BucketedParquetWriter writer,
            EstabelecimentosParser estabelecimentosParser) {
        return switch (tabela) {
            case "empresas" -> empresasParser.parseZip(zipPath, request, writer);
            case "estabelecimentos" -> estabelecimentosParser.parseZip(zipPath, request, writer);
            case "socios" -> sociosParser.parseZip(zipPath, request, writer);
            case "simples" -> simplesParser.parseZip(zipPath, request, writer);
            default -> throw new ParseStageException("Unsupported partitioned tabela: " + tabela);
        };
    }

    private StageReport parseDominio(
            ManifestArquivo arquivo, Path rawDirectory, Manifesto manifesto, Path canonicalRoot) {
        Path zipPath = rawDirectory.resolve(arquivo.nome());
        Sha256Hasher.assertMatches(zipPath, arquivo);
        ParseRequest request = new ParseRequest(manifesto.competencia(), arquivo.nome(), arquivo.sha256());
        return dominioParser.parseZip(zipPath, request, canonicalRoot);
    }

    private static boolean isPartitionedTable(String tabela) {
        return "empresas".equals(tabela) || "estabelecimentos".equals(tabela) || "socios".equals(tabela) || "simples".equals(tabela);
    }

    private static org.apache.avro.Schema schemaFor(String tabela) {
        return switch (tabela) {
            case "empresas" -> CanonicalSchemas.empresas();
            case "estabelecimentos" -> CanonicalSchemas.estabelecimentos();
            case "socios" -> CanonicalSchemas.socios();
            case "simples" -> CanonicalSchemas.simples();
            default -> throw new ParseStageException("No schema for partitioned tabela: " + tabela);
        };
    }
}
