#!/usr/bin/env bash
# Download + normalização + validação de uma competência (A11).
# Uso: ./run-a11.sh 2026-06 [--skip-download]
set -euo pipefail

COMPETENCIA="${1:?informe a competência, ex.: 2026-06}"
shift || true
EXTRA_ARGS=("$@")
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG_DIR="$ROOT/data/logs"
LOG_FILE="$LOG_DIR/a11-${COMPETENCIA}.log"

mkdir -p "$LOG_DIR"
cd "$ROOT"

echo "=== A11 competencia=$COMPETENCIA ===" | tee "$LOG_FILE"
echo "Raiz: $ROOT" | tee -a "$LOG_FILE"
df -h . | tee -a "$LOG_FILE"

mvn -pl cdc-cnpj-ingestion exec:java \
  -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli \
  -Dexec.args="--competencia ${COMPETENCIA} ${EXTRA_ARGS[*]}" \
  2>&1 | tee -a "$LOG_FILE"

echo "=== Concluído. Para liberar disco dos ZIPs brutos: ===" | tee -a "$LOG_FILE"
echo "  rm -rf data/raw/competencia=${COMPETENCIA}" | tee -a "$LOG_FILE"
