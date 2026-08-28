#!/usr/bin/env bash
# Bootstrap da máquina de referência (Ubuntu 22.04/24.04).
set -euo pipefail

REPO_URL="${REPO_URL:-}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/TCC}"

echo "==> Instalando pacotes base"
sudo apt-get update -qq
sudo apt-get install -y -qq git curl ca-certificates gnupg

if ! command -v java >/dev/null 2>&1 || ! java --version 2>&1 | grep -q '21'; then
  echo "==> Java 21"
  sudo apt-get install -y -qq openjdk-21-jdk-headless
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "==> Maven"
  sudo apt-get install -y -qq maven
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "==> Docker"
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" |
    sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -qq
  sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
  sudo usermod -aG docker "$USER"
  echo "AVISO: faça logout/login para usar docker sem sudo"
fi

if [[ -n "$REPO_URL" && ! -d "$INSTALL_DIR/.git" ]]; then
  echo "==> Clonando $REPO_URL em $INSTALL_DIR"
  git clone "$REPO_URL" "$INSTALL_DIR"
fi

echo "==> Verificando toolchain"
java --version
mvn --version
docker --version
docker compose version

if [[ -d "$INSTALL_DIR" ]]; then
  cd "$INSTALL_DIR"
  echo "==> Compilando módulos (sem testes, rápido)"
  mvn -q -pl cdc-cnpj-common,cdc-cnpj-ingestion,cdc-cnpj-diff,cdc-cnpj-events -am install -DskipTests
  python3 scripts/harness_check.py 2>/dev/null || echo "python3 harness_check opcional"
  echo "Bootstrap OK. Próximo: ./infra/cloud-vm/run-a11.sh 2026-06"
else
  echo "Bootstrap OK. Clone o repositório em $INSTALL_DIR e rode run-a11.sh"
fi
