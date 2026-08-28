# CONTRATOS CONGELADOS — CDC-CNPJ

**Versão:** 1.0 · **Congelamento:** 2026-08-27 (recuperação S1)
`ESTADO_CONTRATO: CONGELADO`

Alterar qualquer coisa aqui exige ADR em [`DECISOES.md`](DECISOES.md) aprovado pelas **quatro frentes**.

> Este é o documento **normativo** dos contratos. Estes contratos são a única interface entre as frentes.

**Seu código se adapta ao contrato. O contrato nunca se adapta ao seu código.**

---

## 1. Função de partição (R2) — 🔒 congelada

```
bucket(cnpj_basico) = murmur3_32(utf8(cnpj_basico)) mod 256
```

- Resultado normalizado para não-negativo (`(h % 256 + 256) % 256`).
- Entrada: `cnpj_basico` como **string de 8 posições**, incluindo zeros à esquerda.
- Implementação de referência: Guava `Hashing.murmur3_32().hashString(cnpjBasico, UTF_8).asInt()`.
- **Proibido** `String.hashCode()` ou qualquer hash cuja estabilidade dependa da JVM implícita.
- N = 256 é fixo. Nunca configurável em tempo de execução.

**Teste obrigatório:** arquivo `cdc-cnpj-common/src/test/resources/partition-test-vectors.json`
com pares `{ "cnpj_basico": "...", "bucket": N }` — mínimo 6 vetores, incluindo zeros à esquerda
e caracteres alfanuméricos. Os valores exatos são gerados pelo código de referência na Fase 1 e
nunca alterados sem ADR.

---

## 2. Tratamento do CNPJ (R1) — 🔒 congelado

| Aspecto | Regra |
|---|---|
| Tipo | **String** em toda a stack. Parquet `string`, Postgres `VARCHAR(14)`, chave Kafka `String` |
| Proibido | `long`, `int`, `BIGINT`, `Integer.parseInt`, `Long.parseLong` |
| Composição | `cnpj_basico` (8) + `cnpj_ordem` (4) + `cnpj_dv` (2) = 14 posições |
| Campo derivado | `cnpj` = concatenação dos três segmentos, sem formatação |
| Alfanumérico | Desde 31/07/2026 as 8 primeiras posições podem conter letras |
| Dígito verificador | Regra do formato misto: valor do caractere = código ASCII − 48 |
| Ordenação | Lexicográfica, nunca numérica |
| Particionamento | Por hash (§1), nunca por faixa numérica |

---

## 3. Camada canônica Parquet — 🔒 congelada

### 3.1 Regras gerais

- Schema **explícito e versionado** (`schema_version: "1.0"`). Nunca inferido do CSV.
- Encoding de origem: **ISO-8859-1 (Latin-1)** conforme `cnpj-metadados.pdf`. Leitura converte
  para UTF-8. Se amostra empírica na ingestão indicar divergência em >0,1% das linhas, o estágio
  falha (R4) — não se troca encoding silenciosamente.
- Separador CSV: **`;` (ponto e vírgula)**. Campos textuais podem conter `;` entre aspas duplas;
  o parser deve respeitar RFC-style quoting.
- Campos de código permanecem **string**: CNAE, município, natureza jurídica, situação
  cadastral, motivo, qualificação, país, porte. Zeros à esquerda são significativos.
- Datas normalizadas para `DATE` (Parquet logical type). Sentinela `0`, `00000000` ou data
  impossível → `NULL` **com contador** `sentinel_dates_*` por coluna no relatório do estágio.
- `capital_social` permanece **string** no Parquet (formato brasileiro `1.234,56` na origem);
  conversão numérica, se necessária, é responsabilidade do consumidor, não da camada canônica.
- `cnae_fiscal_secundaria` permanece **string** com códigos separados por vírgula, como na origem.

### 3.2 Colunas de proveniência — obrigatórias em toda tabela

| Coluna | Tipo | Conteúdo |
|---|---|---|
| `competencia` | `string` | `YYYY-MM` |
| `arquivo_origem` | `string` | nome do ZIP e da entrada interna |
| `sha256_origem` | `string` | hash do ZIP de origem, conforme o manifesto |
| `linha_origem` | `int64` | número da linha no CSV de origem (1-based) |

### 3.3 Layout de diretórios

```
data/canonical/competencia=2026-06/tabela=estabelecimentos/bucket=000/part-0.parquet
```

Particionamento Hive-style por `competencia`, `tabela` e `bucket`. Bucket com **3 dígitos**
e zeros à esquerda.

### 3.4 Ponte de município

- `municipio_receita` — código SIAFI de 4 dígitos da Receita (coluna `municipio` do CSV).
- `municipio_ibge` — código IBGE de 7 dígitos via `tabmun.csv` (Tesouro Nacional).
- Contador `municipio_ibge_nao_resolvido` por competência — **nunca silencia** ausências.

### 3.5 Schemas por tabela

Colunas listadas abaixo **além** das de proveniência (§3.2). Posição = índice no CSV (`;`-separated).

#### `empresas` — arquivo `Empresas*.zip`

| Pos | Coluna canônica | Tipo Parquet | Obrig. | Chave / notas |
|---|---|---|---|---|
| 0 | `cnpj_basico` | `string` | sim | PK natural |
| 1 | `razao_social` | `string` | não | |
| 2 | `natureza_juridica` | `string` | não | FK → `naturezas` |
| 3 | `qualificacao_responsavel` | `string` | não | FK → `qualificacoes` |
| 4 | `capital_social` | `string` | não | formato BR na origem |
| 5 | `porte` | `string` | não | `00`,`01`,`03`,`05` |
| 6 | `ente_federativo_responsavel` | `string` | não | naturezas `1XXX` |

**Chave natural para diff:** `cnpj_basico`.

#### `estabelecimentos` — arquivo `Estabelecimentos*.zip`

| Pos | Coluna canônica | Tipo Parquet | Obrig. | Chave / notas |
|---|---|---|---|---|
| 0 | `cnpj_basico` | `string` | sim | parte da PK |
| 1 | `cnpj_ordem` | `string` | sim | parte da PK; `0001`=matriz |
| 2 | `cnpj_dv` | `string` | sim | parte da PK |
| — | `cnpj` | `string` | sim | derivado §2; redundante intencional |
| 3 | `identificador_matriz_filial` | `string` | não | `1`=matriz, `2`=filial |
| 4 | `nome_fantasia` | `string` | não | |
| 5 | `situacao_cadastral` | `string` | não | `01`–`08` |
| 6 | `data_situacao_cadastral` | `date` | não | |
| 7 | `motivo_situacao_cadastral` | `string` | não | FK → `motivos` |
| 8 | `nome_cidade_exterior` | `string` | não | |
| 9 | `pais` | `string` | não | FK → `paises` |
| 10 | `data_inicio_atividade` | `date` | não | |
| 11 | `cnae_fiscal_principal` | `string` | não | FK → `cnaes` |
| 12 | `cnae_fiscal_secundaria` | `string` | não | lista CSV separada por `,` |
| 13 | `tipo_logradouro` | `string` | não | |
| 14 | `logradouro` | `string` | não | |
| 15 | `numero` | `string` | não | |
| 16 | `complemento` | `string` | não | |
| 17 | `bairro` | `string` | não | |
| 18 | `cep` | `string` | não | 8 dígitos |
| 19 | `uf` | `string` | não | 2 letras |
| 20 | `municipio_receita` | `string` | não | código Receita |
| — | `municipio_ibge` | `string` | não | ponte §3.4 |
| 21 | `ddd_1` | `string` | não | |
| 22 | `telefone_1` | `string` | não | |
| 23 | `ddd_2` | `string` | não | |
| 24 | `telefone_2` | `string` | não | |
| 25 | `ddd_fax` | `string` | não | |
| 26 | `fax` | `string` | não | |
| 27 | `correio_eletronico` | `string` | não | |
| 28 | `situacao_especial` | `string` | não | |
| 29 | `data_situacao_especial` | `date` | não | |

**Chave natural para diff:** (`cnpj_basico`, `cnpj_ordem`, `cnpj_dv`).

#### Tabelas de domínio — um registro por linha, sem bucket

Arquivos: `Cnaes.zip`, `Municipios.zip`, `Motivos.zip`, `Naturezas.zip`, `Qualificacoes.zip`,
`Paises.zip`. Layout comum:

| Pos | Coluna canônica | Tipo Parquet | Obrig. |
|---|---|---|---|
| 0 | `codigo` | `string` | sim |
| 1 | `descricao` | `string` | não |

**Nome da tabela Parquet:** `cnaes`, `municipios`, `motivos`, `naturezas`, `qualificacoes`, `paises`.

**Chave natural para diff:** `codigo`.

**Layout de domínio:** escrito em `data/canonical/competencia=YYYY-MM/tabela=cnaes/` (sem subpasta
`bucket=` — arquivo único por competência).

---

## 4. Envelope de evento — 🔒 congelado

### 4.1 Estrutura (JSON Schema `event-envelope-1.0.json`)

```json
{
  "schema_version": "1.0",
  "event_id": "uuid-v5-deterministico",
  "event_type": "estabelecimento.situacao_alterada",
  "entity_key": { "cnpj": "12345678000195" },
  "competencia_de": "2026-06",
  "competencia_para": "2026-07",
  "campos_alterados": ["data_situacao_cadastral", "situacao_cadastral"],
  "before": { "situacao_cadastral": "02", "data_situacao_cadastral": "2019-03-11" },
  "after":  { "situacao_cadastral": "08", "data_situacao_cadastral": "2026-07-02" },
  "detected_at": "2026-08-15T04:12:03Z",
  "pipeline": { "estrategia": "E3", "run_id": "uuid-da-execucao" }
}
```

**Regra de ouro:** um evento representa a diferença entre duas competências, não a data exata da
mudança no mundo. `detected_at` é quando **nosso pipeline** detectou a diferença.

**`entity_key` por entidade:**

| Entidade | Campos |
|---|---|
| `empresas` | `{ "cnpj_basico": "12345678" }` |
| `estabelecimentos` | `{ "cnpj": "12345678000195" }` — CNPJ completo 14 chars |
| `dominio` | `{ "tabela": "cnaes", "codigo": "6201501" }` |

Valores em `before`/`after` usam **nomes canônicos** do Parquet (§3.5), datas como `YYYY-MM-DD`,
campos ausentes omitidos (não `null` explícito salvo valor que virou NULL).

### 4.2 `event_id` determinístico (R6) — 🔒 congelado

```
NAMESPACE_CDC_CNPJ = uuid_v5(NAMESPACE_DNS, "cdc-cnpj")
                   = 1f7a95b3-e59c-5ed5-af9e-7e2d84a7a8cd

canonical_string =
  competencia_de + "|" +
  competencia_para + "|" +
  entity_key_canonical + "|" +
  event_type + "|" +
  campos_alterados_ordenados

event_id = uuid_v5(NAMESPACE_CDC_CNPJ, canonical_string)
```

**`entity_key_canonical`** — ordenação lexicográfica de `chave=valor` unidos por `&`:

- Empresa: `cnpj_basico=12345678`
- Estabelecimento: `cnpj=12345678000195`
- Domínio: `codigo=6201501&tabela=cnaes`

**`campos_alterados_ordenados`:** nomes canônicos ordenados alfabeticamente, unidos por `,`.
Vazio para `*.criada` / `*.criado` / `*.encerrada` (sem campos alterados).

**Proibido** `UUID.randomUUID()` para `event_id`.

### 4.3 Taxonomia e precedência — 🔒 congelada

| Tópico Kafka | Tipos (Núcleo) |
|---|---|
| `cnpj.empresas` | `empresa.criada`, `empresa.encerrada`, `empresa.alterada` |
| `cnpj.estabelecimentos` | `estabelecimento.criado`, `estabelecimento.situacao_alterada`, `estabelecimento.endereco_alterado`, `estabelecimento.cnae_alterado`, `estabelecimento.alterado` |
| `cnpj.dominio` | `dominio.tabela_alterada` |

**Mapeamento `ChangeRecord` → `event_type`:**

| change_type | Condição | event_type |
|---|---|---|
| `INSERT` | tabela `empresas` | `empresa.criada` |
| `DELETE` | tabela `empresas` | `empresa.encerrada` |
| `UPDATE` | tabela `empresas` | `empresa.alterada` |
| `INSERT` | tabela `estabelecimentos` | `estabelecimento.criado` |
| `DELETE` | tabela `estabelecimentos` | tratar como `estabelecimento.situacao_alterada` se `after.situacao_cadastral=08`, senão `estabelecimento.alterado` |
| `UPDATE` | tabela `estabelecimentos` | ver precedência abaixo |
| `UPDATE` | tabela domínio | `dominio.tabela_alterada` |

**Precedência para `UPDATE` em estabelecimentos** — avaliar na ordem; emite o **primeiro** que
couber; se nenhum grupo cobrir **todos** os campos alterados, emite `estabelecimento.alterado`:

| Prioridade | event_type | Grupo de campos (subset) |
|---|---|---|
| 1 | `estabelecimento.situacao_alterada` | `situacao_cadastral`, `data_situacao_cadastral`, `motivo_situacao_cadastral`, `situacao_especial`, `data_situacao_especial` |
| 2 | `estabelecimento.endereco_alterado` | `tipo_logradouro`, `logradouro`, `numero`, `complemento`, `bairro`, `cep`, `uf`, `municipio_receita`, `municipio_ibge`, `nome_cidade_exterior`, `pais` |
| 3 | `estabelecimento.cnae_alterado` | `cnae_fiscal_principal`, `cnae_fiscal_secundaria` |
| 4 | `estabelecimento.alterado` | qualquer outro caso |

Tipo específico só é emitido se `campos_alterados ⊆ grupo` **e** `campos_alterados ≠ ∅`.

### 4.4 Publicação

- Chave Kafka: `cnpj_basico` (string de 8 posições). Para domínio: chave `"dominio"`.
- Semântica: **at-least-once**. Idempotência do consumidor via `event_id`.

---

## 5. Interface `DiffEngine` — 🔒 congelada

### 5.1 Assinatura

```java
Stream<ChangeRecord> diff(
    Path canonicalRoot,
    String competenciaDe,
    String competenciaPara,
    String tabela,
    int bucketId
);
```

- `canonicalRoot`: diretório `data/canonical/`.
- `bucketId`: 0–255; lê apenas `bucket=XXX` das duas competências.
- `tabela`: `empresas`, `estabelecimentos`, ou nome de domínio.

### 5.2 `ChangeRecord`

```java
record ChangeRecord(
    ChangeType changeType,           // INSERT | DELETE | UPDATE
    String tabela,
    int bucketId,
    Map<String, String> entityKey,   // chave natural conforme §3.5
    Map<String, String> before,      // vazio para INSERT
    Map<String, String> after,       // vazio para DELETE
    List<String> camposAlterados,    // ordenada alfabeticamente; vazia p/ INSERT
    String competenciaDe,
    String competenciaPara
) {}
```

Campos comparáveis no diff: todas as colunas de negócio do §3.5 **exceto** proveniência (§3.2) e
`cnpj` derivado (comparar pelos três segmentos).

### 5.3 Comportamento obrigatório

- Mesmo conjunto de `event_id` (via §4.2) para mesma entrada — base do teste E1 ≡ E3.
- No máximo **um bucket** carregado por vez por motor.
- Reexecutável: mesma entrada → mesma sequência de `ChangeRecord` (ordem estável por chave natural).
- Motores **não** publicam Kafka; produzem apenas `ChangeRecord`.

---

## 6. Manifesto de competência — 🔒 congelado

Arquivo: `data/manifests/competencia=YYYY-MM/manifesto.json`

```json
{
  "schema_version": "1.0",
  "competencia": "2026-06",
  "gerado_em": "2026-08-27T14:30:00Z",
  "origem": "casa_dos_dados",
  "origem_url_base": "https://...",
  "arquivos": [
    {
      "nome": "Estabelecimentos0.zip",
      "tabela": "estabelecimentos",
      "parte": 0,
      "tamanho_bytes": 1234567890,
      "sha256": "hex-lowercase-64-chars",
      "url": "https://...",
      "baixado_em": "2026-08-27T12:00:00Z",
      "espelho": true
    }
  ]
}
```

| Campo | Regra |
|---|---|
| `origem` | `casa_dos_dados` \| `receita_federal` |
| `espelho` | `true` se URL não for domínio oficial da Receita |
| `sha256` | hex lowercase, 64 caracteres, do arquivo **compactado** |
| `tabela` | nome canônico: `empresas`, `estabelecimentos`, `cnaes`, … |

Manifesto é versionado no repositório. Dados brutos (`*.zip`) ficam em `data/raw/` (gitignored).

---

## 7. Amostra experimental (diff)

Diff E1/E3 roda sobre **16 buckets** listados em [`docs/experiment-buckets.txt`](experiment-buckets.txt)
(buckets `n×16`, n=0..15). Normalização escreve **256 buckets** (ADR-002).

---

## Histórico de versões deste contrato

| Versão | Data | Mudança | ADR |
|---|---|---|---|
| 1.0-rascunho | 2026-08-18 | Criação a partir da spec v1.0 | — |
| 1.0 | 2026-08-27 | Congelamento: schemas §3.5, precedência §4.3, ChangeRecord §5, manifesto §6 | — |
