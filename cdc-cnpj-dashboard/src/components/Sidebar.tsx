import type { Screen } from '../types/event';

interface NavItem {
  id: Screen;
  label: string;
  star?: boolean;
}

const NAV: NavItem[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'events', label: 'Explorador de Eventos', star: true },
  { id: 'entity', label: 'Entidade' },
  { id: 'bench', label: 'Benchmarks' },
  { id: 'quality', label: 'Qualidade' },
];

interface SidebarProps {
  screen: Screen;
  onNavigate: (screen: Screen) => void;
  totalEvents?: number;
  competenciaDe?: string | null;
  competenciaPara?: string | null;
}

export function Sidebar({ screen, onNavigate, totalEvents, competenciaDe, competenciaPara }: SidebarProps) {
  const compLabel =
    competenciaDe && competenciaPara ? `${competenciaDe} → ${competenciaPara}` : 'aguardando pipeline';

  return (
    <aside
      style={{
        width: 252,
        flex: '0 0 252px',
        borderRight: '1px solid #e3e8ee',
        background: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        position: 'sticky',
        top: 0,
        height: '100vh',
      }}
    >
      <div style={{ padding: '26px 22px 20px', borderBottom: '1px solid #e3e8ee' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
          <div
            style={{
              width: 10,
              height: 10,
              borderRadius: 2,
              background: '#1b5fc1',
              boxShadow: '0 0 12px rgba(27,95,193,0.30)',
            }}
          />
          <div className="mono" style={{ fontSize: 15, fontWeight: 700, letterSpacing: '-0.02em' }}>
            CDC‑CNPJ
          </div>
        </div>
        <div style={{ marginTop: 7, fontSize: 11.5, color: '#566677', letterSpacing: '0.02em' }}>
          TCC · Change Data Capture sobre snapshots da base pública de CNPJ
        </div>
      </div>

      <nav style={{ padding: '14px 12px', display: 'flex', flexDirection: 'column', gap: 3 }}>
        {NAV.map((item) => {
          const active = screen === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onNavigate(item.id)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                width: '100%',
                textAlign: 'left',
                padding: '9px 11px',
                borderRadius: 7,
                border: `1px solid ${active ? '#c7d7f0' : 'transparent'}`,
                background: active ? 'rgba(27,95,193,0.07)' : 'transparent',
                color: active ? '#0d7a4f' : '#3d5065',
                fontSize: 13,
                fontWeight: 500,
                cursor: 'pointer',
              }}
            >
              <span
                style={{
                  width: 3,
                  height: 15,
                  borderRadius: 2,
                  background: active ? '#1b5fc1' : 'transparent',
                  flex: '0 0 3px',
                }}
              />
              <span style={{ flex: 1 }}>{item.label}</span>
              {item.star && (
                <span
                  className="mono"
                  style={{
                    fontSize: 9.5,
                    color: '#1b5fc1',
                    border: '1px solid rgba(27,95,193,0.28)',
                    borderRadius: 4,
                    padding: '1px 4px',
                    letterSpacing: '0.04em',
                  }}
                >
                  MAIN
                </span>
              )}
            </button>
          );
        })}
      </nav>

      <div style={{ marginTop: 'auto', padding: 16 }}>
        <div
          style={{
            border: '1px solid #c7d7f0',
            background: 'linear-gradient(180deg, rgba(27,95,193,0.05), rgba(27,95,193,0.015))',
            borderRadius: 9,
            padding: '13px 13px 12px',
          }}
        >
          <div className="mono" style={{ fontSize: 9.5, letterSpacing: '0.1em', color: '#1b5fc1', textTransform: 'uppercase' }}>
            arquitetura
          </div>
          <div style={{ marginTop: 7, fontSize: 12, color: '#3d5065', lineHeight: 1.5 }}>
            Base completa em <strong style={{ color: '#0f2138', fontWeight: 600 }}>Parquet</strong> (256 buckets) · Banco ={' '}
            <strong style={{ color: '#0f2138', fontWeight: 600 }}>changelog de deltas</strong>.
          </div>
          <div className="mono" style={{ marginTop: 9, display: 'flex', gap: 6, fontSize: 10, color: '#566677' }}>
            <span>{compLabel}</span>
            {totalEvents !== undefined && (
              <>
                <span style={{ color: '#7a8c9f' }}>·</span>
                <span>{totalEvents.toLocaleString('pt-BR')} eventos</span>
              </>
            )}
          </div>
        </div>
        <div className="mono" style={{ marginTop: 12, fontSize: 10, color: '#6b7c90' }}>
          protótipo acadêmico · não é serviço da RFB
        </div>
      </div>
    </aside>
  );
}
