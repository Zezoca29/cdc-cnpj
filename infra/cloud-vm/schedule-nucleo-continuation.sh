#!/usr/bin/env bash
# Continua o núcleo A11 após 2026-06 (já em andamento): 2026-07 → cleanup → power-off.
# Uso: ./schedule-nucleo-continuation.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MARKER="/tmp/cdc-nucleo-continuation.pid"
LOG="/tmp/cdc-nucleo-continuation.log"

if [[ -f "$MARKER" ]] && kill -0 "$(cat "$MARKER")" 2>/dev/null; then
  echo "Continuation já ativo (PID $(cat "$MARKER"))"
  exit 0
fi

# Cancela shutdown simples se estiver ativo — evita desligar entre competências.
if [[ -f /tmp/cdc-shutdown-watcher.pid ]]; then
  kill "$(cat /tmp/cdc-shutdown-watcher.pid)" 2>/dev/null || true
  rm -f /tmp/cdc-shutdown-watcher.pid
fi

nohup bash -c "
  set -euo pipefail
  echo \"[nucleo-continuation] aguardando 2026-06 terminar...\"
  while pgrep -f 'cdc-cnpj-ingestion exec:java' >/dev/null 2>&1; do
    sleep 60
  done
  sleep 10
  echo \"[nucleo-continuation] limpando raw 2026-06\"
  rm -rf '${ROOT}/data/raw/competencia=2026-06'
  df -h /mnt/cdc-data 2>/dev/null || df -h .
  echo \"[nucleo-continuation] iniciando A11 2026-07\"
  cd '${ROOT}'
  chmod +x infra/cloud-vm/*.sh
  ./infra/cloud-vm/run-a11.sh 2026-07
  echo \"[nucleo-continuation] limpando raw 2026-07\"
  rm -rf '${ROOT}/data/raw/competencia=2026-07'
  df -h /mnt/cdc-data 2>/dev/null || df -h .
  echo \"[nucleo-continuation] iniciando pipeline de eventos (256 buckets, E3)\"
  cd '${ROOT}'
  ALL_BUCKETS=1 ENGINE=E3 ./infra/cloud-vm/run-canonical-pipeline.sh
  echo \"[nucleo-continuation] validando dashboard\"
  ./infra/cloud-vm/validate-dashboard-ready.sh || true
  echo \"[nucleo-continuation] pipeline de eventos concluída — desligando\"
  '${ROOT}/infra/cloud-vm/power-off.sh'
" >>"$LOG" 2>&1 &

echo $! >"$MARKER"
echo "Continuation ativo (PID $(cat "$MARKER")). Log: $LOG"
