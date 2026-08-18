# CONTRATOS CONGELADOS — CDC-CNPJ

**Versão:** 1.0-rascunho · **Congelamento previsto:** 24/ago/2026 (fim da S1)
`ESTADO_CONTRATO: EM_DEFINICAO`  <!-- valores: EM_DEFINICAO | CONGELADO -->

Vira `CONGELADO` ao fim da S1. Não altere esta linha sem que os contratos estejam de fato fechados.

> Este é o documento **normativo** dos contratos. Depois do congelamento, alterar qualquer
> coisa aqui exige ADR em [`DECISOES.md`](DECISOES.md) aprovado pelas **quatro frentes**.
>
> A razão é simples: estes contratos são a única interface entre as frentes. Mudá-los no meio
> do projeto quebra o trabalho de todo mundo ao mesmo tempo, e não há folga no cronograma
> para absorver isso.

**Seu código se adapta ao contrato. O contrato nunca se adapta ao seu código.**

---

## 1. Função de partição (R2) — 🔒 congelada desde já

```
bucket(cnpj_basico) = murmur3_32(utf8(cnpj_basico)) mod 256
```

- Resultado normalizado para não-negativo (`(h % 256 + 256) % 256`).
- Entrada: `cnpj_basico` como **string de 8 posições**, incluindo zeros à esquerda.
- **Proibido** `String.hashCode()` ou qualquer hash cuja estabilidade dependa da JVM,
  da versão ou da plataforma.
- N = 256 é fixo. Nunca configurável em tempo de execução.

**Por que congelada antes das outras.** Se esta função mudar depois que a normalização rodar,
todo o Parquet gerado torna-se incomparável entre competências e a frente A perde semanas.
É a decisão mais cara de reverter no projeto inteiro.

**Teste obrigatório:** vetores de teste fixos (`cnpj_basico` → bucket esperado) versionados
junto ao código, garantindo determinismo entre máquinas e entre execuções.

---

## 2. Tratamento do CNPJ (R1, R4 da spec) — 🔒 congelado desde já

| Aspecto | Regra |
|---|---|
| Tipo | **String** em toda a stack. Parquet `string`, Postgres `VARCHAR(14)`, chave Kafka `String` |
| Proibido | `long`, `int`, `BIGINT`, `Integer.parseInt`, `Long.parseLong` |
| Composição | `cnpj_basico` (8) + `cnpj_ordem` (4) + `cnpj_dv` (2) = 14 posições |
| Alfanumérico | Desde 31/07/2026 as 8 primeiras posições podem conter letras |
| Dígito verificador | Regra do formato misto: valor do caractere = código ASCII − 48 |
| Ordenação | Lexicográfica, nunca numérica |
| Particionamento | Por hash (§1), nunca por faixa numérica |

---

## 3. Camada canônica Parquet

**Estado:** 🔶 a fechar na S1 — frente A propõe, as quatro aprovam.

### 3.1 Regras gerais (já acordadas)

- Schema **explícito e versionado**. Nunca inferido do CSV.
- Campos de código permanecem **string**: CNAE, município, natureza jurídica, situação
  cadastral, motivo, qualificação, país. São identificadores, e zeros à esquerda são
  significativos.
- Datas normalizadas para `DATE`. Valores sentinela (`0`, `00000000`, datas impossíveis) viram
  `NULL` **com contador de ocorrências** reportado pelo estágio.
- Texto normalizado para UTF-8 na leitura; o encoding de origem é legado e deve ser
  **determinado empiricamente na S1**, não presumido.

### 3.2 Colunas de proveniência — obrigatórias em toda tabela

| Coluna | Tipo | Conteúdo |
|---|---|---|
| `competencia` | `string` | `YYYY-MM` |
| `arquivo_origem` | `string` | nome do ZIP e da entrada interna |
| `sha256_origem` | `string` | hash do ZIP de origem, conforme o manifesto |
| `linha_origem` | `int64` | número da linha no CSV de origem |

Sem proveniência não há reprodutibilidade, e reprodutibilidade é requisito do TCC.

### 3.3 Layout de diretórios

```
data/canonical/competencia=2026-06/tabela=estabelecimentos/bucket=000/part-0.parquet
```

Particionamento por `competencia`, `tabela` e `bucket`. O `bucket` é escrito com **3 dígitos**
e zeros à esquerda, para ordenação lexicográfica correta.

### 3.4 Ponte de município

A Receita usa código próprio (SIAFI), **não** o código IBGE de 7 dígitos. A normalização:

- preserva `municipio_receita` (código original);
- acrescenta `municipio_ibge` via `tabmun.csv` do Tesouro Nacional;
- registra ausências de correspondência em contador próprio — **nunca silencia**.

### 3.5 Schemas por tabela

> 🔶 **A PREENCHER NA S1** pela frente A, a partir de `cnpj-metadados.pdf`. Cada tabela recebe
> a lista completa de colunas com nome canônico, tipo, obrigatoriedade e a posição de origem
> no CSV. Enquanto esta seção estiver vazia, as frentes B, C e D trabalham contra as fixtures
> sintéticas da frente D.

- [ ] `empresas`
- [ ] `estabelecimentos`
- [ ] tabelas de domínio (`cnaes`, `municipios`, `motivos`, `naturezas`, `qualificacoes`, `paises`)

---

## 4. Envelope de evento

**Estado:** 🔶 estrutura acordada, campos a confirmar na S1.

```json
{
  "schema_version": "1.0",
  "event_id": "uuid-v5-deterministico",
  "event_type": "estabelecimento.situacao_alterada",
  "entity_key": { "cnpj": "12ABC34500019X" },
  "competencia_de": "2026-06",
  "competencia_para": "2026-07",
  "campos_alterados": ["situacao_cadastral", "data_situacao"],
  "before": { "situacao_cadastral": "02", "data_situacao": "2019-03-11" },
  "after":  { "situacao_cadastral": "08", "data_situacao": "2026-07-02" },
  "detected_at": "2026-08-15T04:12:03Z",
  "pipeline": { "estrategia": "E3", "run_id": "..." }
}
```

### 4.1 Regra de ouro

**Um evento representa a diferença entre duas competências, não uma data exata de mudança.**

A granularidade do dado de origem é mensal e o contrato é honesto quanto a isso. É por essa
razão que `competencia_de` e `competencia_para` existem. `detected_at` é o instante em que
*nosso pipeline* detectou a diferença — **jamais** o instante em que o fato ocorreu no mundo.
Documentar isso protege o consumidor de uma interpretação errada e é ponto de defesa na banca.

### 4.2 `event_id` determinístico (R6) — 🔒 congelado desde já

```
event_id = uuid_v5(NAMESPACE_CDC_CNPJ,
                   competencia_de | competencia_para | entity_key | event_type |
                   campos_alterados_ordenados)
```

- Separador e ordenação canônicos definidos no código, com vetores de teste versionados.
- `campos_alterados` **sempre ordenado** antes de entrar no hash.
- **Proibido** `UUID.randomUUID()`.

Dois motivos: idempotência barata no consumo sob at-least-once; e a validação E1≡E3 vira
comparação de conjuntos de IDs em vez de comparação semântica campo a campo.

### 4.3 Taxonomia

| Tópico | Núcleo | Expansão |
|---|---|---|
| `cnpj.empresas` | `empresa.criada`, `empresa.encerrada`, `empresa.alterada` | — |
| `cnpj.estabelecimentos` | `estabelecimento.criado`, `estabelecimento.situacao_alterada`, `estabelecimento.endereco_alterado`, `estabelecimento.cnae_alterado`, `estabelecimento.alterado` | — |
| `cnpj.socios` | — | `socio.incluido`, `socio.excluido`, `socio.alterado` |
| `cnpj.simples` | — | `simples.opcao_alterada` |
| `cnpj.dominio` | `dominio.tabela_alterada` | — |

**Regra de precedência** (🔶 a fechar na S1): tipos específicos são emitidos quando a mudança
se restringe àquele grupo de campos; caso contrário emite-se o `*.alterado` genérico com a
lista completa em `campos_alterados`. A definição exata dos grupos de campos entra aqui.

### 4.4 Publicação

- Chave Kafka: `cnpj_basico` (garante ordenação por empresa).
- Um tópico por entidade, conforme a tabela acima.
- Semântica: **at-least-once**. A idempotência é responsabilidade do consumidor, viabilizada
  pelo `event_id` determinístico.

---

## 5. Interface `DiffEngine`

**Estado:** 🔶 a fechar na S1 pela frente B.

```
DiffEngine.diff(snapshotA, snapshotB, tabela, bucketId) -> Stream<ChangeRecord>
```

Contrato de comportamento que **ambos** os motores devem honrar:

- Produzir o **mesmo conjunto** de `event_id` para a mesma entrada — esta é a base do teste
  E1 ≡ E3 e não é negociável.
- Não carregar mais de um bucket em memória por vez.
- Não escrever nada fora do diretório de saída designado.
- Ser reexecutável: rodar duas vezes sobre a mesma entrada produz exatamente o mesmo resultado.

`ChangeRecord` 🔶 a definir: tipo de mudança (inserção, remoção, alteração), chave natural,
estado anterior, estado posterior, conjunto de campos alterados.

---

## 6. Manifesto de competência

**Estado:** 🔶 a fechar na S1 pela frente A.

Um `manifesto.json` por competência, contendo por arquivo baixado: nome, tamanho em bytes,
SHA-256, URL de origem, se veio do espelho ou da origem oficial, e o timestamp do download.

O manifesto é o que permite reproduzir o experimento sem redistribuir 85 GB — é entregável do
projeto, não artefato interno.

---

## Histórico de versões deste contrato

| Versão | Data | Mudança | ADR |
|---|---|---|---|
| 1.0-rascunho | 2026-08-18 | Criação a partir da spec v1.0 | — |
