export function formatNumber(n: number): string {
  return new Intl.NumberFormat('pt-BR').format(n);
}

export function formatTimestamp(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString('pt-BR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

export function shortId(id: string): string {
  return id.slice(0, 8);
}

export function normalizeCnpjInput(value: string): string {
  return value.replace(/[^0-9A-Za-z]/g, '').toUpperCase().slice(0, 14);
}
