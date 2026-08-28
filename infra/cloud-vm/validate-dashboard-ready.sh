#!/usr/bin/env bash
# Valida gates do plano "dashboard cheio" (A11 ×2 + pipeline canônico).
# Uso: ./infra/cloud-vm/validate-dashboard-ready.sh
# Exit 0 = pronto para uso externo; exit 1 = ainda em andamento ou falha.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

FAIL=0
warn() { echo "WARN: $*"; }
ok() { echo "OK:   $*"; }
fail() { echo "FAIL: $*"; FAIL=1; }

check_competencia() {
  local comp="$1"
  local canon="data/canonical/competencia=${comp}"
  local quality="data/manifests/competencia=${comp}/quality.json"

  if [[ -d "$canon" ]] && [[ "$(find "$canon" -name '*.parquet' 2>/dev/null | wc -l)" -gt 0 ]]; then
    ok "canonical ${comp} ($(du -sh "$canon" 2>/dev/null | awk '{print $1}'))"
  else
    fail "canonical ${comp} ausente ou sem Parquet"
  fi

  if [[ -f "$quality" ]]; then
    ok "quality.json ${comp}"
  else
    warn "quality.json ${comp} ainda não gerado"
  fi
}

echo "=== CDC-CNPJ — validate dashboard ready ==="
echo "Raiz: $ROOT"
df -h /mnt/cdc-data 2>/dev/null | tail -1 || df -h . | tail -1
echo

if pgrep -f 'IngestionPipelineCli' >/dev/null 2>&1; then
  warn "A11 em execução ($(pgrep -af IngestionPipelineCli | head -1))"
else
  ok "nenhum IngestionPipelineCli ativo"
fi

if [[ -f /tmp/cdc-nucleo-continuation.pid ]] && kill -0 "$(cat /tmp/cdc-nucleo-continuation.pid)" 2>/dev/null; then
  ok "watcher nucleo-continuation (PID $(cat /tmp/cdc-nucleo-continuation.pid))"
elif pgrep -f 'nucleo-continuation' >/dev/null 2>&1; then
  ok "watcher nucleo-continuation (processo ativo)"
else
  warn "watcher nucleo-continuation inativo — rode schedule-nucleo-continuation.sh"
fi

echo
check_competencia "2026-06"
check_competencia "2026-07"

echo
if [[ -f data/measurements/pipeline-latest.json ]]; then
  ok "pipeline-latest.json"
else
  warn "pipeline-latest.json ausente (pipeline canônico não concluiu)"
fi

EVENT_COUNT=""
if docker compose ps -q postgres >/dev/null 2>&1; then
  EVENT_COUNT="$(docker compose exec -T postgres psql -U cdc -d cdc_cnpj -tAc 'select count(*) from changelog.events;' 2>/dev/null || true)"
fi

if [[ -n "$EVENT_COUNT" && "$EVENT_COUNT" -gt 0 ]]; then
  ok "changelog.events = ${EVENT_COUNT}"
else
  fail "changelog.events vazio ou Postgres indisponível"
fi

echo
if curl -sf --max-time 5 http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
  ok "API /api/health"
else
  warn "API não responde em localhost:8080"
fi

NGINX_CODE="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1/ 2>/dev/null || echo 000)"
if [[ "$NGINX_CODE" == "200" || "$NGINX_CODE" == "401" ]]; then
  ok "Nginx responde na porta 80 (HTTP ${NGINX_CODE})"
else
  warn "Nginx não responde em localhost:80 (HTTP ${NGINX_CODE})"
fi

echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "RESULTADO: dashboard pronto para dados de eventos."
  exit 0
fi

echo "RESULTADO: pipeline ainda não concluído ou com falha — veja WARN/FAIL acima."
exit 1
