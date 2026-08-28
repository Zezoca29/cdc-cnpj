#!/usr/bin/env bash
# Primeiro evento real: canonical → diff → Kafka → changelog.
# Variáveis: BUCKET (default 0), ALL_BUCKETS=1 (256 buckets), ENGINE (default E3).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BUCKET="${BUCKET:-0}"
ENGINE="${ENGINE:-E3}"
LOG_DIR="$ROOT/data/logs"
LOG_FILE="$LOG_DIR/canonical-pipeline-$(date -u +%Y%m%dT%H%M%SZ).log"

mkdir -p "$LOG_DIR"
cd "$ROOT"

ARGS="--engine ${ENGINE} --truncate-changelog"
if [[ "${ALL_BUCKETS:-0}" == "1" ]]; then
  ARGS="${ARGS} --all-buckets"
else
  ARGS="${ARGS} --bucket ${BUCKET}"
fi

{
  echo "=== Canonical pipeline $(date -u -Iseconds) ==="
  echo "args: ${ARGS}"
  docker compose up -d
  mvn -pl cdc-cnpj-diff,cdc-cnpj-events -am install -DskipTests -q
  mvn -pl cdc-cnpj-events exec:java \
    -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.CanonicalPipelineCli \
    -Dexec.args="${ARGS}"
  echo "=== Canonical pipeline OK $(date -u -Iseconds) ==="
} 2>&1 | tee "$LOG_FILE"
