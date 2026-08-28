# Infraestrutura local

**Máquina de referência (A11, ~250 GB):** ver [`cloud-vm/README.md`](cloud-vm/README.md) — VM na nuvem para download e normalização sem encher o notebook.

```powershell
# Na raiz do repositório
docker compose up -d
docker compose ps
```

| Serviço | Porta | Credenciais |
|---|---|---|
| PostgreSQL 16 | 5434 (host, use `127.0.0.1` in JDBC on Windows) → 5432 (container) | user `cdc` / password `cdc` / db `cdc_cnpj` |
| Kafka (KRaft) | 9092 | PLAINTEXT, bootstrap `localhost:9092` |

Volumes Docker nomeados (`pgdata`) persistem entre reinícios. Para reset:

```powershell
docker compose down -v
```

Ver também [`kafka/README.md`](kafka/README.md) e [`postgres/init.sql`](postgres/init.sql).

## Download de competências (A1–A2)

O pipeline baixa ZIPs do espelho Casa dos Dados (plano A da spec). A pasta no CDN usa a **data de publicação** da RF (`YYYY-MM-DD`), não a competência lógica do TCC.

Mapeamento em `cdc-cnpj-ingestion/src/main/resources/competencia-snapshots.properties`:

| Competência | Snapshot no espelho |
|---|---|
| 2026-06 | 2026-05-10 |
| 2026-07 | 2026-05-14 |

```powershell
# Na raiz do repositório (~85 GB por competência; pode rodar em segundo plano)
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.args="--competencia 2026-06"
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.args="--competencia 2026-07"

# Ou com data de snapshot explícita:
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.args="--competencia 2026-06 --snapshot 2026-05-10"
```

Saída: `data/raw/competencia=YYYY-MM/*.zip` + `data/manifests/competencia=YYYY-MM/manifesto.json`.

Fallback manual (origem oficial RF): `https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9` — coloque os ZIPs em `data/raw/` e rode o manifesto via `ManifestBuilder` ou reexecute o CLI (arquivos existentes são ignorados).

## Normalização (A11)

```powershell
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizeCli -Dexec.args="--competencia 2026-06"
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.normalize.NormalizeCli -Dexec.args="--competencia 2026-07"
```

Saída: `data/canonical/competencia=YYYY-MM/tabela=*/bucket=*/part-0.parquet`.

## Marco S2–S3 — bucket sintético ponta a ponta

```powershell
# Diff + quarentena apenas (sem Kafka; motor E3, sem Postgres)
mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.SyntheticPipelineCli -Dexec.args="--diff-only --rewrite-fixtures"

# Pipeline completo: fixtures → E1 → quarentena → Kafka → changelog
docker compose up -d
# Recompile dependências se alterou cdc-cnpj-diff (E1):
mvn -pl cdc-cnpj-diff,cdc-cnpj-events -am install -DskipTests
mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.SyntheticPipelineCli -Dexec.args="--engine E1 --rewrite-fixtures"
```

JDBC: `jdbc:postgresql://127.0.0.1:5434/cdc_cnpj` (porta 5434 evita conflito com Postgres local).

Esperado: **9** mudanças no bucket `000`, publicadas no changelog com idempotência.

## Marco Fase 3 — primeiro evento REAL no Kafka (21/set)

Pré-requisito: `data/canonical/` materializado pelo A11.

```powershell
# 1) Ingestão (máquina de referência; ~85 GB/competência)
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli
# Uma competência por vez:
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli -Dexec.args="--competencia 2026-06 --skip-download"

# 2) Diff de um bucket (dry-run, sem Kafka)
mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.CanonicalPipelineCli -Dexec.args="--bucket 0 --diff-only"

# 3) Publicar no Kafka + changelog (Plano B: só estabelecimentos)
docker compose up -d
mvn -pl cdc-cnpj-diff,cdc-cnpj-events -am install -DskipTests
mvn -pl cdc-cnpj-events exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.events.pipeline.CanonicalPipelineCli -Dexec.args="--engine E3 --bucket 0 --tables estabelecimentos"
```

Dry-run com fixtures sintéticas: `--canonical-root data/fixtures` (após `SyntheticPipelineCli --rewrite-fixtures`).

## Pipeline completo A11

```powershell
# tabmun (~425 KiB; automático no IngestionPipelineCli)
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.municipio.TabmunDownloadCli

# Download + normalização + validação 256 buckets (2026-06 e 2026-07)
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli

# Pular download se ZIPs já existem:
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli -Dexec.args="--skip-download"
```

Pré-requisito: `tabmun.csv` é baixado automaticamente pelo pipeline (ou via `TabmunDownloadCli`).

## Harness de medição (D2)

```powershell
# Gera fixtures sintéticas (se ainda não existirem)
mvn -pl cdc-cnpj-bench exec:java

# Medição rápida em fixtures (E3, 1 bucket)
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCli

# Matriz completa do protocolo (requer Postgres para E1)
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCli -Dexec.args="--engines E1,E3 --bucket-scales 1,4,8,16 --repetitions 5"
```

CSV em `data/measurements/benchmark.csv` (coluna `warmup=true` para a 1ª repetição descartada na análise).

```powershell
# Protocolo definitivo (requer data/canonical + Postgres para E1)
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.BenchmarkCli -Dexec.args="--protocol definitive"

# Caracterização, invariantes, representatividade, amostra externa
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.analysis.AnalysisCli -Dexec.args="--mode characterize"
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.analysis.AnalysisCli -Dexec.args="--mode sanity"
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.analysis.AnalysisCli -Dexec.args="--mode representativeness"
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.analysis.AnalysisCli -Dexec.args="--mode sample --seed 20260827"
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.analysis.AnalysisCli -Dexec.args="--mode summarize --input data/measurements/benchmark-definitive.csv"
```

## Expansão X1 — escala completa (256 buckets)

```powershell
# Diff E3 nos 256 buckets + validação H2 (< 24 h)
mvn -pl cdc-cnpj-diff exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.bench.harness.FullScaleCli

# Saída: data/measurements/full-scale-256.csv + full-scale-256-detail.csv
```

## Expansão X2 — Sócios e eventos fantasma (P2)

Incluir `Socios0.zip`…`Socios9.zip` no manifesto (`CnpjExpansionCatalog.X2_SOCIOS_FILES`) além dos arquivos do Núcleo. A normalização detecta `socios` automaticamente quando os ZIPs estão presentes.

```powershell
# Após normalizar duas competências com Socios no manifesto:
# Diff ingênuo (baseline P2) — use SocioNaiveDiff em testes/código
# Diff com identidade — HashDiffEngine/E1 com tabela "socios"
# Métrica de fantasmas — GhostEventAnalyzer.analyze(naiveChanges)
```

Classes principais: `SocioIdentityResolver`, `SociosParser`, `SocioNaiveDiff`, `GhostEventAnalyzer` (ADR-004 em `docs/DECISOES.md`).

## Expansão X3 — Simples Nacional

Incluir `Simples.zip` no manifesto (`CnpjExpansionCatalog.X3_SIMPLES_FILES`) além dos arquivos do Núcleo. Particionado por `cnpj_basico` como empresas.

```powershell
# Após normalizar duas competências com Simples.zip no manifesto:
# Diff — HashDiffEngine/E1 com tabela "simples"
# Eventos — tipo simples.opcao_alterada no tópico cnpj.simples
```

Classes principais: `SimplesParser` (ADR-005 em `docs/DECISOES.md`).

## Expansão X4 — Quarentena de deletes (P3)

Deletes do diff não vão direto ao Kafka. Use `QuarantineEventPublisher` ou `DeleteQuarantine` manualmente:

```powershell
# No código de publicação:
# 1. quarantine.acceptForPublishing(changes) — por bucket/tabela
# 2. quarantine.flushConfirmed() — ao final do pipeline
# Ou atalho: QuarantineEventPublisher.publish(producer, changes, "E3", runId)
```

Fast-path: `estabelecimento` com `situacao_cadastral=08` no `before` publica imediatamente.
Demais deletes aguardam confirmação no `flush` (ou cancelamento por `INSERT` subsequente).

Classes: `DeleteQuarantine`, `QuarantineResult`, `QuarantineEventPublisher` (ADR-006).
