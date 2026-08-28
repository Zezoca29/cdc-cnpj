# Deploy do dashboard em produção (VM Hetzner)

Pacote mínimo: **API em systemd** (localhost) + **Nginx** (público) + **Basic Auth** opcional.

## Pré-requisitos na VM

- `bootstrap.sh` já executado (Java 21, Maven, Docker)
- Postgres com changelog populado (`docker compose up -d` + pipeline de eventos)
- `data/manifests/` e `data/measurements/` no `CDC_DATA_ROOT`

## 1. Instalação única (root)

```bash
cd ~/TCC   # ou /mnt/cdc-data/TCC
git pull
chmod +x infra/deploy/*.sh
sudo ./infra/deploy/setup-dashboard.sh
```

Edite `/etc/cdc-cnpj/dashboard.env`:

```bash
sudo nano /etc/cdc-cnpj/dashboard.env
```

Defina pelo menos:

```env
CDC_PROJECT_ROOT=/mnt/cdc-data/TCC
CDC_DATA_ROOT=/mnt/cdc-data/TCC/data
DASHBOARD_SERVER_NAME=2.28.28.54
DASHBOARD_AUTH_USER=admin
DASHBOARD_AUTH_PASSWORD=sua-senha-forte
```

Se mudar auth depois do setup, rode `sudo ./infra/deploy/setup-dashboard.sh` de novo ou crie manualmente:

```bash
sudo htpasswd -c /etc/nginx/.htpasswd-cdc-cnpj admin
```

## 2. Deploy (cada atualização de código)

```bash
cd ~/TCC
git pull
./infra/deploy/deploy-dashboard.sh
```

Isso:

1. Sobe Postgres/Kafka (`docker compose up -d`)
2. Gera `cdc-cnpj-events-*-dashboard.jar`
3. Roda `npm run build` do frontend
4. Copia JAR → `/opt/cdc-cnpj/dashboard-api.jar`
5. Copia `dist/` → `/var/www/cdc-cnpj-dashboard/`
6. Reinicia `cdc-cnpj-dashboard-api` e Nginx

## 3. Verificação

```bash
systemctl status cdc-cnpj-dashboard-api nginx
curl -s http://127.0.0.1:8080/api/health
curl -s -u admin:SENHA http://127.0.0.1/api/stats | head
```

No navegador: `http://IP_DA_VM/` (pedirá usuário/senha se Basic Auth estiver ativo).

## 4. HTTPS (opcional, com domínio)

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d dashboard.seudominio.tld
```

Descomente o bloco `server { listen 443 ...}` em `infra/deploy/nginx/cdc-cnpj-dashboard.conf` antes de um setup customizado, ou deixe o certbot reescrever o site.

## 5. Variáveis de ambiente (API)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `CDC_PROJECT_ROOT` | `.` | Raiz do repo |
| `CDC_DATA_ROOT` | `$PROJECT/data` | Manifests, measurements, canonical |
| `CDC_API_BIND` | `127.0.0.1` | Não exponha a API diretamente |
| `CDC_API_PORT` | `8080` | Porta interna |
| `CDC_JDBC_URL` | `jdbc:postgresql://127.0.0.1:5434/cdc_cnpj` | Postgres |
| `CDC_JDBC_USER` / `CDC_JDBC_PASSWORD` | `cdc` / `cdc` | Credenciais |
| `CDC_CORS_ORIGIN` | (vazio) | Mesma origem via Nginx |

## 6. Segurança

- Postgres (`5434`) e Kafka (`9092`) **não** devem estar abertos no firewall público — só `80`/`443`.
- O changelog contém dados cadastrais; use Basic Auth ou VPN.
- Troque `CDC_JDBC_PASSWORD` em produção real e atualize `docker-compose.yml` + `.env` de forma consistente.

## 7. Desenvolvimento local (sem Nginx)

```bash
docker compose up -d
mvn -pl cdc-cnpj-events -am package -DskipTests
java -jar cdc-cnpj-events/target/cdc-cnpj-events-*-dashboard.jar
cd cdc-cnpj-dashboard && npm run dev
```
