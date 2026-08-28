#!/usr/bin/env bash
# Batch mensal: A11 de uma ou duas competências, limpa ZIPs brutos, desliga o servidor.
# Uso: ./run-batch.sh 2026-06 [2026-07]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
COMPETENCIA1="${1:?informe a competência, ex.: 2026-06}"
COMPETENCIA2="${2:-}"
LOG_DIR="$ROOT/data/logs"
BATCH_LOG="$LOG_DIR/batch-$(date -u +%Y%m%dT%H%M%SZ).log"

mkdir -p "$LOG_DIR"
cd "$ROOT"

exec > >(tee -a "$BATCH_LOG") 2>&1

echo "=== BATCH START $(date -u -Iseconds) ==="
df -h /mnt/cdc-data 2>/dev/null || df -h .

run_one() {
  local competencia="$1"
  echo "=== A11 ${competencia} ==="
  "$ROOT/infra/cloud-vm/run-a11.sh" "$competencia"
  echo "=== Limpando raw ${competencia} ==="
  rm -rf "$ROOT/data/raw/competencia=${competencia}"
  df -h /mnt/cdc-data 2>/dev/null || df -h .
}

run_one "$COMPETENCIA1"
if [[ -n "$COMPETENCIA2" ]]; then
  run_one "$COMPETENCIA2"
fi

echo "=== BATCH DONE $(date -u -Iseconds) ==="
"$ROOT/infra/cloud-vm/power-off.sh"
