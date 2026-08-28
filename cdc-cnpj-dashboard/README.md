# CDC-CNPJ Dashboard

Frontend React (Vite) baseado no template **Dashboard de Explorador de Eventos**. Integra:

| Fonte | Telas |
|-------|--------|
| Postgres `changelog.events` | Explorador, Overview, Entidade |
| `data/measurements/*.csv` | Benchmarks (E1 vs E3) |
| `data/manifests/*/quality.json` | Qualidade (rejeição, buckets) |
| `data/manifests/*/manifesto.json` | SHA-256 por ZIP |
| `data/measurements/pipeline-latest.json` | Quarentena, Overview |
| Tamanho Parquet + `pg_total_relation_size` | Overview, Qualidade |

## Subir a stack

```bash
# 1. Infra
docker compose up -d

# 2. API (porta 8080) — executar na raiz do repo
mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.api.ChangelogApiCli

# 3. Frontend
cd cdc-cnpj-dashboard
npm install
npm run dev
```

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/health` | Health check |
| GET | `/api/stats` | Changelog + `byStrategy` |
| GET | `/api/events` | Lista paginada |
| GET | `/api/events/by-entity?q=` | Timeline por CNPJ |
| GET | `/api/manifests` | Lista competências |
| GET | `/api/manifests?competencia=` | Manifesto completo |
| GET | `/api/quality?competencia=` | Relatório de ingestão |
| GET | `/api/benchmarks` | Resumo CSV + full-scale |
| GET | `/api/pipeline/latest` | Último pipeline |
| GET | `/api/storage?competencia=` | Parquet vs changelog |

## Artefatos gerados automaticamente

- **quality.json** — escrito por `CompetenciaPipeline` / `NormalizeCli` após normalização
- **pipeline-latest.json** — escrito por `CanonicalPipelineCli` após diff/Kafka

Sincronize da VM com:

```bash
scp -r user@VM:~/TCC/data/measurements ./data/
scp -r user@VM:~/TCC/data/manifests ./data/
```
