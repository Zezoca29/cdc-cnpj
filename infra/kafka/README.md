# Kafka local — CDC-CNPJ

Tópicos criados automaticamente pelo serviço `kafka-init` no `docker-compose.yml`:

| Tópico | Entidade | Chave de partição |
|---|---|---|
| `cnpj.empresas` | `empresa.*` | `cnpj_basico` (8 chars) |
| `cnpj.estabelecimentos` | `estabelecimento.*` | `cnpj_basico` |
| `cnpj.dominio` | `dominio.*` | literal `"dominio"` |

**Bootstrap (host):** `localhost:9092`  
**Sem Schema Registry** — payloads JSON com `schema_version` no envelope (CONTRATOS.md §4).

## Smoke test manual

```powershell
docker compose up -d
docker compose ps
docker exec cdc-cnpj-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Esperado: `cnpj.dominio`, `cnpj.empresas`, `cnpj.estabelecimentos`.
