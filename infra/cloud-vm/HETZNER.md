# Hetzner — máquina de referência (A11)

Console: **https://console.hetzner.cloud/**

Custo estimado: **~€18–25/mês** (servidor + volume 250 GB). Desligue o servidor quando não estiver
rodando batch — o volume cobra mesmo parado, mas é bem menos que manter tudo ligado 24/7.

> Hetzner **não tem datacenter no Brasil**. Use **Falkenstein (fsn1)** ou **Nuremberg (nbg1)** na
> Europa. O download da RF funciona; só será um pouco mais lento que São Paulo.

---

## Passo 1 — Conta e projeto

1. Crie conta em https://console.hetzner.cloud/
2. **New Project** → nome ex.: `cdc-cnpj-tcc`

---

## Passo 2 — Chave SSH (no notebook Windows)

```powershell
# Se ainda não tiver chave
ssh-keygen -t ed25519 -f $env:USERPROFILE\.ssh\hetzner-cdc -N '""'
Get-Content $env:USERPROFILE\.ssh\hetzner-cdc.pub
```

No Hetzner: **Security → SSH keys → Add SSH key** → cole o `.pub`.

---

## Passo 3 — Criar o servidor

**Add Server:**

| Campo | Valor |
|---|---|
| Location | **Falkenstein** (fsn1) |
| Image | **Ubuntu 24.04** |
| Type | **CX32** (4 vCPU, 8 GB, 80 GB) — ou **CX42** (8 vCPU, 16 GB) se quiser folga |
| Networking | IPv4 + IPv6 |
| SSH key | a chave que você cadastrou |
| Name | `cdc-ref` |

Clique **Create & Buy now**. Anote o **IPv4** na tela do servidor.

---

## Passo 4 — Volume de 250 GB (dados)

1. Menu **Volumes → Create Volume**
2. Size: **250 GB**, Location: **mesma do servidor** (Falkenstein)
3. **Attach to server** → `cdc-ref`

---

## Passo 5 — Conectar e montar o volume

```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner-cdc root@IP_DO_SERVIDOR
```

Na VM (como `root`):

```bash
# Descobrir o disco do volume (geralmente /dev/sdb)
lsblk

# Formatar e montar (substitua sdb se for outro)
mkfs.ext4 -F /dev/sdb
mkdir -p /mnt/cdc-data
mount /dev/sdb /mnt/cdc-data
echo '/dev/sdb /mnt/cdc-data ext4 defaults,nofail 0 2' >> /etc/fstab
df -h /mnt/cdc-data
```

---

## Passo 6 — Clonar o repositório

Repositório privado: **https://github.com/Zezoca29/cdc-cnpj**

```bash
# Chave SSH da VM no GitHub (Settings → SSH keys)
ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
# Cole no GitHub, depois:

git clone git@github.com:Zezoca29/cdc-cnpj.git ~/TCC
cd ~/TCC

# Dados pesados no volume (não no disco de boot de 80 GB)
mkdir -p /mnt/cdc-data
ln -sfn /mnt/cdc-data ~/TCC/data

chmod +x infra/cloud-vm/*.sh
./infra/cloud-vm/bootstrap.sh
```

Após `bootstrap`, faça **logout/login** (ou `newgrp docker`) se o Docker pedir permissão.

---

## Passo 7 — Rodar o A11

```bash
cd ~/TCC
tmux new -s a11

./infra/cloud-vm/run-a11.sh 2026-06
```

Desanexar: `Ctrl+B`, depois `D`. Reconectar: `tmux attach -t a11`.

Após terminar 2026-06, libere espaço dos ZIPs:

```bash
rm -rf ~/TCC/data/raw/competencia=2026-06
./infra/cloud-vm/run-a11.sh 2026-07
```

Log: `data/logs/a11-2026-06.log`

---

## Passo 8 — Primeiro evento real + medições

```bash
cd ~/TCC
docker compose up -d
./infra/cloud-vm/run-canonical-pipeline.sh
./infra/cloud-vm/run-benchmark.sh
```

---

## Passo 9 — Trazer resultados para o notebook

```powershell
scp -i $env:USERPROFILE\.ssh\hetzner-cdc -r root@IP_DO_SERVIDOR:~/TCC/data/measurements ./data/
scp -i $env:USERPROFILE\.ssh\hetzner-cdc -r root@IP_DO_SERVIDOR:~/TCC/data/manifests ./data/
```

---

## Firewall (recomendado)

No Hetzner: **Firewalls → Create** → permitir só **SSH (22)** de entrada. Anexe ao servidor `cdc-ref`.

---

## Economia

- **Power off** o servidor quando não estiver em batch (Console → servidor → Power → Shutdown).
- O **volume** continua cobrando (~€10/mês para 250 GB) — isso é normal; seus dados ficam lá.
- Para o TCC, ligue só nas semanas de download/normalização/medição.

---

## GitHub Actions — ligar, rodar batch, desligar

Workflow: [`.github/workflows/hetzner-batch.yml`](../../.github/workflows/hetzner-batch.yml)

O job longo roda **na VM** (tmux). O workflow só liga o servidor, dispara o script e sai; a VM
**desliga sozinha** ao terminar (`run-batch.sh` → `power-off.sh`).

### Secrets (GitHub → Settings → Secrets and variables → Actions)

| Secret | Valor |
|---|---|
| `HETZNER_TOKEN` | API token Read & Write (Hetzner → Security → API tokens) |
| `HETZNER_SERVER_ID` | `163897390` |
| `HETZNER_HOST` | `2.28.28.54` |
| `HETZNER_SSH_KEY` | Conteúdo da chave privada `hetzner-cdc` (notebook) |

### Como usar

1. **Actions** → **Hetzner batch** → **Run workflow**
2. `action`: **run-batch** — liga VM, roda A11, desliga ao terminar
3. `action`: **status** — vê log/processo sem ligar batch
4. `action`: **power-off** — desliga manualmente

Competência atual em execução manual: na VM, `./infra/cloud-vm/schedule-shutdown-when-idle.sh`
agenda desligamento quando o pipeline terminar.

---

## Dashboard em produção (após o pipeline popular o changelog)

Com Postgres + changelog na VM:

```bash
cd ~/TCC   # ou /mnt/cdc-data/TCC
git pull
chmod +x infra/deploy/*.sh

# Uma vez (root): Nginx, Node, systemd, Basic Auth
sudo ./infra/deploy/setup-dashboard.sh
sudo nano /etc/cdc-cnpj/dashboard.env   # IP, senha, CDC_DATA_ROOT

# Cada deploy
./infra/deploy/deploy-dashboard.sh
```

Acesso: `http://2.28.28.54/` (usuário/senha do Basic Auth).

Detalhes: [`infra/deploy/README.md`](../deploy/README.md).
