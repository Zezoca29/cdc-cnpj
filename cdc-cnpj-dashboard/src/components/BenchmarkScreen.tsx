import { useEffect, useMemo, useState } from 'react';
import { fetchBenchmarks } from '../api/client';
import type { BenchmarkPayload, BenchmarkSummary } from '../types/artifacts';
import { formatNumber } from '../utils/format';

interface BenchmarkScreenProps {
  onNavigateEvents?: () => void;
}

export function BenchmarkScreen({ onNavigateEvents: _ }: BenchmarkScreenProps) {
  const [data, setData] = useState<BenchmarkPayload | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        setData(await fetchBenchmarks());
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Falha ao carregar benchmarks');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const chartRows = useMemo(() => buildChartRows(data?.summaries ?? []), [data]);
  const avgSpeedup = useMemo(() => {
    const speedups = (data?.summaries ?? [])
      .filter((s) => s.engine === 'E3' && s.speedupVsE1 != null)
      .map((s) => s.speedupVsE1 as number);
    if (speedups.length === 0) return null;
    return speedups.reduce((a, b) => a + b, 0) / speedups.length;
  }, [data]);

  if (loading) {
    return <div style={{ padding: '26px 40px', color: '#5b6d80' }}>carregando medições…</div>;
  }

  if (error) {
    return (
      <div style={{ padding: '26px 40px', color: '#b3372c' }}>
        {error}
      </div>
    );
  }

  const empty = !data?.sourceFile || data.summaries.length === 0;

  if (empty) {
    return (
      <div style={{ padding: '26px 40px 0' }}>
        <div className="card" style={{ padding: '24px 26px' }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Nenhum artefato de benchmark encontrado</div>
          <div style={{ marginTop: 10, fontSize: 12.5, color: '#586a7d', lineHeight: 1.6 }}>
            Execute o harness na VM e sincronize{' '}
            <span className="mono">data/measurements/benchmark-definitive.csv</span> (ou{' '}
            <span className="mono">benchmark.csv</span>).
          </div>
        </div>
      </div>
    );
  }

  const benchKpis = [
    {
      label: 'speedup médio E3',
      value: avgSpeedup != null ? `${avgSpeedup.toFixed(2)}×` : '—',
      color: '#1b5fc1',
      note: data?.sourceFile ?? 'benchmark',
    },
    {
      label: 'linhas brutas',
      value: formatNumber(data?.rawRowCount ?? 0),
      color: '#0f2138',
      note: 'medições no CSV (inclui warmup)',
    },
    {
      label: 'full-scale H2',
      value: data?.fullScale ? (data.fullScale.h2Passed ? 'passou' : 'falhou') : '—',
      color: data?.fullScale?.h2Passed ? '#0d7a4f' : '#a86a11',
      note: data?.fullScale
        ? `${data.fullScale.totalWallHours.toFixed(2)} h · ${formatNumber(data.fullScale.totalEvents)} eventos`
        : 'aguardando full-scale-256.csv',
    },
  ];

  const maxHeight = Math.max(...chartRows.flatMap((r) => [r.e1, r.e3]), 1);

  return (
    <div style={{ padding: '26px 40px 0' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 14 }}>
        {benchKpis.map((k) => (
          <div key={k.label} className="card" style={{ padding: 18 }}>
            <div className="mono field-label">{k.label}</div>
            <div className="mono" style={{ marginTop: 12, fontSize: 26, color: k.color }}>
              {k.value}
            </div>
            <div style={{ marginTop: 7, fontSize: 11.5, color: '#566677' }}>{k.note}</div>
          </div>
        ))}
      </div>

      <div className="card" style={{ marginTop: 20, padding: '22px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 20 }}>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Tempo de diff por escala — E1 vs E3</div>
          <div className="mono" style={{ display: 'flex', gap: 16, fontSize: 11 }}>
            <span style={{ color: '#a86a11' }}>■ E1 · Postgres temp</span>
            <span style={{ color: '#1b5fc1' }}>■ E3 · hash Parquet</span>
          </div>
        </div>

        <div
          style={{
            marginTop: 20,
            display: 'flex',
            alignItems: 'flex-end',
            gap: 18,
            height: 200,
            paddingBottom: 26,
            borderBottom: '1px solid #e3e8ee',
          }}
        >
          {chartRows.map((r) => (
            <div
              key={r.label}
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
                height: '100%',
                justifyContent: 'flex-end',
                position: 'relative',
              }}
            >
              <div style={{ display: 'flex', gap: 5, alignItems: 'flex-end', width: '100%', justifyContent: 'center', height: '100%' }}>
                <div
                  style={{
                    width: 22,
                    height: `${(r.e1 / maxHeight) * 100}%`,
                    minHeight: 4,
                    background: 'linear-gradient(180deg, #a86a11, rgba(168,106,17,0.30))',
                    borderRadius: '3px 3px 0 0',
                  }}
                />
                <div
                  style={{
                    width: 22,
                    height: `${(r.e3 / maxHeight) * 100}%`,
                    minHeight: 4,
                    background: 'linear-gradient(180deg, #1b5fc1, rgba(27,95,193,0.28))',
                    borderRadius: '3px 3px 0 0',
                  }}
                />
              </div>
              <div
                className="mono"
                style={{ position: 'absolute', bottom: -22, fontSize: 10.5, color: '#5b6d80' }}
              >
                {r.label}
              </div>
            </div>
          ))}
        </div>

        <table style={{ width: '100%', marginTop: 28, borderCollapse: 'collapse' }} className="mono">
          <thead>
            <tr>
              {['eng', 'tabela', 'scale', 'median_ms', 'q1_ms', 'q3_ms', 'throughput', 'speedup'].map((h) => (
                <th
                  key={h}
                  style={{
                    textAlign: h === 'eng' || h === 'tabela' ? 'left' : 'right',
                    padding: '9px 10px',
                    color: '#5b6d80',
                    fontWeight: 500,
                    fontSize: 10,
                    letterSpacing: '0.09em',
                    textTransform: 'uppercase',
                    borderBottom: '1px solid #e3e8ee',
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data!.summaries.map((r) => (
              <tr key={`${r.engine}-${r.tabela}-${r.bucketScale}`}>
                <td style={{ padding: '9px 10px', color: r.engine === 'E1' ? '#a86a11' : '#1b5fc1', borderBottom: '1px solid #eaeff4' }}>
                  {r.engine}
                </td>
                <td style={{ padding: '9px 10px', color: '#3d5065', borderBottom: '1px solid #eaeff4' }}>{r.tabela}</td>
                <td style={{ padding: '9px 10px', textAlign: 'right', borderBottom: '1px solid #eaeff4' }}>{r.bucketScale}</td>
                <td style={{ padding: '9px 10px', textAlign: 'right', borderBottom: '1px solid #eaeff4' }}>
                  {r.medianWallMs.toFixed(1)}
                </td>
                <td style={{ padding: '9px 10px', textAlign: 'right', borderBottom: '1px solid #eaeff4' }}>
                  {r.q1WallMs.toFixed(1)}
                </td>
                <td style={{ padding: '9px 10px', textAlign: 'right', borderBottom: '1px solid #eaeff4' }}>
                  {r.q3WallMs.toFixed(1)}
                </td>
                <td style={{ padding: '9px 10px', textAlign: 'right', borderBottom: '1px solid #eaeff4' }}>
                  {r.medianThroughput.toFixed(0)}
                </td>
                <td style={{ padding: '9px 10px', textAlign: 'right', color: '#0d7a4f', borderBottom: '1px solid #eaeff4' }}>
                  {r.speedupVsE1 != null ? `${r.speedupVsE1.toFixed(2)}×` : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div
          style={{
            marginTop: 18,
            border: '1px solid #d7dee6',
            background: '#f2f5f8',
            borderRadius: 9,
            padding: '13px 16px',
            fontSize: 12,
            color: '#586a7d',
          }}
        >
          Fonte: <span className="mono">{data!.sourceFile}</span> · E1 usa Postgres como motor de comparação temporário;
          E3 usa hash sobre Parquet. O Postgres permanente guarda somente{' '}
          <span className="mono">changelog.events</span>.
        </div>
      </div>
    </div>
  );
}

function buildChartRows(summaries: BenchmarkSummary[]): { label: string; e1: number; e3: number }[] {
  const scales = [...new Set(summaries.map((s) => s.bucketScale))].sort((a, b) => a - b);
  const tabela = summaries[0]?.tabela ?? 'empresas';
  return scales.map((scale) => {
    const e1 = summaries.find((s) => s.engine === 'E1' && s.bucketScale === scale && s.tabela === tabela);
    const e3 = summaries.find((s) => s.engine === 'E3' && s.bucketScale === scale && s.tabela === tabela);
    return {
      label: String(scale),
      e1: e1?.medianWallMs ?? 0,
      e3: e3?.medianWallMs ?? 0,
    };
  });
}
