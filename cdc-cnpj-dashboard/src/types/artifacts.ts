export interface StrategyCount {
  strategy: string;
  count: number;
}

export interface ManifestSummary {
  competencia: string;
  geradoEm: string;
  origem: string;
  arquivoCount: number;
  hasQuality: boolean;
}

export interface ManifestArquivo {
  nome: string;
  tabela: string;
  parte: number;
  tamanho_bytes: number;
  sha256: string;
  url: string;
  baixado_em: string;
  espelho: boolean;
}

export interface Manifesto {
  schema_version: string;
  competencia: string;
  gerado_em: string;
  origem: string;
  origem_url_base: string;
  arquivos: ManifestArquivo[];
}

export interface ArquivoQuality {
  lines_read: number;
  lines_accepted: number;
  lines_rejected: number;
  rejection_rate: number;
  sentinel_dates: Record<string, number>;
  municipio_ibge_nao_resolvido: number;
}

export interface BucketQuality {
  tabela: string;
  buckets_present: number;
  complete: boolean;
  missing_buckets: number[];
}

export interface QualityReport {
  schema_version: string;
  competencia: string;
  gerado_em: string;
  rejection_threshold: number;
  total_lines_read: number;
  total_lines_rejected: number;
  rejection_rate: number;
  municipio_ibge_nao_resolvido: number;
  by_arquivo: Record<string, ArquivoQuality>;
  buckets: BucketQuality[];
}

export interface BenchmarkSummary {
  engine: string;
  tabela: string;
  bucketScale: number;
  medianWallMs: number;
  q1WallMs: number;
  q3WallMs: number;
  medianThroughput: number;
  medianPeakHeapBytes: number;
  speedupVsE1: number | null;
}

export interface BenchmarkPayload {
  sourceFile: string | null;
  summaries: BenchmarkSummary[];
  rawRowCount: number;
  fullScale?: FullScaleSummary;
}

export interface FullScaleSummary {
  runId: string;
  engine: string;
  competenciaDe: string;
  competenciaPara: string;
  bucketScale: number;
  totalWallHours: number;
  totalEvents: number;
  peakHeapBytes: number;
  peakRssBytes: number;
  bytesRead: number;
  h2Passed: boolean;
}

export interface PipelineSnapshot {
  runId: string;
  engine: string;
  competenciaDe: string;
  competenciaPara: string;
  tables: string[];
  bucketCount: number;
  diffChanges: number;
  eventsPublished: number;
  changelogInserted: number;
  inputDeletes: number;
  publishedDeletes: number;
  cancelledByResurrection: number;
  immediatelyConfirmed: number;
  stillPending: number;
  completedAt: string;
}

export interface StorageSummary {
  parquetLabel: string;
  parquetBytes: number;
  changelogLabel: string;
  changelogBytes: number;
}
