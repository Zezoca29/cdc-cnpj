#!/usr/bin/env bash
# Executar no notebook/Linux com scp. Trazer medições e manifestos (não o canonical inteiro).
# Uso: VM_USER=ubuntu VM_HOST=1.2.3.4 ./fetch-artifacts.sh
set -euo pipefail

VM_USER="${VM_USER:?defina VM_USER}"
VM_HOST="${VM_HOST:?defina VM_HOST}"
REMOTE_DIR="${REMOTE_DIR:-$VM_USER@$VM_HOST:~/TCC}"
LOCAL_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

mkdir -p "$LOCAL_ROOT/data/measurements" "$LOCAL_ROOT/data/manifests"

scp -r "$REMOTE_DIR/data/measurements/"* "$LOCAL_ROOT/data/measurements/" 2>/dev/null || true
scp -r "$REMOTE_DIR/data/manifests/"* "$LOCAL_ROOT/data/manifests/" 2>/dev/null || true

echo "Artefatos em $LOCAL_ROOT/data/measurements e data/manifests"
