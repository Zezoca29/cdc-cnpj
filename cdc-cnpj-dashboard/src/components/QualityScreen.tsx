import { useEffect, useMemo, useState } from 'react';
import {
  fetchManifest,
  fetchManifests,
  fetchPipelineLatest,
  fetchQuality,
  fetchStorage,
} from '../api/client';
import type { ManifestSummary, PipelineSnapshot, QualityReport, StorageSummary } from '../types/artifacts';
import type { DashboardStats } from '../types/event';
import { formatNumber } from '../utils/format';

interface QualityScreenProps {
  stats: DashboardStats | null;
}

const THRESHOLD_PCT = 0.1;

export function QualityScreen({ stats }: QualityScreenProps) {
  const [manifests, setManifests] = useState<ManifestSummary[]>([]);
  const [competencia, setCompetencia] = useState('');
  const [quality, setQuality] = useState<QualityReport | null>(null);
  const [pipeline, setPipeline] = useState<PipelineSnapshot | null>(null);
  const [storage, setStorage] = useState<StorageSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [manifestHashes, setManifestHashes] = useState<{ name: string; hash: string; status: string }[]>([]);

  useEffect(() => {
    void (async () => {
      try {
        const items = await fetchManifests();
        setManifests(items);
        const defaultComp =
          stats?.competenciaPara ?? items[items.length - 1]?.competencia ?? '2026-07';
        setCompetencia(defaultComp);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Falha ao listar manifestos');
      }
    })();
  }, [stats?.competenciaPara]);

  useEffect(() => {
    if (!competencia) return;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const [q, p, s] = await Promise.all([
          fetchQuality(competencia).catch(() => null),
          fetchPipelineLatest(),
          fetchStorage(competencia).catch(() => null),
        ]);
        setQuality(q);
        setPipeline(p);
        setStorage(s);
        try {
          const manifest = await fetchManifest(competencia);
          setManifestHashes(
            manifest.arquivos.map((a) => ({
              name: a.nome,
              hash: a.sha256,
              status: 'ok',
            })),
          );
        } catch {
          setManifestHashes([]);
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Falha ao carregar qualidade');
      } finally {
        setLoading(false);
      }
    })();
  }, [competencia]);

  const rejects = useMemo(() => {
    if (!quality) return [];
    return Object.entries(quality.by_arquivo).map(([file, r]) => {
      const pct = r.rejection_rate * 100;
      const over = pct > THRESHOLD_PCT;
      return {
        file,
        pct: `${pct.toFixed(4)}%`,
        width: `${Math.min(100, (pct / THRESHOLD_PCT) * 100)}%`,
        color: over ? '#b3372c' : pct > 0 ? '#a86a11' : '#0d7a4f',
        detail: `${formatNumber(r.lines_rejected)} / ${formatNumber(r.lines_read)} linhas`,
      };
    });
  }, [quality]);

  const quarantine = useMemo(() => {
    if (!pipeline) return [];
    return [
      {
        title: 'deletes em quarentena',
        count: String(pipeline.inputDeletes - pipeline.publishedDeletes),
        note: 'aguardando confirmação na competência seguinte',
        color: '#a86a11',
        border: '#ecd9b3',
        bg: 'rgba(168,106,17,0.045)',
      },
      {
        title: 'cancelados por ressurreição',
        count: String(pipeline.cancelledByResurrection),
        note: 'chave reapareceu antes do delete publicado',
        color: '#1b5fc1',
        border: '#c7d7f0',
        bg: 'rgba(27,95,193,0.04)',
      },
      {
        title: 'confirmados imediatamente',
        count: String(pipeline.immediatelyConfirmed),
        note: 'delete publicado sem esperar competência extra',
        color: '#0d7a4f',
        border: '#c7e8d8',
        bg: 'rgba(13,122,79,0.04)',
      },
    ];
  }, [pipeline]);

  const parquetPct =
    storage && storage.parquetBytes + storage.changelogBytes > 0
      ? (storage.parquetBytes / (storage.parquetBytes + storage.changelogBytes)) * 100
      : 100;
  const changelogPct = 100 - parquetPct;

  return (
    <div style={{ padding: '26px 40px 0' }}>
      <div style={{ marginBottom: 18, display: 'flex', alignItems: 'center', gap: 12 }}>
        <span className="field-label">competência</span>
        <select className="select" value={competencia} onChange={(e) => setCompetencia(e.target.value)}>
          {manifests.map((m) => (
            <option key={m.competencia} value={m.competencia}>
              {m.competencia} ({m.origem})
            </option>
          ))}
          {manifests.length === 0 && <option value={competencia}>{competencia || '—'}</option>}
        </select>
      </div>

      {loading && <div style={{ color: '#5b6d80' }}>carregando qualidade…</div>}
      {error && <div style={{ color: '#b3372c' }}>{error}</div>}

      {!loading && !quality && !error && (
        <div className="card" style={{ padding: 24 }}>
          <div style={{ fontWeight: 600 }}>Sem quality.json para {competencia}</div>
          <div style={{ marginTop: 8, fontSize: 12.5, color: '#586a7d' }}>
            Rode a normalização (A11) — o relatório é gravado em{' '}
            <span className="mono">data/manifests/competencia={competencia}/quality.json</span>
          </div>
        </div>
      )}

      {quality && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
            gap: 20,
            alignItems: 'start',
          }}
        >
          <div className="card" style={{ padding: '22px 24px' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Rejeição por arquivo</div>
              <div className="mono" style={{ fontSize: 11, color: '#5b6d80' }}>
                limite {(quality.rejection_threshold * 100).toFixed(1)}%
              </div>
            </div>
            <div style={{ marginTop: 8, fontSize: 12, color: '#566677' }}>
              Total: {formatNumber(quality.total_lines_rejected)} / {formatNumber(quality.total_lines_read)} (
              {(quality.rejection_rate * 100).toFixed(4)}%)
            </div>
            <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 13 }}>
              {rejects.length === 0 ? (
                <div style={{ color: '#566677' }}>Nenhum arquivo no relatório.</div>
              ) : (
                rejects.map((r) => (
                  <div key={r.file}>
                    <div
                      className="mono"
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'baseline',
                        fontSize: 11.5,
                      }}
                    >
                      <span style={{ color: '#3d5065' }}>{r.file}</span>
                      <span style={{ color: r.color }}>{r.pct}</span>
                    </div>
                    <div style={{ marginTop: 6, height: 6, background: '#eaeff4', borderRadius: 3, overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: r.width, background: r.color, borderRadius: 3 }} />
                    </div>
                    <div className="mono" style={{ marginTop: 5, fontSize: 10.5, color: '#5b6d80' }}>
                      {r.detail}
                    </div>
                  </div>
                ))
              )}
            </div>

            {quality.buckets.length > 0 && (
              <div style={{ marginTop: 26, paddingTop: 20, borderTop: '1px solid #e3e8ee' }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>Buckets canônicos (256)</div>
                <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {quality.buckets.map((b) => (
                    <div
                      key={b.tabela}
                      className="mono"
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: 12,
                        border: '1px solid #e3e8ee',
                        borderRadius: 7,
                        padding: '9px 12px',
                        background: '#fafbfd',
                      }}
                    >
                      <span>{b.tabela}</span>
                      <span style={{ color: b.complete ? '#0d7a4f' : '#b3372c' }}>
                        {b.buckets_present}/256 {b.complete ? '✓' : `faltam ${b.missing_buckets.length}`}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div style={{ marginTop: 26, paddingTop: 20, borderTop: '1px solid #e3e8ee' }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Manifesto SHA-256</div>
              <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
                {manifestHashes.length === 0 ? (
                  <div style={{ color: '#566677', fontSize: 12 }}>Manifesto não encontrado.</div>
                ) : (
                  manifestHashes.map((m) => (
                    <div
                      key={m.name}
                      className="mono"
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        fontSize: 11.5,
                        border: '1px solid #e3e8ee',
                        background: '#fafbfd',
                        borderRadius: 7,
                        padding: '9px 12px',
                      }}
                    >
                      <span style={{ color: '#0d7a4f' }}>✓</span>
                      <span style={{ flex: '0 0 150px', color: '#3d5065' }}>{m.name}</span>
                      <span
                        style={{
                          flex: 1,
                          color: '#6b7c90',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {m.hash}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div className="card" style={{ padding: '22px 24px' }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Quarentena de deletes</div>
              <div style={{ marginTop: 6, fontSize: 12, color: '#566677' }}>
                Métricas do último pipeline ({pipeline?.runId ?? 'nenhum snapshot'}).
              </div>
              <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
                {quarantine.length === 0 ? (
                  <div style={{ color: '#566677', fontSize: 12 }}>
                    Rode <span className="mono">CanonicalPipelineCli</span> para gerar{' '}
                    <span className="mono">pipeline-latest.json</span>.
                  </div>
                ) : (
                  quarantine.map((q) => (
                    <div
                      key={q.title}
                      style={{
                        border: `1px solid ${q.border}`,
                        background: q.bg,
                        borderRadius: 9,
                        padding: '13px 15px',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 10 }}>
                        <span style={{ fontSize: 12.5, fontWeight: 600, color: q.color }}>{q.title}</span>
                        <span className="mono" style={{ fontSize: 17, color: q.color }}>
                          {q.count}
                        </span>
                      </div>
                      <div style={{ marginTop: 5, fontSize: 11.5, color: '#586a7d' }}>{q.note}</div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {storage && (
              <div
                style={{
                  border: '1px solid #c7d7f0',
                  background: 'linear-gradient(180deg, rgba(27,95,193,0.045), rgba(27,95,193,0.01))',
                  borderRadius: 11,
                  padding: '22px 24px',
                }}
              >
                <div style={{ fontSize: 14, fontWeight: 600 }}>Volume Parquet vs changelog</div>
                <div style={{ marginTop: 18, display: 'flex', alignItems: 'flex-end', gap: 26 }}>
                  <div style={{ flex: 1 }}>
                    <div
                      style={{
                        height: Math.max(14, parquetPct * 1.3),
                        background: 'linear-gradient(180deg, #cdd6e0, #e7ecf1)',
                        borderRadius: '6px 6px 0 0',
                      }}
                    />
                    <div className="mono" style={{ marginTop: 10, fontSize: 17, color: '#0f2138' }}>
                      {storage.parquetLabel}
                    </div>
                    <div style={{ marginTop: 3, fontSize: 11.5, color: '#566677' }}>
                      Parquet canônico · competência {competencia}
                    </div>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div
                      style={{
                        height: Math.max(14, changelogPct * 1.3),
                        background: 'linear-gradient(180deg, #1b5fc1, rgba(27,95,193,0.32))',
                        borderRadius: '4px 4px 0 0',
                      }}
                    />
                    <div className="mono" style={{ marginTop: 10, fontSize: 17, color: '#0d7a4f' }}>
                      {storage.changelogLabel}
                    </div>
                    <div style={{ marginTop: 3, fontSize: 11.5, color: '#566677' }}>
                      changelog.events · {formatNumber(stats?.totalEvents ?? 0)} eventos
                    </div>
                  </div>
                </div>
                <div style={{ marginTop: 18, fontSize: 11.5, color: '#5b6d80' }}>
                  Barras em escala real ({changelogPct.toFixed(1)}% do volume é changelog).
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
