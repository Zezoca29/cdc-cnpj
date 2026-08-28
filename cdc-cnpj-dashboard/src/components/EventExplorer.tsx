import { useCallback, useEffect, useState } from 'react';
import { fetchEvents } from '../api/client';
import type { ChangelogEvent, DashboardStats } from '../types/event';
import { formatNumber } from '../utils/format';
import { EventCard } from './EventCard';

interface EventExplorerProps {
  stats: DashboardStats | null;
}

export function EventExplorer({ stats }: EventExplorerProps) {
  const [eventType, setEventType] = useState('');
  const [competenciaDe, setCompetenciaDe] = useState(stats?.competenciaDe ?? '');
  const [competenciaPara, setCompetenciaPara] = useState(stats?.competenciaPara ?? '');
  const [page, setPage] = useState(0);
  const [events, setEvents] = useState<ChangelogEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openIds, setOpenIds] = useState<Record<string, boolean>>({});

  const typeOptions = [
    { value: '', label: 'todos os tipos' },
    ...(stats?.byEventType ?? []).map((t) => ({
      value: t.eventType,
      label: `${t.eventType} (${formatNumber(t.count)})`,
    })),
  ];

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchEvents({
        event_type: eventType || undefined,
        competencia_de: competenciaDe || undefined,
        competencia_para: competenciaPara || undefined,
        page,
        size: 50,
      });
      setEvents(result.items);
      setTotal(result.total);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Falha ao consultar changelog');
      setEvents([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [eventType, competenciaDe, competenciaPara, page]);

  useEffect(() => {
    if (stats?.competenciaDe) setCompetenciaDe(stats.competenciaDe);
    if (stats?.competenciaPara) setCompetenciaPara(stats.competenciaPara);
  }, [stats?.competenciaDe, stats?.competenciaPara]);

  useEffect(() => {
    void load();
  }, [load]);

  function clearFilters() {
    setEventType('');
    setCompetenciaDe(stats?.competenciaDe ?? '');
    setCompetenciaPara(stats?.competenciaPara ?? '');
    setPage(0);
  }

  const totalPages = Math.max(1, Math.ceil(total / 50));

  return (
    <div style={{ padding: '26px 40px 0' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 14,
          border: '1px solid #c7d7f0',
          background: 'linear-gradient(90deg, rgba(27,95,193,0.06), rgba(27,95,193,0.01))',
          borderRadius: 10,
          padding: '14px 18px',
        }}
      >
        <div
          className="mono"
          style={{
            width: 26,
            height: 26,
            flex: '0 0 26px',
            borderRadius: 6,
            border: '1px solid rgba(27,95,193,0.32)',
            display: 'grid',
            placeItems: 'center',
            fontSize: 13,
            color: '#1b5fc1',
          }}
        >
          Δ
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13.5, fontWeight: 600 }}>Changelog — apenas mudanças detectadas entre snapshots</div>
          <div style={{ marginTop: 2, fontSize: 12, color: '#586a7d' }}>
            Mostra o que mudou entre competências, não o universo cadastral. O snapshot completo vive no Parquet canônico.
          </div>
        </div>
        <div className="mono" style={{ display: 'flex', gap: 18, fontSize: 11, textAlign: 'right' }}>
          <div>
            <div style={{ color: '#5b6d80' }}>changelog</div>
            <div style={{ color: '#0d7a4f', fontSize: 13 }}>{formatNumber(stats?.totalEvents ?? total)} ev.</div>
          </div>
        </div>
      </div>

      <div style={{ marginTop: 22, display: 'flex', alignItems: 'flex-end', gap: 12, flexWrap: 'wrap' }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span className="field-label">event_type</span>
          <select className="select" style={{ minWidth: 268 }} value={eventType} onChange={(e) => { setEventType(e.target.value); setPage(0); }}>
            {typeOptions.map((o) => (
              <option key={o.value || 'all'} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span className="field-label">competência de</span>
          <input className="input" style={{ width: 120 }} value={competenciaDe} onChange={(e) => { setCompetenciaDe(e.target.value); setPage(0); }} placeholder="2026-06" />
        </label>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <span className="field-label">competência para</span>
          <input className="input" style={{ width: 120 }} value={competenciaPara} onChange={(e) => { setCompetenciaPara(e.target.value); setPage(0); }} placeholder="2026-07" />
        </label>
        <button type="button" className="btn" onClick={clearFilters}>
          limpar
        </button>
      </div>

      <div
        style={{
          marginTop: 18,
          display: 'flex',
          alignItems: 'baseline',
          gap: 10,
          paddingBottom: 12,
          borderBottom: '1px solid #e3e8ee',
        }}
      >
        <span className="mono" style={{ fontSize: 13, color: '#0f2138' }}>
          {formatNumber(total)}
        </span>
        <span style={{ fontSize: 12.5, color: '#566677' }}>
          eventos em <span className="mono">changelog.events</span> para este filtro
        </span>
      </div>

      {loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
          {[1, 2, 3].map((i) => (
            <div key={i} className="card" style={{ padding: 22 }}>
              <div className="skeleton" style={{ width: 220 }} />
              <div className="skeleton" style={{ marginTop: 16, width: 200, height: 22 }} />
            </div>
          ))}
          <div className="mono" style={{ textAlign: 'center', fontSize: 11.5, color: '#5b6d80', padding: 6 }}>
            consultando changelog.events…
          </div>
        </div>
      )}

      {!loading && error && (
        <div
          style={{
            marginTop: 16,
            border: '1px solid #efcdc9',
            background: 'rgba(179,55,44,0.045)',
            borderRadius: 11,
            padding: '24px 26px',
            display: 'flex',
            gap: 18,
          }}
        >
          <div
            className="mono"
            style={{
              width: 28,
              height: 28,
              flex: '0 0 28px',
              borderRadius: 6,
              border: '1px solid rgba(179,55,44,0.30)',
              display: 'grid',
              placeItems: 'center',
              color: '#b3372c',
            }}
          >
            !
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#b3372c' }}>Falha ao consultar o changelog</div>
            <div style={{ marginTop: 6, fontSize: 12.5, color: '#586a7d' }}>{error}</div>
            <button type="button" className="btn" style={{ marginTop: 14, borderColor: '#efcdc9', color: '#b3372c' }} onClick={() => void load()}>
              tentar novamente
            </button>
          </div>
        </div>
      )}

      {!loading && !error && events.length === 0 && (
        <div style={{ marginTop: 16, border: '1px dashed #cdd6e0', borderRadius: 11, padding: '60px 40px', textAlign: 'center' }}>
          <div className="mono" style={{ fontSize: 26, color: '#cdd6e0' }}>
            Δ = 0
          </div>
          <div style={{ marginTop: 14, fontSize: 15, fontWeight: 600 }}>Nenhum evento neste filtro</div>
          <div style={{ marginTop: 7, fontSize: 12.5, color: '#566677', maxWidth: 460, margin: '7px auto 0' }}>
            O changelog só contém deltas detectados entre as competências selecionadas.
          </div>
          <button type="button" className="btn-primary btn" style={{ marginTop: 20 }} onClick={clearFilters}>
            limpar filtros
          </button>
        </div>
      )}

      {!loading && !error && events.length > 0 && (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
            {events.map((ev) => (
              <EventCard
                key={ev.eventId}
                event={ev}
                open={!!openIds[ev.eventId]}
                onToggle={() => setOpenIds((prev) => ({ ...prev, [ev.eventId]: !prev[ev.eventId] }))}
              />
            ))}
          </div>
          <div
            style={{
              marginTop: 24,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 16,
              paddingTop: 18,
              borderTop: '1px solid #e3e8ee',
            }}
          >
            <div className="mono" style={{ fontSize: 11.5, color: '#5b6d80' }}>
              página {page + 1} de {formatNumber(totalPages)} · 50 eventos / página · ordenado por consumed_at desc
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <button type="button" className="btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                anterior
              </button>
              <button type="button" className="btn-primary btn" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                próxima
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
