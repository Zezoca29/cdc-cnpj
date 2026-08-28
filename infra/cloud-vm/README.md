# Máquina de referência na nuvem (A11)

O notebook **não** precisa guardar os ~85 GB/competência. A spec prevê uma **máquina de
referência** com ~250 GB livres; uma VM na nuvem cumpre esse papel.

O notebook continua com: código, testes, fixtures sintéticas (`data/fixtures`) e medições
pequenas sincronizadas de volta (`data/measurements/`).

---

## 1. Provisionar a VM

Requisitos mínimos:

| Recurso | Mínimo | Recomendado |
|---|---|---|
| Disco | 250 GB | 300 GB (margem para raw + Parquet das 2 competências) |
| RAM | 8 GB | 16 GB |
| CPU | 4 vCPU | 4–8 vCPU |
| SO | Ubuntu 22.04/24.04 LTS | |

Provedores que costumam funcionar (ago/2026 — conferir preços):

| Provedor | Exemplo | Observação |
|---|---|---|
| **Hetzner** | CX42 + volume 250 GB | Simples, ~€15–25/mês; desligar quando não usar |
| **Oracle Cloud** | VM.Standard.A1.Flex (ARM) + boot 200 GB | Free tier; ARM roda Java 21 sem problema |
| **Azure** | D4s_v5 + disco Premium 256 GB | Crédito estudante se disponível |

Região: **Brasil** (São Paulo) reduz latência no download da RF/Casa dos Dados.

---

## 2. Bootstrap (uma vez)

Na VM, como usuário com `sudo`:

```bash
# Do notebook: copiar scripts (ajuste user@host)
scp -r infra/cloud-vm user@VM_IP:~/cdc-cloud-vm

# Na VM
chmod +x ~/cdc-cloud-vm/*.sh
REPO_URL=https://github.com/Zezoca29/cdc-cnpj.git INSTALL_DIR=~/TCC ~/cdc-cloud-vm/bootstrap.sh
```

Ou clone direto:

```bash
git clone https://github.com/Zezoca29/cdc-cnpj.git ~/TCC
cd ~/TCC
chmod +x infra/cloud-vm/*.sh
./infra/cloud-vm/bootstrap.sh
```

O bootstrap instala: Java 21, Maven, Docker, Git e valida `docker compose` + `mvn -v`.

---

## 3. Rodar o A11 (download + normalização)

Conecte com `tmux` ou `screen` — o processo leva **horas**:

```bash
cd ~/TCC   # raiz do repositório na VM
tmux new -s a11

./infra/cloud-vm/run-a11.sh 2026-06
# Quando terminar, na mesma sessão ou outra:
./infra/cloud-vm/run-a11.sh 2026-07
```

Logs em `data/logs/a11-YYYY-MM.log`.

**Espaço em disco:** após normalizar cada competência, você pode apagar os ZIPs brutos:

```bash
rm -rf data/raw/competencia=2026-06
```

O Parquet canônico em `data/canonical/` é o que importa daqui em diante.

---

## 4. Marco Fase 3 — primeiro evento real (na VM)

Com `data/canonical/` pronto:

```bash
docker compose up -d
./infra/cloud-vm/run-canonical-pipeline.sh --bucket 0
```

Esperado: eventos no Kafka + `changelog.events` no Postgres.

---

## 5. Medições definitivas (D2)

```bash
./infra/cloud-vm/run-benchmark.sh
```

Saída: `data/measurements/benchmark-definitive.csv`

---

## 6. Trazer resultados para o notebook

Não sincronize `data/canonical/` inteiro (~dezenas de GB). Traga só artefatos:

```powershell
# No notebook (Windows), ajuste VM_IP e caminho
scp -r user@VM_IP:~/TCC/data/measurements ./data/
scp -r user@VM_IP:~/TCC/data/manifests ./data/
# Amostra opcional: um bucket para teste local
scp -r "user@VM_IP:~/TCC/data/canonical/competencia=2026-06/tabela=empresas/bucket=000" ./data/canonical-sample/
```

Ou use `infra/cloud-vm/fetch-artifacts.sh` a partir de uma máquina com `bash` + `scp`.

---

## 7. Segurança

- Exponha apenas **SSH (22)** no firewall da VM.
- **Não** abra Postgres (5434) nem Kafka (9092) para a internet.
- Desligue a VM quando não estiver rodando batch (economia).

---

## 8. Monografia

Registre na seção de ambiente:

- Provedor, região, tipo de VM, tamanho de disco, RAM, CPU
- Que o experimento rodou em **um** ambiente fixo (ameaça à validade já prevista na spec)
- Que notebooks de desenvolvimento usaram apenas dados sintéticos

Ver ADR-007 em [`docs/DECISOES.md`](../../docs/DECISOES.md).
