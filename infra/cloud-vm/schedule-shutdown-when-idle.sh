#!/usr/bin/env bash
# Agenda desligamento quando não houver mais IngestionPipelineCli nem run-batch.sh rodando.
# Útil se o batch foi iniciado manualmente sem run-batch.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MARKER="/tmp/cdc-shutdown-watcher.pid"

if [[ -f "$MARKER" ]] && kill -0 "$(cat "$MARKER")" 2>/dev/null; then
  echo "Watcher já ativo (PID $(cat "$MARKER"))"
  exit 0
fi

nohup bash -c "
  echo \"[shutdown-watcher] aguardando pipeline terminar...\"
  while pgrep -f 'IngestionPipelineCli|run-batch.sh' >/dev/null 2>&1; do
    sleep 60
  done
  sleep 30
  echo \"[shutdown-watcher] pipeline idle — desligando\"
  '${ROOT}/infra/cloud-vm/power-off.sh'
" >/tmp/cdc-shutdown-watcher.log 2>&1 &

echo $! >"$MARKER"
echo "Watcher ativo (PID $(cat "$MARKER")). Log: /tmp/cdc-shutdown-watcher.log"
