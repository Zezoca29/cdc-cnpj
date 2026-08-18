# ESCOPO VIVO — CDC-CNPJ

**Última atualização:** 2026-08-18 · **Por:** criação inicial do harness
**Semana atual:** S1 (18–24/ago/2026)

> **Este arquivo é obrigatório de atualizar ao concluir qualquer tarefa.** Ele é a resposta
> rápida para "o que já está pronto e o que ainda falta". Se ele estiver desatualizado, o time
> perde a única visão confiável de progresso que tem.
>
> Legenda: `[ ]` pendente · `[~]` em andamento · `[x]` concluído · `[!]` bloqueado

**A regra que governa este arquivo:** nenhum item da seção **Expansão** pode ser iniciado
enquanto houver qualquer item do **Núcleo** fora do estado `[x]`.

---

## Núcleo — semanas 1 a 8 (obrigatório)

Progresso: **0 / 30** concluídos.

### Frente A — Ingestão e normalização

- [ ] **A1** Download da competência 2026-06 (espelho Casa dos Dados; fallback origem oficial)
- [ ] **A2** Download da competência 2026-07
- [ ] **A3** SHA-256 de cada arquivo e `manifesto.json` por competência
- [ ] **A4** Teste de contrato na ingestão (falha ruidosa se o layout mudar)
- [ ] **A5** Parser streaming ZIP→Parquet de **Estabelecimentos** (o mais difícil — começa por ele)
- [ ] **A6** Parser streaming de **Empresas**
- [ ] **A7** Parsers das tabelas de domínio (Cnaes, Municipios, Motivos, Naturezas, Qualificacoes, Paises)
- [ ] **A8** Ponte de município Receita/SIAFI → IBGE via `tabmun.csv` do Tesouro Nacional
- [ ] **A9** Particionamento nos 256 buckets (função congelada — ver R2 no CLAUDE.md)
- [ ] **A10** Contadores de rejeição por arquivo + validação estatística (limite de 0,1%)
- [ ] **A11** Normalização completa das 2 competências, 256 buckets

### Frente B — Motores de diff

- [ ] **B1** Interface `DiffEngine` e o tipo `ChangeRecord`
- [ ] **B2** **E1** — Postgres FULL OUTER JOIN, por bucket (baseline de corretude)
- [ ] **B3** **E3** — hash por registro + comparação paralela por bucket
- [ ] **B4** Preenchimento de `campos_alterados` na classificação de alterações
- [ ] **B5** Teste automatizado de equivalência E1 ≡ E3 (diferença simétrica dos `event_id`)

### Frente C — Eventos, Kafka e consumer

- [ ] **C1** JSON Schema versionado do envelope de evento
- [ ] **C2** Geração de `event_id` determinístico (uuid v5 — ver R6)
- [ ] **C3** Mapeamento `ChangeRecord` → evento tipado (taxonomia de `empresa.*` e `estabelecimento.*`)
- [ ] **C4** Produtor Kafka (key = `cnpj_basico`, tópicos por entidade)
- [ ] **C5** Consumer demo materializando o changelog consultável em Postgres

### Frente D — Bancada e análise

- [ ] **D1** Gerador de dados sintéticos com taxa de mudança controlada (destrava B e C na S2)
- [ ] **D2** Harness de medição (tempo, memória de pico, I/O, throughput, CPU-tempo)
- [ ] **D3** Execuções definitivas: 5 repetições × {E1, E3} × {1, 4, 8, 16 buckets}
- [ ] **D4** Caracterização: registros por tabela, taxa real de mudança, distribuição de buckets
- [ ] **D5** Validação amostral de 200 eventos contra oráculo externo (semente registrada)
- [ ] **D6** Invariantes de sanidade com tolerância declarada
- [ ] **D7** Verificação de representatividade dos 16 buckets frente aos 256

### Transversal

- [ ] **T1** Docker Compose com Postgres e Kafka
- [ ] **T2** Fixtures sintéticas compartilhadas entre as frentes

---

## Expansão — semanas 9 a 11

**BLOQUEADA** até o Núcleo estar 30/30. Ordem de valor é obrigatória: não pule itens.

- [ ] **X1** Execução de escala completa nos 256 buckets — valida **H2** por medição direta
- [ ] **X2** Tabela **Socios** + problema de identidade (P2) + métrica de eventos fantasma
- [ ] **X3** Tabela **Simples**
- [ ] **X4** Quarentena de deletes (P3)

---

## Fora de escopo — trabalhos futuros

Estes itens **não são backlog**. Trazer qualquer um para o Núcleo exige ADR em
`docs/DECISOES.md` dizendo explicitamente **o que sai em troca**. O orçamento de horas é fixo
em 468 h e não é expansível.

- Motor **E2** (sort-merge externo)
- Apache Spark
- Schema Registry e Avro
- Retenção longa e replay histórico no Kafka
- Séries com mais de duas competências e análise sazonal

---

## Marcos e critérios de corte

| Data | Marco | Critério de corte se não atingido |
|---|---|---|
| 07/set (fim S3) | Um bucket ponta a ponta com dado sintético | Revisar divisão de frentes |
| **21/set (fim S5)** | **Primeiro evento REAL no Kafka** | **Cortar para uma única entidade (Estabelecimentos)** |
| 05/out (fim S7) | E1 ≡ E3 com diferença simétrica zero | Bloqueia S8 — não avança sem resolver |
| **12/out (fim S8)** | **CONGELAMENTO DE ESCOPO** | **O que não roda vira trabalho futuro. Sem exceção.** |
| **02/nov (fim S11)** | **Fim do código** | **Só escrita e defesa a partir daqui** |
| 16/nov (fim S13) | Entrega | — |

---

## Histórico de alterações deste arquivo

| Data | Quem | O que mudou |
|---|---|---|
| 2026-08-18 | — | Criação a partir da spec v1.0 |
