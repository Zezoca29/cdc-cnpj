# STATUS — CDC-CNPJ

**Última atualização:** 2026-08-27 · **Semana:** S1 de 13 · **Dias até a entrega:** 81

> **Atualize este arquivo ao fim de cada sessão de trabalho.** Não no fim da semana, não na
> véspera do marco: ao fim da sessão. Uma linha honesta hoje vale mais que um relatório
> completo depois que o problema já custou caro.
>
> Se você trabalhou e não atualizou aqui, para as outras três frentes o trabalho não existiu.

---

## Semáforo do projeto

| Frente | Estado | Comentário |
|---|---|---|
| **A** — Ingestão e normalização | 🟢 no prazo | Pipeline A11 pronto (`IngestionPipelineCli`); execução real ~85 GB/competência |
| **B** — Motores de diff | 🟢 no prazo | E1 + E3 validados; `EquivalenceIT` confirma mesmos `event_id` nas fixtures sintéticas |
| **C** — Eventos e Kafka | 🟢 no prazo | Módulo `cdc-cnpj-events`: schema, mapper, produtor Kafka, consumer Postgres; IT E1→Kafka→changelog |
| **D** — Bancada e análise | 🟢 no prazo | X1 pronto (`FullScaleCli`); H2 validado após execução em `data/canonical/` |
| **Monografia** | 🟢 no prazo | Rascunho de Introdução e Metodologia pronto na S1 (Atividade 01) — prazo era S3 |

Legenda: 🟢 no prazo · 🟡 atenção · 🔴 em risco · ⚪ não iniciada

---

## Semana atual — S1 (18–24/ago/2026)

**Tema:** semana conjunta. Ninguém trabalha isolado; o objetivo é congelar os contratos para
que a S2 possa correr em paralelo real.

**Objetivo da semana:**
- [x] Congelar em `docs/CONTRATOS.md`: schema Parquet canônico, envelope de evento, função de partição, `ChangeRecord`, manifesto
- [~] Ler `cnpj-metadados.pdf` (layout oficial) e o ETL do `cuducos/minha-receita` — schemas derivados de layout público + `cnpj-metadados.pdf`; validação linha-a-linha pendente na ingestão
- [x] Iniciar o download das competências 2026-06 e 2026-07 (roda em segundo plano a semana toda)
- [x] Subir o Docker Compose com Postgres e Kafka
- [ ] Definir quem assume cada frente (A, B, C, D)
- [x] Gerador sintético (D1) — `cdc-cnpj-bench`; materializar com `mvn -pl cdc-cnpj-bench exec:java`

**Marco da semana:** contratos v1 congelados + fixtures sintéticas disponíveis. **Concluído** (gerador + E1 sobre fixtures; Parquet local em `data/fixtures/`).

---

## Bloqueios ativos

| # | Bloqueio | Frente | Desde | Quem destrava | Ação |
|---|---|---|---|---|---|
| — | nenhum registrado | — | — | — | — |

> Bloqueio parado há mais de 2 dias vira pauta obrigatória. Com 9 h/semana por pessoa,
> 2 dias travado é quase uma semana perdida.

---

## Diário de bordo

Entrada mais recente no topo. Formato: data · quem · frente · o que avançou · o que travou ·
o que a próxima pessoa precisa saber.

### 2026-08-27 · agente · Expansão X1

**Escala completa 256 buckets:**

- `FullScaleDiffBenchmark` / `FullScaleCli` — diff E3 (ou E1) nos 256 buckets, CSV + validação H2 (&lt; 24 h).
- `H2Validator` — critério de aceitação da spec §4.

**Próximo:** X2 (Sócios + P2) ou execução real do pipeline + `FullScaleCli` sobre dado canônico.

### 2026-08-27 · agente · Expansão X2

**Sócios + P2 + eventos fantasma (ADR-004):**

- `SociosParser` + schema Parquet `socios` com `socio_identity_key` (particionado por `cnpj_basico`).
- `SocioIdentityResolver` — chave estável por empresa + identificador + nome normalizado (sem CPF mascarado).
- `SocioNaiveDiff` + `GhostEventAnalyzer` — mede pares fantasma `excluido`+`incluido` do diff ingênuo.
- Eventos: tópico `cnpj.socios`, tipos `socio.incluido|excluido|alterado`.
- Catálogo expansão: `CnpjExpansionCatalog.X2_SOCIOS_FILES` (`Socios0–9.zip`).

**Próximo:** X3 (Simples) ou download Socios + normalização na máquina de referência.

### 2026-08-27 · agente · Expansão X3

**Simples Nacional (ADR-005):**

- `SimplesParser` + schema Parquet `simples` (chave `cnpj_basico`, 7 colunas CSV RF).
- `CnpjExpansionCatalog.X3_SIMPLES_FILES` — `Simples.zip`.
- Diff E1/E3 e eventos `simples.opcao_alterada` no tópico `cnpj.simples`.

**Próximo:** X4 (quarentena de deletes, P3).

### 2026-08-27 · agente · Expansão X4

**Quarentena de deletes (ADR-006, P3):**

- `DeleteQuarantine` — retém `DELETE` antes do Kafka; `INSERT` cancela ressurreição.
- `QuarantineEventPublisher` — publica com `acceptAndFlush` ao final do lote.
- Fast-path: `estabelecimentos` com `situacao_cadastral=08` confirma imediatamente.
- `QuarantineResult` — métricas `quarantined / cancelled / confirmed`.

**Expansão 4/4 concluída.** Próximo foco: execução real do pipeline + medições definitivas.

### 2026-08-27 · agente · Marco E2E sintético

**Bucket 000 ponta a ponta — validado:**

- `SyntheticPipelineCli` / `SyntheticPipelineRunner` — fixtures → diff (E1/E3) → quarentena → Kafka → changelog.
- `KafkaTopicProvisioner` + `ChangelogConsumer.awaitAssignment` — corrige falha de assignment na 1ª execução.
- Postgres no host: porta **5434** (`127.0.0.1`, evita conflito com Postgres local + IPv6 no Windows).
- CLI trunca `changelog.events` antes do marco (event_ids determinísticos).
- **E3 e E1** validados: `diff_changes=9 events_published=9 changelog_inserted=9`.

**Próximo:** `IngestionPipelineCli` na máquina de referência + `CanonicalPipelineCli` para primeiro evento real.

### 2026-08-28 · agente · Marco Fase 3 (prep)

**Pipeline canônico real:**

- `EventPipelineRunner` — generaliza diff → quarentena → Kafka (N buckets, N tabelas).
- `CanonicalPipelineCli` — aponta para `data/canonical/`; flags `--bucket`, `--all-buckets`, `--tables`, `--diff-only`.
- `IngestionPipelineCli --competencia YYYY-MM` — ingestão incremental por competência.
- Dry-run validado com `--canonical-root data/fixtures` (9 mudanças, bucket 0).

**Próximo:** aguardar download 2026-06 (~85 GB); depois `IngestionPipelineCli --competencia 2026-06 --skip-download` + `CanonicalPipelineCli`.

### 2026-08-28 · agente · VM na nuvem (ADR-007)

**Máquina de referência sem encher o notebook:**

- `infra/cloud-vm/` — README, `bootstrap.sh`, `run-a11.sh`, `run-canonical-pipeline.sh`, `run-benchmark.sh`, `fetch-artifacts.sh`.
- ADR-007: VM Linux ~250 GB para A11 + medições; notebook fica com fixtures e artefatos sincronizados.
- Download local 2026-06 **cancelado** (~4 GB parciais removidos).

**Próximo:** provisionar VM, rodar `bootstrap.sh` + `run-a11.sh 2026-06` em `tmux`.

### 2026-08-28 · agente · A11 prep (tabmun + download)

- `TabmunDownloader` / `TabmunDownloadCli` — `tabmun.csv` do CKAN STN baixado (436 KB).
- `IngestionPipelineCli` baixa tabmun automaticamente (`--skip-tabmun` para pular).
- `pom.xml` ingestion: removido `mainClass` fixo do exec plugin.
- Download competência **2026-06** iniciado em segundo plano (`DownloadCli`).

**Próximo:** monitorar `data/logs/download-2026-06.log`; normalizar após conclusão.

### 2026-08-27 · agente · Frente A (A11) — Núcleo 30/30

**Pipeline completo e ADR-002:**

- `IngestionPipelineCli` — download + normalização + validação 256 buckets para 2026-06/07.
- `BucketMaterializer` — materializa buckets vazios (ADR-002); merge de Empresas0–9/Estabelecimentos0–9 num writer único.
- Correção: múltiplos ZIPs da mesma tabela não sobrescrevem mais o Parquet.

**Executar A11 na máquina de referência:**

```powershell
mvn -pl cdc-cnpj-ingestion exec:java -Dexec.mainClass=br.univesp.tcc.cdccnpj.ingestion.pipeline.IngestionPipelineCli
```

### 2026-08-27 · agente · Frente D (D3–D7) + A11 prep

**Análise e validação:**

- `BenchmarkCli --protocol definitive` — matriz 5×{E1,E3}×{1,4,8,16} + mediana/IQR/speedup (`BenchmarkAnalyzer`).
- `AnalysisCli` — modos `characterize`, `sanity`, `representativeness`, `sample`, `summarize` (D4–D7).
- `NormalizationValidator` + flag `--validate` no `NormalizeCli`.

**Próximo:** único item do Núcleo — A11 (download + normalização real das 2 competências, 256 buckets).

### 2026-08-27 · agente · Frente D (D2) + Frente A (NormalizeCli)

**Harness de medição (D2):**

- `DiffMeasurementHarness` / `BenchmarkMatrix` — tempo de parede, CPU, heap/RSS, bytes lidos, throughput.
- Micrometer `Timer` + exportação CSV em `data/measurements/benchmark.csv`.
- `BenchmarkCli` no módulo `cdc-cnpj-diff` (evita ciclo Maven bench↔diff).
- `NormalizeCli` — orquestra normalização por competência (prepara A11).

**Próximo:** download real + normalização A11; matriz experimental D3 (5×{E1,E3}×{1,4,8,16}).

### 2026-08-27 · agente · Frente A (A1–A2)

**Downloader de competências:**

- `CompetenciaDownloader` + `CasaDosDadosSource` — streaming HTTP→disco para os 22 ZIPs do Núcleo; pula arquivos já presentes.
- `competencia-snapshots.properties` — mapeia competência lógica (2026-06/07) → pasta de snapshot no espelho (`2026-05-10`, `2026-05-14`).
- `DownloadCli` — `mvn -pl cdc-cnpj-ingestion exec:java -Dexec.args="--competencia 2026-06"`.
- Testes com `HttpServer` local (sem download real em CI).

**Próximo:** executar download real (~85 GB por competência) e normalização completa (A11).

### 2026-08-27 · agente · Frente A (A3–A4, A9–A10)

**Orquestração e manifesto:**

- `ManifestBuilder` / `ManifestIo` / `Sha256Hasher` — manifesto.json §6 com validação SHA-256 antes do parse.
- `NormalizationRunner` — processa competência inteira (empresas, estabelecimentos, domínio) com relatório por arquivo.
- `LayoutContractTest` — rejeição ruidosa quando layout CSV diverge do contrato.

**Próximo:** download competências (A1–A2) e normalização real (A11).

### 2026-08-27 · agente · Frente A (A8)

**Ponte tabmun concluída:**

- `TabmunLoader` / `TabmunIndex` — carrega `tabmun.csv` (SIAFI→IBGE, layout `CO_MUNICIPIO;…;CO_IBGE`).
- `EstabelecimentosParser` recebe `TabmunIndex` e preenche `municipio_ibge`; contador `municipio_ibge_nao_resolvido` preservado.

**Próximo:** download (A1–A3) ou orquestração de normalização (A9–A11).

### 2026-08-27 · agente · Frente A (A7)

**Parsers de domínio concluídos:**

- `DominioParser` + `DominioTabela` — layout comum `codigo;descricao` para as 6 tabelas.
- `FlatParquetWriter` — Parquet sem bucket (`tabela=cnaes/part-0.parquet`, etc.).
- `DominioParserTest` — CSV, ZIP, aspas, mapeamento de nomes de ZIP.

**Próximo:** ponte tabmun (A8) ou download (A1–A3).

### 2026-08-27 · agente · Frente A (A6)

**Parser Empresas concluído:**

- `EmpresasParser` — 7 colunas CSV → Parquet canônico, 256 buckets, limite 0,1% rejeição.
- `ParseRequest` extraído e compartilhado com Estabelecimentos.
- `EmpresasParserTest` — CSV, ZIP, aspas com `;`, round-trip Parquet.

**Próximo:** parsers de domínio (A7) ou download (A1–A3).

### 2026-08-27 · agente · Frente A (A5)

**Módulo `cdc-cnpj-ingestion` criado:**

- `CsvLineParser` — separador `;`, aspas RFC-style, Latin-1→UTF-8.
- `EstabelecimentosParser` — streaming ZIP/CSV→Parquet com 256 buckets, proveniência, contadores e falha se rejeição > 0,1%.
- `BucketedParquetWriter` + schema canônico explícito; `municipio_ibge` nulo (ponte A8 pendente).
- Testes: `CsvLineParserTest`, `EstabelecimentosParserTest` (round-trip Parquet via `CanonicalParquetReader`).

**Próximo:** download competências (A1–A3) ou parser Empresas (A6).

### 2026-08-27 · agente · Frente B (B3–B5)

**Motor E3 concluído:**

- `HashDiffEngine` — diff em memória por bucket com fingerprint murmur3_128; `RecordDiffSupport` compartilhado com E1.
- `HashDiffEngineIT` — 3+6 mudanças no bucket 000 (sem Postgres).
- `EquivalenceIT` (em `cdc-cnpj-events`) — conjuntos de `event_id` idênticos entre E1 e E3 nas fixtures.

**Próximo:** parser Estabelecimentos (A5) ou ingestão completa (A1–A4).

### 2026-08-27 · agente · Frente C (Fase 2 Task 5)

**Módulo `cdc-cnpj-events` concluído:**

- JSON Schema `event-envelope-1.0.json`; `EventTypeResolver` com precedência §4.3; `EventMapper` → envelope + `event_id` via `EventIdGenerator`.
- `KafkaEventProducer` (key `cnpj_basico`, tópicos `cnpj.*`); `ChangelogConsumer` idempotente em `changelog.events`.
- Testes unitários + `EventsPipelineIT` (E1 → Kafka → Postgres; skip se Docker não estiver up).

**Próximo:** parser Estabelecimentos (A5) ou motor E3 (B3).

### 2026-08-27 · agente · Frentes B/D/transversal (Fase 1–2)

**Tasks 2–4 avançadas:**

- **Docker Compose (T1):** Postgres 16 + Kafka KRaft; tópicos `cnpj.*` criados via `kafka-init` (listener interno `kafka:9094`).
- **Gerador sintético (D1/T2):** `cdc-cnpj-bench` — 9 mudanças documentadas; Parquet em `data/fixtures/` via `mvn -pl cdc-cnpj-bench exec:java`.
- **E1 (B2):** `PostgresDiffEngine` — FULL OUTER JOIN por bucket; IT confirma 3 mudanças em empresas + 6 em estabelecimentos no bucket 000.

**Próximo:** Fase 2 Task 5 — produtor Kafka + consumer (frente C).

### 2026-08-27 · agente · Frente B / common

**Task 1 (Fase 1) concluída:** módulo `cdc-cnpj-common` com `CnpjPartitioner` (Guava murmur3_32),
`CnpjValidator` (DV formato misto), `EventIdGenerator` (uuid v5), `ChangeRecord`, `DiffEngine`.
17 testes passando (`mvn -pl cdc-cnpj-common test`). Vetores de partição em
`partition-test-vectors.json`.

**Próximo:** Task 2 Docker Compose (T1) e Task 3 gerador sintético (D1).

### 2026-08-27 · agente · transversal (Fase 0)

**Contratos v1 congelados** em [`docs/CONTRATOS.md`](CONTRATOS.md) (`ESTADO_CONTRATO: CONGELADO`):

- Schemas Parquet completos: `empresas`, `estabelecimentos`, 6 tabelas de domínio (§3.5)
- Encoding ISO-8859-1, separador `;`, regras de data sentinela
- Taxonomia de eventos com grupos de precedência (§4.3)
- `ChangeRecord` + assinatura `DiffEngine` (§5)
- Formato `manifesto.json` (§6)
- 16 buckets experimentais em [`docs/experiment-buckets.txt`](experiment-buckets.txt)

**ADR-003** (título da monografia) marcado **aceito** — objeções até 31/ago.

**Pendências humanas da S1:** download das competências, Docker Compose, atribuição de frentes,
leitura das 8 obras (Atividade 01), gerador sintético (D1).

**Próximo passo técnico:** Fase 1 do plano mestre — scaffold Maven + `cdc-cnpj-common` + Docker + D1.

### 2026-08-18 · — · monografia

**Atividade 01 (primeira entrega do TCC) montada** em
[`docs/entregas/01-atividade-01.md`](entregas/01-atividade-01.md): título proposto,
8 palavras-chave com strings de busca prontas para o Google Acadêmico, fichamento de 8 obras
(referência ABNT, metodologia, arquitetura e limitações de cada uma) e os textos de **Introdução**
e **Metodologia**, derivados da spec §1–§3 e §9–§11.

Efeito de cronograma: Introdução e Metodologia tinham prazo **S3** e saem em **S1**. Isso compra
folga real na semana em que a frente A estiver brigando com o parser.

Gerada também a versão **`.docx`** ao lado do `.md` (A4, margens ABNT 3/2/3/2 cm, Arial 12,
entrelinha 1,5). **A fonte da verdade continua sendo o `.md`** — o `.docx` é derivado e precisa ser
regerado a cada alteração, senão os dois divergem em silêncio.

**Nenhum item do ESCOPO mudou de estado** — o ESCOPO rastreia o Núcleo técnico, e a escrita da
monografia é rastreada na tabela do fim deste arquivo. Registrado assim de propósito, para não
inflar o ESCOPO com itens que não são código.

**O que a próxima pessoa precisa saber.** As 8 obras estão **fichadas, não lidas**. A spec §15 é
explícita: não se cita o que não foi lido. Antes de a Atividade 01 ser submetida, cada integrante
precisa ler 2 obras e confirmar (ou corrigir) os campos *metodologia*, *arquitetura* e *limitações*
da respectiva ficha. As fichas trazem um campo *Estado da leitura* justamente para isso.

O título foi para **ADR-003, estado `proposto`** — precisa da ratificação das 4 frentes até 24/ago.
Enquanto ele não for aceito, não use o título em nenhum outro artefato.

### 2026-08-18 · — · setup

Repositório criado: **https://github.com/Zezoca29/cdc-cnpj** (privado, branch `main`).
Vira público na defesa, conforme decidido.

**Colaboradores.** Convite de escrita enviado a `luizlevindiniz` (Luiz Felipe Levin Diniz) em
18/08 — **pendente de aceitação**; ele precisa aceitar em github.com/notifications antes de
conseguir clonar. Faltam ainda 2 integrantes: sem convite, ninguém mais acessa o repositório.

`.gitignore` bloqueia `data/`, `*.zip`, `*.parquet` e `*.csv`: os 85 GB **nunca** entram no
repositório. O que garante reprodutibilidade é o `manifesto.json` (nome, tamanho, SHA-256,
origem), que é versionado de propósito. Licença MIT — o titular está como
"CDC-CNPJ contributors"; ajustar quando a equipe estiver definida.

Criação do harness de projeto a partir da spec v1.0. Estrutura de documentos, checklists e
verificação automatizada no lugar.

**Próximo passo crítico:** iniciar o download das duas competências ainda nesta semana. É a
tarefa com maior tempo de espera e pré-requisito de tudo — se atrasar, atrasa o projeto
inteiro, e o plano B (reduzir para uma única família de arquivos) precisa ser acionado ainda
na S2.

---

## Registro de marcos

| Marco | Data-alvo | Estado | Data real |
|---|---|---|---|
| Contratos v1 + fixtures | 24/ago | 🟢 concluído | 2026-08-27 |
| Um bucket ponta a ponta (sintético) | 07/set | 🟢 concluído | 2026-08-27 — E1/E3, 9 eventos no changelog |
| **Primeiro evento REAL no Kafka** | **21/set** | 🟡 ferramenta pronta | `CanonicalPipelineCli`; aguarda `data/canonical/` |
| E1 ≡ E3, diferença simétrica zero | 05/out | ⚪ pendente | — |
| **CONGELAMENTO DE ESCOPO** | **12/out** | ⚪ pendente | — |
| **Fim do código** | **02/nov** | ⚪ pendente | — |
| Entrega | 16/nov | ⚪ pendente | — |

---

## Progresso da escrita da monografia

A escrita corre em paralelo desde a S1. Cada marco técnico fecha com sua seção redigida — é
isto que evita o modo de falha mais comum em TCC.

| Seção | Origem | Prazo | Estado |
|---|---|---|---|
| Introdução e contexto | spec §1–2 | S3 | 🟡 rascunho pronto (Atividade 01, S1) |
| Metodologia | spec §9 | S3 | 🟡 rascunho pronto (Atividade 01, S1) |
| Arquitetura | spec §6–7 | S5 | ⚪ |
| Corretude e validação | spec §10 | S7 | ⚪ |
| Resultados | medições da S8 | S8 | ⚪ |
| Trabalhos relacionados | spec §15 | S9 | ⚪ |
| Ameaças à validade | spec §14 | S10 | ⚪ |
| Conclusão e trabalhos futuros | — | S12 | ⚪ |
