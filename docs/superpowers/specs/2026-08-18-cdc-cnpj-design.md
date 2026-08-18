# CDC-CNPJ — Documento de Design (Spec)

| Campo | Valor |
|---|---|
| Projeto | CDC-CNPJ — Change Data Capture sobre snapshots dos Dados Abertos do CNPJ |
| Versão | 1.0 |
| Data | 2026-08-18 |
| Janela de execução | 18/ago/2026 a 16/nov/2026 (13 semanas) |
| Equipe | 4 pessoas, ~9 h/semana cada (~470 h totais) |
| Abordagem aprovada | **A — Vertical fino com escala amostral** |
| Origem | `TCC ideia Univesp.pdf` (dossiê técnico v1.0, ago/2026) |

> Este documento é a **fonte da verdade** do projeto. Divergências entre este documento e
> qualquer outro artefato (código, dossiê original, conversas) resolvem-se a favor deste
> documento — ou por uma decisão registrada em `docs/DECISOES.md`.

---

## 1. Contexto e problema

O Cadastro Nacional da Pessoa Jurídica é o maior cadastro empresarial aberto do Brasil.
A Receita Federal o publica **mensalmente**, como fotografia completa: dezenas de arquivos
ZIP contendo CSVs, ~85 GB descompactados, sem nenhuma API.

A consequência é que **não existe forma aberta de saber o que mudou**. Quem precisa do
delta — jornalismo de dados, órgãos de controle, pesquisa acadêmica, análise de crédito —
tem duas opções: reprocessar 85 GB por conta própria a cada mês, ou pagar por um serviço
comercial fechado que faz isso e não publica nem o método nem os números.

Change Data Capture (CDC) clássico resolve esse problema lendo o log de transações do banco
de origem. Aqui não há banco nem log: há duas fotografias gigantes. A literatura chama essa
situação de **snapshot differencing** e historicamente a trata como recurso de último caso.
Este trabalho a trata como objeto de primeira classe.

## 2. Pergunta de pesquisa

> É viável construir, com recursos computacionais de commodity, um pipeline reprodutível que
> converta snapshots mensais do CNPJ em um fluxo de eventos de mudança semanticamente
> tipado — e qual o custo comparativo entre uma estratégia relacional e uma estratégia
> particionada por hash para o cálculo desse delta?

## 3. Objetivos

**Objetivo geral.** Projetar, implementar e avaliar experimentalmente um protótipo de pipeline
de CDC baseado em snapshot differencing sobre os Dados Abertos do CNPJ, publicando método,
código e medições de forma aberta e reprodutível.

**Objetivos específicos.**

1. Caracterizar quantitativamente os snapshots do CNPJ: volume por tabela, distribuição de
   registros por partição e taxa real de mudança mensal por tipo de evento.
2. Especificar uma camada canônica (Parquet, schema explícito e versionado) resistente às
   armadilhas conhecidas da fonte: encoding legado, CSV posicional com delimitadores
   embutidos, particionamento instável de arquivos e o novo CNPJ alfanumérico.
3. Implementar e comparar dois motores de diff sob uma mesma interface: baseline relacional
   (E1) e particionado por hash (E3).
4. Especificar um contrato de eventos versionado, inspirado na semântica Debezium, e
   publicá-lo em Apache Kafka.
5. Medir tempo, memória de pico, I/O, throughput e custo projetado de cada motor sob
   protocolo experimental documentado e repetível.
6. Validar a corretude dos eventos sem dispor de gabarito oficial, por meio de validação
   cruzada entre implementações, amostragem contra oráculo externo e invariantes de sanidade.

## 4. Hipóteses

| ID | Enunciado | Critério de aceitação |
|---|---|---|
| **H1** | O particionamento estável por `hash(cnpj_basico) % N` permite executar o diff de snapshots multi-GB com **memória de pico aproximadamente constante** em relação ao volume total, limitada pelo maior bucket. | Memória de pico de E3 varia menos de 20% entre processar 1 bucket e 16 buckets, enquanto a de E1 cresce monotonicamente com a escala. |
| **H2** | Um ciclo completo (ingestão → normalização → diff → publicação) de uma competência é executável em **menos de 24 h** em hardware de commodity (≤ 8 vCPU, ≤ 32 GB RAM, SSD). | Tempo total medido, ou projetado a partir da amostra com intervalo de confiança, inferior a 24 h. |
| **H3** | A estratégia particionada (E3) apresenta speedup relevante sobre o baseline relacional (E1) para o mesmo conjunto de buckets. | Speedup mediano `t_E1 / t_E3` ≥ 2,0 sobre 5 repetições. |

Hipóteses são **falsificáveis por construção**: um resultado negativo em qualquer uma delas é
resultado publicável, desde que os números expliquem o porquê. A spec não presume que as três
se confirmem.

## 5. Escopo

A regra que governa o projeto: **nada da coluna Expansão começa antes de toda a coluna Núcleo
estar verde.** O estado corrente de cada item vive em `docs/ESCOPO.md` e deve ser atualizado a
cada marco.

### 5.1 Núcleo (semanas 1–8) — obrigatório

- Ingestão e congelamento das competências **2026-06** e **2026-07**, com SHA-256 e manifesto.
- Normalização em streaming para Parquet canônico das tabelas **Empresas**, **Estabelecimentos**
  e tabelas de domínio (Cnaes, Municipios, Motivos, Naturezas, Qualificacoes, Paises).
- Particionamento em **todos os 256 buckets** (o passe de leitura é O(n); particionar em 256 em
  vez de 16 não acrescenta custo relevante e fornece a distribuição completa de tamanhos).
- **16 buckets** como conjunto experimental para o diff.
- Motores **E1** (Postgres, baseline de corretude, por bucket) e **E3** (hash particionado).
- Eventos `empresa.*` e `estabelecimento.*` publicados em Kafka local.
- Consumer demo materializando um changelog consultável em Postgres.
- Bancada de medição: tempo, memória de pico, I/O, throughput.
- Validação cruzada E1≡E3 e validação amostral contra oráculo externo.
- Gerador de dados sintéticos (destrava as frentes B, C e D já na semana 2).

### 5.2 Expansão (semanas 9–11) — por ordem de valor, e só se o núcleo fechou

1. Execução de escala completa nos 256 buckets — é o que valida **H2** por medição direta.
2. Tabela **Socios** e o problema de identidade (P2), com métrica de eventos fantasma.
3. Tabela **Simples**.
4. Quarentena de deletes (P3).

### 5.3 Fora de escopo — declarado na monografia como trabalhos futuros

- Motor **E2** (sort-merge externo).
- Apache Spark.
- Schema Registry e Avro (o projeto usa **JSON Schema versionado**).
- Retenção longa e replay histórico no Kafka.
- Séries com mais de duas competências e análise sazonal.

> Itens fora de escopo **não são backlog**. Mover um item de Fora para Núcleo exige um registro
> em `docs/DECISOES.md` explicitando o que sai em troca. O orçamento de horas é fixo.

---

## 6. Arquitetura

Cinco estágios. As interfaces entre eles são **arquivos Parquet** e **tópicos Kafka** — o que
permite às quatro frentes trabalharem em paralelo com acoplamento quase nulo.

```
[1 INGESTÃO]      ZIPs 2026-06 + 2026-07 -> SHA-256 -> manifesto da competência
                  espelho Casa dos Dados (plano A) -> origem oficial (plano B)
        v  arquivos ZIP verificados + manifesto.json
[2 NORMALIZAÇÃO]  ZIP -> parser em streaming -> Parquet canônico
                  - NUNCA materializa o CSV em disco (restrição física, ver 6.1)
                  - schema explícito versionado; CNPJ como STRING
                  - ponte município Receita->IBGE (tabmun.csv, Tesouro Nacional)
                  - particionado por hash(cnpj_basico) % 256
                  - contadores de rejeição por arquivo
        v  Parquet particionado, imutável, versionado
[3 DIFF]          DiffEngine(snapshotA, snapshotB, bucketId) -> Stream<ChangeRecord>
                  E1  Postgres FULL OUTER JOIN — baseline de corretude, POR BUCKET
                  E3  hash por registro + comparação paralela por bucket
        v  Stream<ChangeRecord>
[4 EVENTOS]       ChangeRecord -> envelope tipado (JSON Schema versionado)
                  Kafka local; key = cnpj_basico; tópicos por entidade
        v  tópicos Kafka
[5 CONSUMO]       consumer materializa changelog consultável em Postgres
                  (prova de utilidade E ferramenta de QA do próprio time)

transversal: bancada de medição (Micrometer + métricas de SO); gerador sintético
```

### 6.1 Restrição física que molda o design

A máquina de referência dispõe de **216 GB livres**. Duas competências descompactadas em CSV
ocupariam ~170 GB, e o Parquet ainda precisaria caber por cima. **Não cabe.**

Portanto o estágio 2 **deve** normalizar em streaming: ler o ZIP, decodificar, parsear e escrever
Parquet sem jamais materializar o CSV completo em disco. Isto não é otimização; é requisito de
viabilidade, e qualquer implementação que grave CSV intermediário está errada.

### 6.2 Decisões arquiteturais congeladas

Estas quatro decisões são **caras de reverter** e estão congeladas desde a semana 1. Alterá-las
exige ADR em `docs/DECISOES.md` com aprovação de todas as frentes.

**D1 — Função de partição explícita e congelada.**
`bucket(cnpj_basico) = murmur3_32(utf8(cnpj_basico)) mod 256`, com resultado normalizado para
não-negativo. **Proibido usar `String.hashCode()`** ou qualquer hash cuja estabilidade dependa da
JVM ou da plataforma. Se a função de partição mudar no meio do projeto, todos os snapshots já
normalizados tornam-se incomparáveis e o trabalho de normalização é perdido.

**D2 — E1 opera bucket a bucket, jamais globalmente.**
Carrega o bucket N das duas competências em tabelas temporárias, executa o FULL OUTER JOIN,
emite os `ChangeRecord`, descarta. O Postgres nunca vê o dataset inteiro. É isso que torna o
baseline viável no hardware disponível.

**D3 — `event_id` determinístico.**
`event_id = uuid_v5(namespace, competencia_de | competencia_para | entity_key | event_type | campos_alterados_ordenados)`.

Dois ganhos: idempotência barata no consumo sob semântica at-least-once (P7); e — o mais
importante — a validação cruzada E1≡E3 reduz-se a **comparação de conjuntos de IDs**, em vez de
comparação semântica campo a campo. Economiza dias de trabalho na semana 7.

**D4 — CNPJ é STRING em toda a stack.**
Coluna Parquet `string`; coluna Postgres `VARCHAR(14)`; chave Kafka `String`. **Jamais `BIGINT`,
`long` ou `int`.** Desde 31/07/2026 novas inscrições podem conter letras nas 8 primeiras posições.
Consequências que o código deve respeitar: ordenação é lexicográfica; o dígito verificador segue
a regra do formato misto (valor do caractere = código ASCII − 48); particionamento por faixa
numérica é inválido — daí D1.

---

## 7. Contratos

Os contratos são congelados na semana 1 e vivem em `docs/CONTRATOS.md`, que é a versão
normativa. Esta seção registra a intenção de design.

### 7.1 Camada canônica (Parquet)

- Schema explícito e versionado — nunca inferido do CSV.
- Todo campo de código permanece **string** (CNAE, município, natureza jurídica, situação):
  são identificadores, não números, e zeros à esquerda são significativos.
- Datas normalizadas para `DATE`; datas inválidas ou sentinela (`0`, `00000000`) viram `NULL`
  com contador de ocorrências.
- Colunas de proveniência obrigatórias em toda tabela: `competencia`, `arquivo_origem`,
  `sha256_origem`, `linha_origem`.
- Ponte de município: a Receita usa código próprio (SIAFI), não o código IBGE de 7 dígitos.
  A normalização acrescenta `municipio_ibge` via `tabmun.csv` do Tesouro Nacional, mantendo
  também o código original. Ausência de correspondência é registrada, não silenciada.

### 7.2 Envelope de evento

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

**Regra de ouro do contrato.** Um evento representa a **diferença entre duas competências**, não
uma data exata de mudança. A granularidade do dado de origem é mensal, e o contrato é honesto
quanto a isso — é por essa razão que `competencia_de` e `competencia_para` existem, e por que
`detected_at` jamais deve ser interpretado como o instante em que o fato ocorreu no mundo.

### 7.3 Taxonomia de eventos

| Tópico | Tipos no núcleo | Tipos na expansão |
|---|---|---|
| `cnpj.empresas` | `empresa.criada`, `empresa.encerrada`, `empresa.alterada` | — |
| `cnpj.estabelecimentos` | `estabelecimento.criado`, `estabelecimento.situacao_alterada`, `estabelecimento.endereco_alterado`, `estabelecimento.cnae_alterado`, `estabelecimento.alterado` | — |
| `cnpj.socios` | — | `socio.incluido`, `socio.excluido`, `socio.alterado` |
| `cnpj.simples` | — | `simples.opcao_alterada` |
| `cnpj.dominio` | `dominio.tabela_alterada` | — |

Eventos `*.alterado` genéricos carregam `campos_alterados`; consumidores filtram. Os tipos
específicos (`situacao_alterada`, `endereco_alterado`, `cnae_alterado`) são emitidos quando a
mudança se restringe àquele grupo de campos, e a regra de precedência é documentada no contrato.

### 7.4 Semântica de entrega (P7)

At-least-once, com `event_id` determinístico tornando o consumo idempotente a baixo custo.
Chave de partição Kafka = `cnpj_basico`, garantindo ordenação por empresa.

---

## 8. Motores de diff

Ambos implementam a mesma interface e produzem `Stream<ChangeRecord>` a partir de
`(snapshotA, snapshotB, bucketId, tabela)`. A equivalência dos resultados é testada
automaticamente, não apenas medida no experimento.

**E1 — baseline relacional.** Carrega os dois lados do bucket em tabelas temporárias no
Postgres e executa `FULL OUTER JOIN` pela chave natural. Existe para ser **obviamente correto**,
não para ser rápido: é o oráculo contra o qual E3 é validado. Simplicidade de leitura tem
precedência sobre desempenho nesta implementação.

**E3 — hash particionado.** Para cada registro, calcula um hash de conteúdo dos campos
comparáveis. Compara os dois lados do bucket em memória por chave natural, classificando em
inserção, remoção ou alteração; para alterações, compara campo a campo para preencher
`campos_alterados`. Buckets são processados em paralelo, e o pico de memória é limitado pelo
maior bucket — que é a essência de **H1**.

---

## 9. Protocolo experimental

**Ambiente.** Máquina de referência única, integralmente documentada: CPU (modelo e núcleos),
RAM, tipo de disco, sistema operacional, versão e flags da JVM, versões de Postgres e Kafka.

O pipeline roda **nativo na JVM**; apenas Postgres e Kafka rodam em contêiner. A razão é que o
Docker Desktop sobre WSL2 acrescenta overhead de I/O real e variável, que contaminaria as
medições do estágio de normalização. Esta escolha é declarada na metodologia da monografia.

**Variáveis.**

| Tipo | Variáveis |
|---|---|
| Independentes | motor (E1, E3); escala (nº de buckets: 1, 4, 8, 16) |
| Dependentes | tempo de parede; memória de pico (RSS e heap); bytes lidos e escritos; throughput (eventos/s); CPU-tempo |
| Controladas | mesma máquina; mesmo par de snapshots; mesmos buckets; ausência de carga concorrente |

**Execução.** Cada configuração roda **5 repetições, descartando a primeira** (aquecimento de JIT
e de cache do sistema operacional). Reporta-se **mediana e amplitude interquartil** — nunca a
média, porque distribuições de tempo dominadas por I/O são assimétricas e a média engana.

O estado do cache de páginas é declarado explicitamente: no Windows, limpá-lo entre execuções é
impraticável, então todas as medições ocorrem com **cache quente**, e a monografia diz isso.

**As três medidas de "ganho".**

1. **Speedup** = `mediana(t_E1) / mediana(t_E3)` sobre os mesmos buckets. É o número-manchete.
2. **Escalabilidade de memória** = memória de pico em função do número de buckets processados.
   Se E3 permanecer aproximadamente constante enquanto E1 cresce, **este é o resultado mais
   forte do trabalho** — mais relevante que o speedup, porque é o que sustenta a afirmação de
   que o processamento cabe em máquina comum.
3. **Custo projetado** = extrapolação de 16 para 256 buckets, convertida em custo de instância
   cloud equivalente, com intervalo de confiança — e confirmada pela execução de escala completa,
   caso a expansão ocorra.

---

## 10. Validação de corretude (P8)

Não existe gabarito oficial do "diff verdadeiro". A corretude é construída em três camadas
independentes, e a **metodologia de validação é, ela própria, contribuição do trabalho**.

1. **Validação cruzada E1 ≡ E3.** Métrica: `|diferença simétrica| / |união|` dos conjuntos de
   `event_id`. Meta: zero. Qualquer divergência é bug de uma das implementações e bloqueia o
   marco. Roda como teste automatizado, não apenas como experimento.
2. **Validação amostral contra oráculo externo.** 200 eventos sorteados aleatoriamente com
   semente registrada, conferidos contra a API do Minha Receita ou a consulta pública da
   Receita. Métrica: taxa de concordância, com os desacordos analisados individualmente.
3. **Invariantes de sanidade.** Por exemplo:
   `|empresa.criada| − |empresa.encerrada| ≈ Δ(contagem de registros entre competências)`,
   com tolerância declarada. Violação de invariante bloqueia a publicação dos resultados.

---

## 11. Estratégia de testes

Em ordem de proteção oferecida ao projeto:

1. **Teste de propriedade (o mais valioso).** Para qualquer par de snapshots sintéticos com
   mudanças conhecidas, o conjunto de eventos emitido deve ser exatamente o esperado. É o que
   impede o pipeline de mentir silenciosamente.
2. **Teste de contrato na ingestão.** Valida o layout dos arquivos da Receita e falha
   ruidosamente se a fonte mudar — a Receita já quebrou os projetos da comunidade em 2019 e 2021.
3. **E1 ≡ E3 automatizado.** Equivalência entre implementações independentes como teste de
   regressão contínuo.
4. **Unitários.** Dígito verificador do CNPJ alfanumérico (formato misto); determinismo da função
   de partição; parser de linha CSV com aspas e `;` embutido; ponte município Receita→IBGE;
   normalização de datas sentinela.
5. **Integração.** Pipeline completo sobre fixture sintética pequena, executável em CI e em
   menos de dois minutos.

**Regra de qualidade do estágio 2:** a taxa de rejeição por arquivo é medida e comparada com a
contagem esperada de linhas. Acima de **0,1%**, o estágio falha. O princípio é *falhar
ruidosamente em vez de mentir baixinho* — diff sobre dado corrompido gera eventos falsos, e
eventos falsos invalidam todo o experimento.

---

## 12. Cronograma e frentes

Quatro frentes, acopladas apenas por Parquet e Kafka:
**A** ingestão e normalização · **B** motores de diff · **C** Kafka, contrato e consumer ·
**D** bancada de medição e análise.

O problema que a divisão ingênua não resolve: nas semanas 1–2 as frentes B, C e D não têm dado
real. A solução é **contratos primeiro** — congelar os schemas na semana 1 para que todos
trabalhem contra fixtures a partir da semana 2. É por isso que o gerador sintético pertence ao
núcleo, e não à expansão.

| Sem. | Datas | Foco | Marco |
|---|---|---|---|
| S1 | 18–24/ago | **Conjunta.** Ler `cnpj-metadados.pdf` e o ETL do minha-receita. Congelar schema Parquet, envelope de evento e função de partição. Iniciar download. Subir Docker Compose. | Contratos v1 + fixtures |
| S2–S3 | 25/ago–07/set | **A** parser streaming de Estabelecimentos (o mais difícil primeiro) · **B** E1 sobre fixtures · **C** produtor e consumer sobre eventos sintéticos · **D** harness de medição e gerador sintético | **Um bucket ponta a ponta (sintético)** |
| S4–S5 | 08–21/set | **A** normalização completa, 256 buckets, 2 competências · **B** E3 · **C** taxonomia completa · **D** bancada sobre dado real | **Primeiro evento REAL no Kafka** |
| S6–S7 | 22/set–05/out | **B** E1 e E3 estáveis nos 16 buckets · **D** validação cruzada e caracterização da taxa de mudança · **C** changelog consultável | **E1≡E3, diferença simétrica zero** |
| S8 | 06–12/out | Execuções definitivas (5 repetições × configurações) e validação amostral dos 200 eventos | **CONGELAMENTO DE ESCOPO** |
| S9–S11 | 13/out–02/nov | Expansão na ordem de valor da §5.2. Cada item entra só se o anterior fechou | Camadas opcionais |
| S12–S13 | 03–16/nov | Análise, gráficos, revisão da monografia, ensaio da defesa, empacotamento reprodutível | **Entrega** |

**A escrita corre em paralelo desde a S1 e não é negociável.** Cada marco fecha com sua seção
redigida: S3 → metodologia; S5 → arquitetura; S7 → corretude; S8 → resultados. Ao chegar na S12
o texto está ~70% pronto, o que evita o modo de falha mais comum em TCC.

Orçamento: núcleo (S1–S8) = 288 h; expansão e fechamento (S9–S13) = 180 h; total 468 h.

---

## 13. Riscos e critérios de corte

| Risco | Gatilho objetivo | Plano B |
|---|---|---|
| Download de dezenas de GB falha ou arrasta | S2 sem as duas competências completas | Reduzir a **uma** família: apenas Estabelecimentos. É a tabela mais valiosa (endereço + situação + CNAE) e sustenta o trabalho sozinha |
| Parser corrompe dados silenciosamente | rejeição > 0,1% ou contagem de linhas divergente | Validação estatística obrigatória no estágio 2 desde a S2; comparar contagens com o minha-receita |
| E1 lento demais mesmo por bucket | um bucket não fecha em < 10 min na S6 | Reduzir para 8 buckets. E1 é baseline de corretude, não precisa ser rápido |
| P2 (sócios) engole o time | S10 sem sócios funcionando | Já está fora do núcleo. Corte seco; vira trabalho futuro |
| Membro do time sai ou fica indisponível | qualquer momento | Frentes acopladas só por Parquet e Kafka; contratos congelados na S1 permitem redistribuir sem retrabalho |
| Receita altera layout ou remove arquivos | qualquer momento | Snapshots congelados com SHA-256 na S1; o experimento não depende de competências futuras |

**Critérios de corte, com data:**

- **21/set (fim da S5)** — sem evento real no Kafka? Corta para uma única entidade e segue.
- **12/out (fim da S8)** — congelamento de escopo. O que não roda vira trabalho futuro, sem
  exceção e sem discussão.
- **02/nov (fim da S11)** — fim do código. As duas últimas semanas são exclusivamente escrita e
  preparação da defesa.

---

## 14. Ameaças à validade

Declaradas na monografia, não escondidas:

- **Representatividade da amostra.** O experimento roda sobre 16 de 256 buckets (~6%). Mitigação:
  como a normalização escreve os 256 buckets, a distribuição de tamanhos é conhecida e a
  aderência da amostra ao todo é verificável e reportada.
- **Ambiente único.** Uma só máquina; os resultados não generalizam entre hardwares distintos.
- **Duas competências apenas.** Sem variação sazonal; a taxa de mudança medida é de um único par
  de meses e pode não representar o comportamento anual.
- **Cache quente.** As medições não isolam efeitos do cache de páginas do SO.
- **Oráculo parcial.** A validação amostral cobre 200 eventos, não o universo; e o próprio
  oráculo (Minha Receita) deriva da mesma fonte, o que limita sua independência.

---

## 15. Trabalhos relacionados

> **Atenção da equipe:** as referências abaixo indicam as áreas e obras a consultar. Toda
> referência deve ser **verificada na fonte original** antes de ser citada formalmente na
> monografia. Não citar nada que não tenha sido lido.

- **Snapshot differencing.** Labio & Garcia-Molina, *Efficient Snapshot Differential Algorithms
  for Data Warehousing* (VLDB, 1996) — âncora teórica direta deste trabalho, propondo algoritmos
  de janela, sort-merge e hash particionado para exatamente este problema. E1 e E3 devem ser
  posicionados explicitamente em relação a essa taxonomia.
- **CDC e event sourcing.** Kleppmann, *Designing Data-Intensive Applications* (O'Reilly, 2017),
  capítulos sobre sistemas derivados e streams; documentação de design do **Debezium**, para o
  vocabulário de envelope `before`/`after` que o contrato adota.
- **Record linkage** (para P2, na expansão). Fellegi & Sunter, *A Theory for Record Linkage*
  (JASA, 1969); Christen, *Data Matching* (Springer, 2012).
- **ETL e qualidade de dados.** Vassiliadis, *A Survey of Extract-Transform-Load Technology*;
  Kimball & Caserta, *The Data Warehouse ETL Toolkit*.
- **Formatos colunares.** Melnik et al., *Dremel* (VLDB, 2010), como fundamentação do modelo de
  armazenamento colunar que o Parquet implementa.
- **Referências de engenharia (estudar, não depender).** `cuducos/minha-receita` (ETL em Go sobre
  os mesmos arquivos; documenta armadilhas reais de parsing);
  `libercapital/dados_publicos_cnpj_receita_federal` (testes agendados que detectam mudança de
  formato — origem da nossa ideia de teste de contrato); `okfn-brasil/receita` (regras de negócio
  do identificador).

---

## 16. Entregáveis

1. **Monografia**, com as seções de resultados sustentadas pelas medições da §9.
2. **Código-fonte aberto**, com licença permissiva e README de reprodução.
3. **Docker Compose** que sobe o ambiente completo, e scripts que reproduzem o experimento
   ponta a ponta a partir dos manifestos de competência.
4. **Manifestos de competência** (arquivo, tamanho, SHA-256, origem, timestamp), garantindo
   reprodutibilidade sem exigir redistribuição dos 85 GB.
5. **Contrato de eventos** publicado como JSON Schema versionado.
6. **Dados de medição brutos** (CSV das execuções) e os scripts de análise que geram os gráficos.

---

## 17. Glossário

| Termo | Significado |
|---|---|
| **Competência** | O mês de referência de um snapshot, no formato `YYYY-MM` |
| **Snapshot** | A fotografia completa e imutável do cadastro em uma competência |
| **Bucket** | Uma das 256 partições determinadas por `murmur3_32(cnpj_basico) mod 256` |
| **ChangeRecord** | Registro interno de diferença, produzido pelo estágio 3 e ainda não tipado |
| **Evento** | O envelope tipado e versionado publicado em Kafka pelo estágio 4 |
| **Motor (E1/E3)** | Implementação de estratégia de diff sob a interface `DiffEngine` |
| **Evento fantasma** | Par falso `excluido`+`incluido` gerado por erro de pareamento de identidade (P2) |
| **Oráculo** | Fonte externa usada como referência de corretude (consulta oficial ou Minha Receita) |
