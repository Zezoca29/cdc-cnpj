#!/usr/bin/env bash
# Build e publicação do dashboard (API JAR + frontend estático).
# Uso: ./infra/deploy/deploy-dashboard.sh
# Requer setup-dashboard.sh já executado (sudo) na VM.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${ENV_FILE:-/etc/cdc-cnpj/dashboard.env}"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

PROJECT_ROOT="${CDC_PROJECT_ROOT:-$REPO_ROOT}"
DATA_ROOT="${CDC_DATA_ROOT:-$PROJECT_ROOT/data}"

cd "$PROJECT_ROOT"

echo "==> Projeto: $PROJECT_ROOT"
echo "==> Data:    $DATA_ROOT"

echo "==> Docker (Postgres + Kafka)"
docker compose up -d
docker compose ps

echo "==> Build API (fat JAR)"
mvn -q -pl cdc-cnpj-events -am package -DskipTests
JAR="$(ls -1 cdc-cnpj-events/target/cdc-cnpj-events-*-dashboard.jar | head -1)"
if [[ ! -f "$JAR" ]]; then
  echo "JAR não encontrado. Esperado: cdc-cnpj-events/target/*-dashboard.jar"
  exit 1
fi

echo "==> Build frontend"
cd cdc-cnpj-dashboard
npm ci
npm run build
cd "$PROJECT_ROOT"

echo "==> Publicar artefatos"
if [[ "${EUID:-$(id -u)}" -eq 0 ]]; then
  install -m 0644 "$JAR" /opt/cdc-cnpj/dashboard-api.jar
  rsync -a --delete cdc-cnpj-dashboard/dist/ /var/www/cdc-cnpj-dashboard/
  systemctl restart cdc-cnpj-dashboard-api.service
  systemctl reload nginx.service
else
  sudo install -m 0644 "$JAR" /opt/cdc-cnpj/dashboard-api.jar
  sudo rsync -a --delete cdc-cnpj-dashboard/dist/ /var/www/cdc-cnpj-dashboard/
  sudo systemctl restart cdc-cnpj-dashboard-api.service
  sudo systemctl reload nginx.service
fi

echo "==> Health"
sleep 2
if curl -sf http://127.0.0.1:8080/api/health >/dev/null; then
  echo "API OK (localhost:8080)"
else
  echo "AVISO: API não respondeu em /api/health — veja: journalctl -u cdc-cnpj-dashboard-api -n 50"
fi

PUBLIC_URL="http://${DASHBOARD_SERVER_NAME:-127.0.0.1}"
echo ""
echo "Deploy concluído."
echo "  Dashboard: $PUBLIC_URL"
echo "  Logs API:  journalctl -u cdc-cnpj-dashboard-api -f"
