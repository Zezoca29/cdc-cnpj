#!/usr/bin/env bash
# Desliga o servidor Hetzner (para de cobrar compute; volume continua).
# Usa API se HETZNER_TOKEN + HETZNER_SERVER_ID estiverem definidos; senão shutdown local.
set -euo pipefail

LOG_TAG="[power-off]"

if [[ -n "${HETZNER_TOKEN:-}" && -n "${HETZNER_SERVER_ID:-}" ]]; then
  echo "${LOG_TAG} Hetzner API poweroff (server ${HETZNER_SERVER_ID})"
  curl -sf -X POST \
    -H "Authorization: Bearer ${HETZNER_TOKEN}" \
    -H "Content-Type: application/json" \
    "https://api.hetzner.cloud/v1/servers/${HETZNER_SERVER_ID}/actions/poweroff"
  echo "${LOG_TAG} OK"
  exit 0
fi

echo "${LOG_TAG} shutdown -h now (sem API token na VM)"
shutdown -h now
