export interface EventBadge {
  fg: string;
  bg: string;
  bd: string;
}

const META: Record<string, EventBadge> = {
  'empresa.criada': { fg: '#0d7a4f', bg: 'rgba(13,122,79,0.07)', bd: 'rgba(13,122,79,0.26)' },
  'empresa.encerrada': { fg: '#b3372c', bg: 'rgba(179,55,44,0.07)', bd: 'rgba(179,55,44,0.24)' },
  'empresa.alterada': { fg: '#12688f', bg: 'rgba(18,104,143,0.07)', bd: 'rgba(18,104,143,0.24)' },
  'estabelecimento.criado': { fg: '#0d7a4f', bg: 'rgba(13,122,79,0.07)', bd: 'rgba(13,122,79,0.26)' },
  'estabelecimento.situacao_alterada': {
    fg: '#a86a11',
    bg: 'rgba(168,106,17,0.08)',
    bd: 'rgba(168,106,17,0.26)',
  },
  'estabelecimento.endereco_alterado': {
    fg: '#1b5fc1',
    bg: 'rgba(47,212,171,0.10)',
    bd: 'rgba(27,95,193,0.16)',
  },
  'estabelecimento.cnae_alterado': {
    fg: '#1b5fc1',
    bg: 'rgba(47,212,171,0.10)',
    bd: 'rgba(27,95,193,0.16)',
  },
  'estabelecimento.alterado': {
    fg: '#1b5fc1',
    bg: 'rgba(47,212,171,0.10)',
    bd: 'rgba(27,95,193,0.16)',
  },
  'dominio.tabela_alterada': { fg: '#54419f', bg: 'rgba(84,65,159,0.07)', bd: 'rgba(84,65,159,0.24)' },
};

const DEFAULT_BADGE: EventBadge = {
  fg: '#3d5065',
  bg: '#eaeff4',
  bd: '#d7dee6',
};

export function eventBadge(eventType: string): EventBadge {
  return META[eventType] ?? DEFAULT_BADGE;
}

export function primaryEntityKey(entityKey: Record<string, string>): {
  label: string;
  value: string;
} {
  if (entityKey.cnpj) return { label: 'entity_key.cnpj', value: entityKey.cnpj };
  if (entityKey.cnpj_basico) return { label: 'entity_key.cnpj_basico', value: entityKey.cnpj_basico };
  if (entityKey.tabela) return { label: 'entity_key.tabela', value: entityKey.tabela };
  const first = Object.entries(entityKey)[0];
  return first ? { label: `entity_key.${first[0]}`, value: first[1] } : { label: 'entity_key', value: '—' };
}
