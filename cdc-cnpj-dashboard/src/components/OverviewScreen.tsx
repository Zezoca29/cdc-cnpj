import { useEffect, useState } from 'react';
import { fetchPipelineLatest, fetchStorage } from '../api/client';
import type { PipelineSnapshot, StorageSummary } from '../types/artifacts';
import type { DashboardStats } from '../types/event';
import { formatNumber } from '../utils/format';

interface OverviewScreenProps {
  stats: DashboardStats | null;
  apiOnline: boolean;
}

export function OverviewScreen({ stats, apiOnline }: OverviewScreenProps) {
  const [pipeline, setPipeline] = useState<PipelineSnapshot | null>(null);
  const [storage, setStorage] = useState<StorageSummary | null>(null);

  useEffect(() => {
    if (!apiOnline) return;
    void (async () => {
      try {
        const [p, s] = await Promise.all([
          fetchPipelineLatest(),
          fetchStorage(stats?.competenciaPara ?? undefined),
        ]);
        setPipeline(p);
        setStorage(s);
      } catch {
        setPipeline(null);
        setStorage(null);
      }
    })();
  }, [apiOnline, stats?.competenciaPara]);

  const total = stats?.totalEvents ?? 0;
  const types = stats?.distinctEventTypes ?? 0;
  const e1Count = stats?.byStrategy?.find((s) => s.strategy === 'E1')?.count ?? 0;
  const e3Count = stats?.byStrategy?.find((s) => s.strategy === 'E3')?.count ?? 0;
  const strategyMismatch = e1Count > 0 && e3Count > 0 && e1Count !== e3Count;

  const kpis = [
    { label: 'eventos no changelog', value: formatNumber(total), color: '#0d7a4f', note: 'deltas em changelog.events' },
    { label: 'tipos distintos', value: String(types), color: '#1b5fc1', note: 'event_type únicos' },
    {
      label: 'último pipeline',
      value: pipeline ? formatNumber(pipeline.diffChanges) : '—',
      color: '#a86a11',
      note: pipeline ? `${pipeline.engine} · ${pipeline.bucketCount} buckets` : 'sem pipeline-latest.json',
    },
    {
      label: 'changelog inserido',
      value: pipeline ? formatNumber(pipeline.changelogInserted) : '—',
      color: '#0f2138',
      note: pipeline?.runId ?? '—',
    },
  ];

  const scaleBars = storage
    ? [
        {
          label: 'Parquet canônico (competência)',
          width: '100%',
          fill: 'linear-gradient(90deg, #cdd6e0, #e7ecf1)',
          value: storage.parquetLabel,
          textColor: '#0f2138',
        },
        {
          label: 'changelog.events (Postgres)',
          width:
            storage.parquetBytes > 0
              ? `${Math.max(2, (storage.changelogBytes / storage.parquetBytes) * 100)}%`
              : '4%',
          fill: 'linear-gradient(90deg, #1b5fc1, rgba(27,95,193,0.5))',
          value: storage.changelogLabel,
          textColor: '#0d7a4f',
        },
      ]
    : [];

  const stages = [
    {
      n: '1',
      name: 'Ingestão RF',
      store: 'ZIP + manifesto SHA-256',
      volume: 'raw/',
      status: 'manifesto',
      dot: '#0d7a4f',
      border: '#c7e8d8',
      bg: 'rgba(13,122,79,0.04)',
      volSize: 12,
      volColor: '#3d5065',
      arrow: true,
    },
    {
      n: '2',
      name: 'Parquet canônico',
      store: '256 buckets · snapshot completo',
      volume: storage?.parquetLabel ?? 'Parquet',
      status: 'imutável',
      dot: '#1b5fc1',
      border: '#c7d7f0',
      bg: 'rgba(27,95,193,0.04)',
      volSize: 12,
      volColor: '#1b5fc1',
      arrow: true,
    },
    {
      n: '3',
      name: 'Diff E1 / E3',
      store: 'motores sobre buckets',
      volume: pipeline ? formatNumber(pipeline.diffChanges) : formatNumber(total),
      status: pipeline?.engine ?? '—',
      dot: '#a86a11',
      border: '#ecd9b3',
      bg: 'rgba(168,106,17,0.04)',
      volSize: 14,
      volColor: '#a86a11',
      arrow: true,
    },
    {
      n: '4',
      name: 'Kafka → Postgres',
      store: 'changelog.events',
      volume: formatNumber(total),
      status: apiOnline ? 'conectado' : 'offline',
      dot: apiOnline ? '#0d7a4f' : '#b3372c',
      border: apiOnline ? '#c7e8d8' : '#efcdc9',
      bg: apiOnline ? 'rgba(13,122,79,0.04)' : 'rgba(179,55,44,0.04)',
      volSize: 14,
      volColor: '#0d7a4f',
      arrow: false,
    },
  ];

  return (
    <div style={{ padding: '26px 40px 0' }}>
      {!apiOnline && (
        <div
          style={{
            display: 'flex',
            gap: 14,
            alignItems: 'center',
            border: '1px solid #ecd9b3',
            background: 'rgba(168,106,17,0.05)',
            borderRadius: 10,
            padding: '13px 18px',
            marginBottom: 22,
          }}
        >
          <div className="mono" style={{ color: '#a86a11' }}>
            !
          </div>
          <div style={{ flex: 1, fontSize: 12.5, color: '#586a7d' }}>
            API offline — inicie o servidor com{' '}
            <span className="mono">mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.api.ChangelogApiCli</span>
          </div>
        </div>
      )}

      {strategyMismatch && (
        <div
          style={{
            display: 'flex',
            gap: 14,
            alignItems: 'center',
            border: '1px solid #ecd9b3',
            background: 'rgba(168,106,17,0.05)',
            borderRadius: 10,
            padding: '13px 18px',
            marginBottom: 22,
          }}
        >
          <div className="mono" style={{ color: '#a86a11' }}>
            ≠
          </div>
          <div style={{ flex: 1, fontSize: 12.5, color: '#586a7d' }}>
            Changelog mistura estratégias: E1={formatNumber(e1Count)} · E3={formatNumber(e3Count)} — runs diferentes no
            mesmo changelog.
          </div>
        </div>
      )}

      <div className="card" style={{ padding: '22px 24px' }}>
        <div style={{ fontSize: 14, fontWeight: 600 }}>
          Pipeline · {stats?.competenciaDe ?? '—'} → {stats?.competenciaPara ?? '—'}
        </div>

        <div style={{ marginTop: 20, display: 'flex', alignItems: 'stretch', gap: 0, flexWrap: 'wrap', rowGap: 12 }}>
          {stages.map((st) => (
            <div key={st.n} style={{ display: 'flex', alignItems: 'stretch', flex: '1 1 190px', minWidth: 0 }}>
              <div
                style={{
                  flex: 1,
                  minWidth: 0,
                  border: `1px solid ${st.border}`,
                  background: st.bg,
                  borderRadius: 10,
                  padding: 14,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 9,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: st.dot }} />
                  <span className="mono" style={{ fontSize: 9.5, letterSpacing: '0.08em', color: '#5b6d80', textTransform: 'uppercase' }}>
                    estágio {st.n}
                  </span>
                </div>
                <div style={{ fontSize: 13, fontWeight: 600, color: '#0f2138' }}>{st.name}</div>
                <div className="mono" style={{ fontSize: 10.5, color: '#566677', overflowWrap: 'break-word' }}>
                  {st.store}
                </div>
                <div style={{ marginTop: 'auto', paddingTop: 6 }}>
                  <div className="mono" style={{ fontSize: st.volSize, fontWeight: 500, color: st.volColor }}>
                    {st.volume}
                  </div>
                  <div
                    className="mono"
                    style={{
                      marginTop: 5,
                      display: 'inline-flex',
                      fontSize: 10,
                      color: st.dot,
                      border: `1px solid ${st.border}`,
                      borderRadius: 20,
                      padding: '2px 8px',
                    }}
                  >
                    {st.status}
                  </div>
                </div>
              </div>
              {st.arrow && (
                <div style={{ flex: '0 0 26px', display: 'grid', placeItems: 'center', color: '#7a8c9f', fontSize: 14 }}>
                  →
                </div>
              )}
            </div>
          ))}
        </div>

        {scaleBars.length > 0 && (
          <div style={{ marginTop: 22, paddingTop: 20, borderTop: '1px solid #e3e8ee' }}>
            <div className="mono field-label">contraste de escala — armazenamento</div>
            <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {scaleBars.map((b) => (
                <div key={b.label} style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <div style={{ flex: '0 0 210px', fontSize: 12.5, color: '#3d5065' }}>{b.label}</div>
                  <div style={{ flex: 1, height: 22, background: '#f2f5f8', borderRadius: 4, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: b.width, background: b.fill, borderRadius: 4 }} />
                  </div>
                  <div className="mono" style={{ flex: '0 0 120px', textAlign: 'right', fontSize: 12.5, color: b.textColor }}>
                    {b.value}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div style={{ marginTop: 20, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
        {kpis.map((k) => (
          <div key={k.label} className="card" style={{ padding: 18 }}>
            <div className="mono field-label">{k.label}</div>
            <div className="mono" style={{ marginTop: 12, fontSize: 26, fontWeight: 500, color: k.color, letterSpacing: '-0.02em' }}>
              {k.value}
            </div>
            <div style={{ marginTop: 7, fontSize: 11.5, color: '#566677' }}>{k.note}</div>
          </div>
        ))}
      </div>

      {stats && stats.byEventType.length > 0 && (
        <div className="card" style={{ marginTop: 20, padding: '22px 24px' }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Distribuição por event_type</div>
          <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
            {stats.byEventType.map((t) => {
              const pct = total > 0 ? (t.count / total) * 100 : 0;
              return (
                <div key={t.eventType}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
                    <span className="mono" style={{ color: '#3d5065' }}>
                      {t.eventType}
                    </span>
                    <span className="mono">{formatNumber(t.count)}</span>
                  </div>
                  <div style={{ marginTop: 6, height: 6, background: '#eaeff4', borderRadius: 3, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${pct}%`, background: '#1b5fc1', borderRadius: 3 }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
