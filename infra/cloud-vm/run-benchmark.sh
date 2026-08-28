#!/usr/bin/env bash
# Protocolo definitivo de medição (D2) — requer data/canonical/ e Postgres para E1.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

docker compose up -d

mvn -pl cdc-cnpj-diff exec:java \
  -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCli \
  -Dexec.args="--protocol definitive --summarize"
