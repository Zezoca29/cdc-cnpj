import { useState } from 'react';
import type { ChangelogEvent } from '../types/event';
import { eventBadge, primaryEntityKey } from '../utils/eventMeta';
import { formatTimestamp, shortId } from '../utils/format';
import { bucketLabel } from '../utils/bucket';

interface DiffLine {
  sign: string;
  text: string;
  color: string;
  bg: string;
}

function buildDiffLines(event: ChangelogEvent): DiffLine[] {
  const lines: DiffLine[] = [];
  const fields = event.camposAlterados.length > 0 ? event.camposAlterados : Object.keys({ ...event.before, ...event.after });

  for (const field of fields) {
    const beforeVal = event.before[field];
    const afterVal = event.after[field];
    if (beforeVal !== undefined) {
      lines.push({
        sign: '−',
        text: `  "${field}": "${beforeVal}"`,
        color: '#b3372c',
        bg: 'rgba(179,55,44,0.04)',
      });
    }
    if (afterVal !== undefined) {
      lines.push({
        sign: '+',
        text: `  "${field}": "${afterVal}"`,
        color: '#0d7a4f',
        bg: 'rgba(13,122,79,0.04)',
      });
    }
  }
  return lines;
}

interface EventCardProps {
  event: ChangelogEvent;
  open: boolean;
  onToggle: () => void;
}

export function EventCard({ event, open, onToggle }: EventCardProps) {
  const badge = eventBadge(event.eventType);
  const key = primaryEntityKey(event.entityKey);
  const cnpjBasico = event.entityKey.cnpj_basico ?? event.entityKey.cnpj?.slice(0, 8);
  const bucket = bucketLabel(cnpjBasico);
  const diffLines = buildDiffLines(event);
  const [copied, setCopied] = useState(false);

  async function copyId() {
    await navigator.clipboard.writeText(event.eventId);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  }

  return (
    <article className="card" style={{ overflow: 'hidden' }}>
      <div style={{ padding: '20px 22px', display: 'flex', gap: 20, alignItems: 'flex-start' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <span
              className="mono"
              style={{
                fontSize: 11.5,
                fontWeight: 500,
                padding: '4px 9px',
                borderRadius: 5,
                background: badge.bg,
                color: badge.fg,
                border: `1px solid ${badge.bd}`,
              }}
            >
              {event.eventType}
            </span>
            <span className="mono" style={{ fontSize: 11.5, color: '#566677' }}>
              {event.competenciaDe}
            </span>
            <span style={{ color: '#1b5fc1', fontSize: 12 }}>→</span>
            <span className="mono" style={{ fontSize: 11.5, color: '#3d5065' }}>
              {event.competenciaPara}
            </span>
            <span style={{ width: 1, height: 12, background: '#cdd6e0' }} />
            <span className="mono" style={{ fontSize: 11, color: '#5b6d80' }}>
              bucket {bucket}
            </span>
            <span className="mono" style={{ fontSize: 11, color: '#5b6d80' }}>
              {event.pipelineEstrategia}
            </span>
          </div>

          <div style={{ marginTop: 13, display: 'flex', alignItems: 'baseline', gap: 10 }}>
            <span className="mono field-label" style={{ marginRight: 10 }}>
              {key.label}
            </span>
            <span className="mono" style={{ fontSize: 19, fontWeight: 500, letterSpacing: '0.06em', color: '#0f2138' }}>
              {key.value}
            </span>
          </div>

          <div style={{ marginTop: 13, display: 'flex', alignItems: 'center', gap: 7, flexWrap: 'wrap' }}>
            <span className="mono field-label" style={{ marginRight: 3 }}>
              campos_alterados
            </span>
            {event.camposAlterados.map((c) => (
              <span
                key={c}
                className="mono"
                style={{
                  fontSize: 11,
                  color: '#3d5065',
                  background: '#eaeff4',
                  border: '1px solid #d7dee6',
                  borderRadius: 20,
                  padding: '3px 10px',
                }}
              >
                {c}
              </span>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 10, flex: '0 0 auto' }}>
          <span className="mono" style={{ fontSize: 11, color: '#5b6d80' }}>
            {formatTimestamp(event.consumedAt ?? event.detectedAt)}
          </span>
          <button
            type="button"
            onClick={copyId}
            className="mono"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 7,
              border: '1px solid #d7dee6',
              background: '#f2f5f8',
              borderRadius: 6,
              padding: '5px 9px',
              cursor: 'pointer',
              fontSize: 10.5,
              color: '#586a7d',
            }}
          >
            <span>{shortId(event.eventId)}</span>
            <span style={{ color: '#1b5fc1' }}>{copied ? 'copiado' : 'copiar'}</span>
          </button>
          <button type="button" onClick={onToggle} className="btn" style={{ fontSize: 11.5 }}>
            {open ? 'ocultar diff' : 'ver diff JSON'}
          </button>
        </div>
      </div>

      {open && (
        <div style={{ borderTop: '1px solid #e3e8ee', background: '#fafbfd', padding: '16px 22px 18px' }}>
          <div
            className="mono"
            style={{
              display: 'flex',
              gap: 16,
              marginBottom: 11,
              fontSize: 10,
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
            }}
          >
            <span style={{ color: '#b3372c' }}>− before</span>
            <span style={{ color: '#0d7a4f' }}>+ after</span>
            <span style={{ color: '#5b6d80' }}>· diff sobre os campos do evento</span>
          </div>
          <div
            className="mono"
            style={{
              fontSize: 12.5,
              lineHeight: 1.75,
              border: '1px solid #e3e8ee',
              borderRadius: 8,
              overflow: 'hidden',
            }}
          >
            {diffLines.map((line, i) => (
              <div key={i} style={{ display: 'flex', gap: 12, padding: '1px 14px', background: line.bg }}>
                <span style={{ width: 8, color: line.color, flex: '0 0 8px' }}>{line.sign}</span>
                <span style={{ color: line.color, whiteSpace: 'pre' }}>{line.text}</span>
              </div>
            ))}
          </div>
          <div className="mono" style={{ marginTop: 12, fontSize: 11, color: '#5b6d80' }}>
            event_id {event.eventId}
          </div>
        </div>
      )}
    </article>
  );
}
