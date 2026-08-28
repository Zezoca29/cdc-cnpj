import type { ChangelogEvent, DashboardStats, EventPage } from '../types/event';
import type {
  BenchmarkPayload,
  ManifestSummary,
  Manifesto,
  PipelineSnapshot,
  QualityReport,
  StorageSummary,
} from '../types/artifacts';

const API_BASE = import.meta.env.VITE_API_BASE ?? '';

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error ?? `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function fetchHealth(): Promise<boolean> {
  try {
    await getJson<{ status: string }>('/api/health');
    return true;
  } catch {
    return false;
  }
}

export async function fetchStats(): Promise<DashboardStats> {
  return getJson<DashboardStats>('/api/stats');
}

export async function fetchEvents(params: {
  event_type?: string;
  competencia_de?: string;
  competencia_para?: string;
  search?: string;
  page?: number;
  size?: number;
}): Promise<EventPage> {
  const query = new URLSearchParams();
  if (params.event_type) query.set('event_type', params.event_type);
  if (params.competencia_de) query.set('competencia_de', params.competencia_de);
  if (params.competencia_para) query.set('competencia_para', params.competencia_para);
  if (params.search) query.set('search', params.search);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 50));
  return getJson<EventPage>(`/api/events?${query}`);
}

export async function fetchEventsByEntity(q: string, limit = 100): Promise<ChangelogEvent[]> {
  const query = new URLSearchParams({ q, limit: String(limit) });
  const body = await getJson<{ items: ChangelogEvent[] }>(`/api/events/by-entity?${query}`);
  return body.items;
}

export async function fetchManifests(): Promise<ManifestSummary[]> {
  const body = await getJson<{ items: ManifestSummary[] }>('/api/manifests');
  return body.items;
}

export async function fetchManifest(competencia: string): Promise<Manifesto> {
  const query = new URLSearchParams({ competencia });
  return getJson<Manifesto>(`/api/manifests?${query}`);
}

export async function fetchQuality(competencia: string): Promise<QualityReport> {
  const query = new URLSearchParams({ competencia });
  return getJson<QualityReport>(`/api/quality?${query}`);
}

export async function fetchBenchmarks(): Promise<BenchmarkPayload> {
  return getJson<BenchmarkPayload>('/api/benchmarks');
}

export async function fetchPipelineLatest(): Promise<PipelineSnapshot | null> {
  const body = await getJson<Partial<PipelineSnapshot>>('/api/pipeline/latest');
  return body.runId ? (body as PipelineSnapshot) : null;
}

export async function fetchStorage(competencia?: string): Promise<StorageSummary> {
  const query = competencia ? `?competencia=${encodeURIComponent(competencia)}` : '';
  return getJson<StorageSummary>(`/api/storage${query}`);
}
