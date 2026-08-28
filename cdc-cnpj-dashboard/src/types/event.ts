export type Screen = 'overview' | 'events' | 'entity' | 'bench' | 'quality';

export interface ChangelogEvent {
  eventId: string;
  schemaVersion: string;
  eventType: string;
  entityKey: Record<string, string>;
  competenciaDe: string;
  competenciaPara: string;
  camposAlterados: string[];
  before: Record<string, string>;
  after: Record<string, string>;
  detectedAt: string;
  pipelineEstrategia: string;
  pipelineRunId: string;
  consumedAt: string;
}

export interface EventPage {
  items: ChangelogEvent[];
  total: number;
  page: number;
  size: number;
}

export interface EventTypeCount {
  eventType: string;
  count: number;
}

export interface DashboardStats {
  totalEvents: number;
  competenciaDe: string | null;
  competenciaPara: string | null;
  distinctEventTypes: number;
  byEventType: EventTypeCount[];
  byStrategy?: { strategy: string; count: number }[];
}

export interface EventFilters {
  eventType: string;
  competenciaDe: string;
  competenciaPara: string;
  search: string;
  page: number;
  size: number;
}
