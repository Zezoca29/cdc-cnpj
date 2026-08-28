#!/usr/bin/env bash
# Primeiro evento real: canonical → diff → Kafka → changelog.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BUCKET="${BUCKET:-0}"
ENGINE="${ENGINE:-E3}"

cd "$ROOT"
docker compose up -d

mvn -pl cdc-cnpj-diff,cdc-cnpj-events -am install -DskipTests -q

mvn -pl cdc-cnpj-events exec:java \
  -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.CanonicalPipelineCli \
  -Dexec.args="--engine ${ENGINE} --bucket ${BUCKET} --truncate-changelog"
