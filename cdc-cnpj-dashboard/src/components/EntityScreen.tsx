import { useState } from 'react';
import { fetchEventsByEntity } from '../api/client';
import type { ChangelogEvent } from '../types/event';
import { eventBadge } from '../utils/eventMeta';
import { bucketLabel } from '../utils/bucket';
import { formatTimestamp, normalizeCnpjInput } from '../utils/format';

export function EntityScreen() {
  const [cnpj, setCnpj] = useState('');
  const [events, setEvents] = useState<ChangelogEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searched, setSearched] = useState(false);

  const cnpjBasico = cnpj.length >= 8 ? cnpj.slice(0, 8) : cnpj;
  const bucket = bucketLabel(cnpjBasico || undefined);

  async function search() {
    if (!cnpj.trim()) return;
    setLoading(true);
    setError(null);
    setSearched(true);
    try {
      const items = await fetchEventsByEntity(cnpj);
      setEvents(items);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Falha na busca');
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }

  const inferred = buildInferredState(events);

  return (
    <div style={{ padding: '26px 40px 0', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(430px, 1fr))', gap: 22, alignItems: 'start' }}>
      <div>
        <div className="card" style={{ padding: '20px 22px' }}>
          <div className="field-label">buscar por CNPJ</div>
          <div style={{ marginTop: 12, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <input
              className="input"
              style={{ flex: '1 1 260px', minWidth: 0, fontSize: 17, letterSpacing: '0.12em', padding: '12px 14px' }}
              value={cnpj}
              onChange={(e) => setCnpj(normalizeCnpjInput(e.target.value))}
              maxLength={14}
              placeholder="14 caracteres · alfanumérico nas 8 primeiras"
              onKeyDown={(e) => e.key === 'Enter' && void search()}
            />
            <button type="button" className="btn-primary btn" style={{ padding: '0 22px', fontSize: 13, fontWeight: 500 }} onClick={() => void search()}>
              buscar eventos
            </button>
          </div>
          <div className="mono" style={{ marginTop: 10, display: 'flex', gap: 16, fontSize: 10.5, color: '#5b6d80' }}>
            <span>{cnpj.length}/14 chars</span>
            <span>bucket calculado {bucket}</span>
            <span>sem máscara — CNPJ é string</span>
          </div>
        </div>

        <div className="card" style={{ marginTop: 20, padding: '20px 22px' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>Timeline de eventos no changelog</div>
            <div className="mono" style={{ fontSize: 11, color: '#5b6d80' }}>
              {events.length} eventos
            </div>
          </div>

          {loading && <div style={{ marginTop: 18, color: '#5b6d80' }}>consultando…</div>}
          {error && <div style={{ marginTop: 18, color: '#b3372c' }}>{error}</div>}
          {searched && !loading && !error && events.length === 0 && (
            <div style={{ marginTop: 18, color: '#566677' }}>Nenhum evento para este CNPJ no changelog.</div>
          )}

          <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column' }}>
            {events.map((t) => {
              const badge = eventBadge(t.eventType);
              const field = t.camposAlterados[0];
              const before = field ? t.before[field] : '—';
              const after = field ? t.after[field] : '—';
              return (
                <div key={t.eventId} style={{ display: 'flex', gap: 16 }}>
                  <div className="mono" style={{ flex: '0 0 92px', textAlign: 'right', fontSize: 11.5, color: '#566677', paddingTop: 1 }}>
                    {t.competenciaPara}
                  </div>
                  <div style={{ flex: '0 0 12px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', background: badge.fg, marginTop: 5 }} />
                    <span style={{ flex: 1, width: 1, background: '#e3e8ee' }} />
                  </div>
                  <div style={{ flex: 1, paddingBottom: 22, minWidth: 0 }}>
                    <span
                      className="mono"
                      style={{
                        display: 'inline-block',
                        fontSize: 11.5,
                        padding: '3px 8px',
                        borderRadius: 5,
                        background: badge.bg,
                        color: badge.fg,
                        border: `1px solid ${badge.bd}`,
                      }}
                    >
                      {t.eventType}
                    </span>
                    <div className="mono" style={{ marginTop: 9, fontSize: 12, color: '#3d5065', display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                      <span style={{ color: '#b3372c' }}>− {before}</span>
                      <span style={{ color: '#0d7a4f' }}>+ {after}</span>
                    </div>
                    <div className="mono" style={{ marginTop: 6, fontSize: 10.5, color: '#6b7c90' }}>
                      {formatTimestamp(t.consumedAt)} · {t.eventId}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div style={{ border: '1px solid #ecd9b3', background: 'rgba(168,106,17,0.045)', borderRadius: 11, padding: '18px 20px' }}>
          <div className="mono field-label" style={{ color: '#a86a11' }}>
            estado inferido
          </div>
          <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 11 }}>
            {inferred.length === 0 ? (
              <div style={{ fontSize: 12, color: '#586a7d' }}>Busque um CNPJ para derivar estado a partir dos eventos.</div>
            ) : (
              inferred.map((f) => (
                <div key={f.k}>
                  <div className="mono" style={{ fontSize: 10.5, color: '#5b6d80' }}>
                    {f.k}
                  </div>
                  <div className="mono" style={{ marginTop: 2, fontSize: 13, color: '#0f2138' }}>
                    {f.v}
                  </div>
                </div>
              ))
            )}
          </div>
          <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px solid #ecd9b3', fontSize: 11.5, color: '#8a5c0d', lineHeight: 1.5 }}>
            Estado parcial — derivado dos eventos do changelog. O snapshot completo está no Parquet canônico.
          </div>
        </div>

        <div className="card" style={{ padding: '18px 20px' }}>
          <div className="field-label">localização no Parquet</div>
          <div className="mono" style={{ marginTop: 13, fontSize: 11.5, color: '#3d5065', lineHeight: 1.7, wordBreak: 'break-all' }}>
            /parquet/estabelecimento/
            <br />
            <span style={{ color: '#0d7a4f' }}>competencia={events[0]?.competenciaPara ?? '—'}</span>/
            <br />
            <span style={{ color: '#0d7a4f' }}>bucket={bucket}</span>/part-*.parquet
          </div>
        </div>
      </div>
    </div>
  );
}

function buildInferredState(events: ChangelogEvent[]): { k: string; v: string }[] {
  if (events.length === 0) return [];
  const state: Record<string, string> = {};
  const chronological = [...events].reverse();
  for (const ev of chronological) {
    for (const [k, v] of Object.entries(ev.after)) {
      state[k] = v;
    }
  }
  return Object.entries(state).map(([k, v]) => ({ k, v }));
}
