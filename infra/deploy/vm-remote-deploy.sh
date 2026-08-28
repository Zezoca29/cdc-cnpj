#!/bin/bash
set -euo pipefail
cd /root/TCC

ADMIN_USER=admin
ADMIN_PASS=$(openssl rand -base64 18 | tr -dc 'A-Za-z0-9' | head -c 16)

ENV=/etc/cdc-cnpj/dashboard.env
set_kv() {
  local key="$1" val="$2"
  if grep -q "^${key}=" "$ENV"; then
    sed -i "s|^${key}=.*|${key}=${val}|" "$ENV"
  else
    echo "${key}=${val}" >> "$ENV"
  fi
}

set_kv CDC_PROJECT_ROOT /root/TCC
set_kv CDC_DATA_ROOT /root/TCC/data
set_kv DASHBOARD_SERVER_NAME 2.28.28.54
set_kv DASHBOARD_AUTH_USER "$ADMIN_USER"
set_kv DASHBOARD_AUTH_PASSWORD "$ADMIN_PASS"

export DASHBOARD_SERVER_NAME=2.28.28.54
export DASHBOARD_AUTH_USER=$ADMIN_USER
export DASHBOARD_AUTH_PASSWORD=$ADMIN_PASS

chmod +x infra/deploy/*.sh
./infra/deploy/setup-dashboard.sh
./infra/deploy/deploy-dashboard.sh

echo "===== DASHBOARD CREDENTIALS ====="
echo "URL:      http://2.28.28.54/"
echo "Usuario:  $ADMIN_USER"
echo "Senha:    $ADMIN_PASS"
echo "================================="

curl -sf http://127.0.0.1:8080/api/health && echo "API health OK" || echo "API health FAILED"
systemctl is-active cdc-cnpj-dashboard-api nginx
