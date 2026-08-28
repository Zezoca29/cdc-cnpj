#!/usr/bin/env bash
# Instalação inicial do dashboard em produção (Ubuntu 22.04/24.04 na VM).
# Uso: sudo ./infra/deploy/setup-dashboard.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${ENV_FILE:-/etc/cdc-cnpj/dashboard.env}"
ENV_EXAMPLE="$SCRIPT_DIR/dashboard.env.example"

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  echo "Execute com sudo: sudo $0"
  exit 1
fi

echo "==> Pacotes: nginx, apache2-utils (htpasswd), Node.js 20"
apt-get update -qq
apt-get install -y -qq nginx apache2-utils curl ca-certificates

if ! command -v node >/dev/null 2>&1 || ! node --version | grep -qE '^v2[0-9]'; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  apt-get install -y -qq nodejs
fi

echo "==> Diretórios"
install -d -m 0755 /etc/cdc-cnpj
install -d -m 0755 /opt/cdc-cnpj
install -d -m 0755 /var/www/cdc-cnpj-dashboard

if [[ ! -f "$ENV_FILE" ]]; then
  echo "==> Criando $ENV_FILE a partir do exemplo"
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  # Ajuste padrão para layout comum na VM Hetzner
  if [[ -d /mnt/cdc-data/TCC ]]; then
    sed -i 's|CDC_PROJECT_ROOT=.*|CDC_PROJECT_ROOT=/mnt/cdc-data/TCC|' "$ENV_FILE"
    sed -i 's|CDC_DATA_ROOT=.*|CDC_DATA_ROOT=/mnt/cdc-data/TCC/data|' "$ENV_FILE"
  fi
  echo "Edite $ENV_FILE antes do primeiro deploy se necessário."
fi

# shellcheck disable=SC1090
source "$ENV_FILE" 2>/dev/null || true
SERVER_NAME="${DASHBOARD_SERVER_NAME:-_}"
AUTH_USER="${DASHBOARD_AUTH_USER:-}"
AUTH_PASS="${DASHBOARD_AUTH_PASSWORD:-}"

AUTH_SNIPPET="/etc/nginx/cdc-cnpj-dashboard-auth.conf"
if [[ -n "$AUTH_USER" && -n "$AUTH_PASS" ]]; then
  HTPASSWD_FILE="/etc/nginx/.htpasswd-cdc-cnpj"
  htpasswd -bc "$HTPASSWD_FILE" "$AUTH_USER" "$AUTH_PASS"
  chmod 640 "$HTPASSWD_FILE"
  chown root:www-data "$HTPASSWD_FILE"
  cat >"$AUTH_SNIPPET" <<'EOF'
    auth_basic "CDC-CNPJ";
    auth_basic_user_file /etc/nginx/.htpasswd-cdc-cnpj;
EOF
  echo "==> Basic Auth habilitado (usuário: $AUTH_USER)"
elif [[ -f /etc/nginx/.htpasswd-cdc-cnpj ]]; then
  cat >"$AUTH_SNIPPET" <<'EOF'
    auth_basic "CDC-CNPJ";
    auth_basic_user_file /etc/nginx/.htpasswd-cdc-cnpj;
EOF
  echo "==> Basic Auth: usando /etc/nginx/.htpasswd-cdc-cnpj existente"
else
  echo "# Basic Auth desabilitado" >"$AUTH_SNIPPET"
  echo "AVISO: sem Basic Auth. Defina DASHBOARD_AUTH_USER e DASHBOARD_AUTH_PASSWORD em $ENV_FILE e rode de novo,"
  echo "      ou: sudo htpasswd -c /etc/nginx/.htpasswd-cdc-cnpj admin"
fi

echo "==> Nginx site"
sed -e "s|__SERVER_NAME__|$SERVER_NAME|g" \
    -e "s|__WEB_ROOT__|/var/www/cdc-cnpj-dashboard|g" \
    "$SCRIPT_DIR/nginx/cdc-cnpj-dashboard.conf" >/etc/nginx/sites-available/cdc-cnpj-dashboard
ln -sf /etc/nginx/sites-available/cdc-cnpj-dashboard /etc/nginx/sites-enabled/cdc-cnpj-dashboard
rm -f /etc/nginx/sites-enabled/default
nginx -t

echo "==> systemd"
install -m 0644 "$SCRIPT_DIR/systemd/cdc-cnpj-dashboard-api.service" /etc/systemd/system/cdc-cnpj-dashboard-api.service
systemctl daemon-reload
systemctl enable cdc-cnpj-dashboard-api.service

echo "==> Firewall (ufw): liberar 80/tcp se ufw estiver ativo"
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  ufw allow 80/tcp
  ufw allow 443/tcp 2>/dev/null || true
fi

echo ""
echo "Setup concluído. Próximo passo (como usuário com acesso ao repo):"
echo "  cd $REPO_ROOT && ./infra/deploy/deploy-dashboard.sh"
echo ""
echo "Depois: systemctl status cdc-cnpj-dashboard-api nginx"
