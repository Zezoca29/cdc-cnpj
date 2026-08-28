import type { Screen } from '../types/event';

const TITLES: Record<Screen, { kicker: string; title: string }> = {
  overview: { kicker: 'visão geral', title: 'Overview do pipeline' },
  events: { kicker: 'changelog', title: 'Explorador de Eventos' },
  entity: { kicker: 'entidade', title: 'Timeline por CNPJ' },
  bench: { kicker: 'medição', title: 'Benchmarks E1 vs E3' },
  quality: { kicker: 'qualidade', title: 'Qualidade e manifestos' },
};

interface HeaderProps {
  screen: Screen;
  competenciaDe?: string | null;
  competenciaPara?: string | null;
}

export function Header({ screen, competenciaDe, competenciaPara }: HeaderProps) {
  const meta = TITLES[screen];
  const de = competenciaDe ?? '—';
  const para = competenciaPara ?? '—';

  return (
    <header
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 5,
        background: 'rgba(245,247,250,0.86)',
        backdropFilter: 'blur(10px)',
        borderBottom: '1px solid #e3e8ee',
        padding: '20px 40px 18px',
        display: 'flex',
        alignItems: 'flex-end',
        gap: 24,
      }}
    >
      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="mono" style={{ fontSize: 10, letterSpacing: '0.14em', color: '#5b6d80', textTransform: 'uppercase' }}>
          {meta.kicker}
        </div>
        <h1 style={{ margin: '6px 0 0', fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em' }}>{meta.title}</h1>
      </div>
      <div className="mono" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
        <span style={{ color: '#5b6d80' }}>competência</span>
        <span style={{ border: '1px solid #cdd6e0', background: '#f2f5f8', borderRadius: 6, padding: '6px 10px', color: '#0f2138' }}>
          {de}
        </span>
        <span style={{ color: '#1b5fc1' }}>→</span>
        <span
          style={{
            border: '1px solid #c7d7f0',
            background: 'rgba(27,95,193,0.06)',
            borderRadius: 6,
            padding: '6px 10px',
            color: '#0d7a4f',
          }}
        >
          {para}
        </span>
      </div>
    </header>
  );
}
