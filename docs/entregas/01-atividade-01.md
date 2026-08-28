# Atividade 01 — Título, palavras-chave, fichamento de artigos, Introdução e Metodologia

**Projeto:** CDC-CNPJ · **Curso:** Univesp · **Entrega:** Atividade 01 (primeira entrega)
**Data:** 2026-08-18 · **Semana do projeto:** S1 de 13 · **Equipe:** 4 integrantes
**Fonte normativa:** [`docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md`](../superpowers/specs/2026-08-18-cdc-cnpj-design.md) (spec v1.0)

> **Como este documento foi montado.** Título, introdução e metodologia derivam da spec v1.0
> (§1–§3 e §9–§11) e do ADR-001. As referências foram escolhidas para cobrir os quatro eixos que
> o trabalho precisa sustentar na banca: *snapshot differencing*, CDC clássico, armazenamento
> colunar e qualidade de dados em ETL.
>
> **Regra da spec §15 que continua valendo:** nenhuma referência entra na monografia sem ter sido
> lida na fonte original. As fichas abaixo são **ponto de partida para a leitura**, não substituto
> dela. Cada ficha traz um campo *Estado da leitura* — atualize-o conforme o time avançar.

---

## 1. Título

**Título principal (proposto):**

> **Change Data Capture sobre snapshots de dados abertos: um pipeline reprodutível para detecção
> e publicação de eventos de mudança no cadastro do CNPJ**

**Subtítulo alternativo, caso a banca prefira ênfase experimental:**

> *…: comparação entre uma estratégia relacional e uma estratégia particionada por hash em
> hardware de commodity*

**Alternativas descartadas e por quê:**

| Alternativa | Por que não |
|---|---|
| "Big Data aplicado aos dados abertos do CNPJ" | Vago; não diz o que foi construído nem o que foi medido |
| "Pipeline ETL para os dados públicos do CNPJ" | Descreve um ETL comum; esconde a contribuição, que é o *diff* de snapshots e o contrato de eventos |
| "Detecção de mudanças em grandes volumes de dados" | Genérico demais; perde o domínio e a reprodutibilidade, que são os diferenciais |

**Título em inglês (para o *abstract*):**
*Change Data Capture over open-data snapshots: a reproducible pipeline for detecting and
publishing change events in the Brazilian business registry (CNPJ)*

> **Pendência de time:** o título é uma decisão que afeta as quatro frentes. Registrado como
> **ADR-003 (estado: proposto)** em [`docs/DECISOES.md`](../DECISOES.md). Ratificar até 24/ago.

---

## 2. Palavras-chave para busca no Google Acadêmico

A literatura central deste tema é publicada **em inglês**. As buscas em português servem para
contexto nacional (dados abertos, CNPJ) e devem ser tratadas como complementares.

### 2.1 Palavras-chave (8 — o guia pede no mínimo 5)

| # | Português | Inglês (termo de busca real) | Para que serve no trabalho |
|---|---|---|---|
| 1 | Diferenciação de snapshots | `snapshot differential` · `snapshot differencing` | Âncora teórica direta: é exatamente o nosso problema |
| 2 | Captura de dados de mudança | `change data capture` · `CDC` | Área-mãe; posiciona o trabalho e mostra por que a via clássica não se aplica |
| 3 | Detecção de alterações em dados | `change detection` · `delta computation` · `differential refresh` | Sinônimos usados pela literatura de data warehouse |
| 4 | Particionamento por hash | `hash partitioning` · `partitioned hash join` | Fundamenta o motor E3 e a hipótese H1 (memória constante) |
| 5 | Armazenamento colunar | `columnar storage` · `column-oriented` · `record shredding` | Justifica a camada canônica em Parquet |
| 6 | Arquitetura orientada a eventos | `event streaming` · `log-based architecture` · `Apache Kafka` | Fundamenta o estágio 4 e o contrato de eventos |
| 7 | Qualidade de dados em ETL | `data cleaning` · `data quality` · `ETL` | Fundamenta os contadores de rejeição e o limite de 0,1% |
| 8 | Dados abertos governamentais | `open government data` · `open data reuse` | Justificativa social e contexto brasileiro |

*Extra, só para a Expansão (Sócios / problema P2):* `record linkage` · `entity resolution` ·
`data matching`.

### 2.2 Strings de busca prontas (copiar e colar no Google Acadêmico)

```text
"snapshot differential" AND ("data warehouse" OR "change detection")
"change data capture" AND ("transaction log" OR "log-based") AND (evaluation OR benchmark)
("snapshot differencing" OR "delta computation") AND ("hash partitioning" OR "partitioned join")
"columnar storage" AND (Parquet OR "record shredding" OR "nested data")
"event streaming" AND Kafka AND ("exactly-once" OR "at-least-once" OR idempoten*)
"data quality" AND ETL AND ("rejection rate" OR "error detection" OR "data cleaning")
"open government data" AND (reuse OR "data quality") AND Brazil
"record linkage" AND ("entity resolution" OR "data matching") AND blocking
```

**Filtros recomendados:** para os eixos 1, 3 e 7, **não** limite por ano — as obras fundadoras são
de 1996–2009. Para os eixos 2, 5 e 6, filtre **a partir de 2015**, porque a prática mudou muito.

---

## 3. Fichamento dos artigos

Oito fichas, cada uma com **referência**, **metodologia**, **arquitetura** e **limitações** — os
quatro campos pedidos pelo guia —, mais um campo de uso no nosso trabalho.

### Visão geral

| # | Obra | Eixo | Papel no TCC |
|---|---|---|---|
| A1 | Labio & Garcia-Molina (1996) | Snapshot differencing | **Âncora teórica.** E1 e E3 são posicionados na taxonomia deste artigo |
| A2 | Andreakis & Papapanagiotou (2020) | CDC baseado em log | Define o "caminho feliz" do CDC — e mostra por que ele **não se aplica** ao nosso caso |
| A3 | Kreps, Narkhede & Rao (2011) | Streaming de eventos | Fundamenta o estágio 4 e a semântica *at-least-once* (P7) |
| A4 | Melnik et al. (2010) | Armazenamento colunar | Fundamenta a escolha do Parquet como camada canônica |
| A5 | Armbrust et al. (2020) | Lakehouse / changelog | Contraponto: changelog só existe quando se controla a escrita |
| A6 | Vassiliadis (2009) | ETL | Vocabulário e posicionamento do pipeline |
| A7 | Rahm & Do (2000) | Qualidade de dados | Fundamenta contadores de rejeição e o princípio de falha ruidosa (R4) |
| A8 | Fellegi & Sunter (1969) | Record linkage | Só para a **Expansão** (Sócios, problema P2) |

---

### A1 — Labio & Garcia-Molina (1996) · *a referência central deste TCC*

**Referência (ABNT).** LABIO, Wilburt J.; GARCIA-MOLINA, Hector. Efficient snapshot differential
algorithms for data warehousing. In: INTERNATIONAL CONFERENCE ON VERY LARGE DATA BASES (VLDB),
22., 1996, Bombaim. *Proceedings* […]. San Francisco: Morgan Kaufmann, 1996. p. 63–74.
Disponível em: https://www.vldb.org/conf/1996/P063.PDF.

**Metodologia.** Proposição algorítmica acompanhada de modelagem analítica de custo (I/O e
memória) e de avaliação experimental sobre snapshots gerados sinteticamente, variando o grau de
similaridade entre as duas fotografias, a memória disponível e o tamanho do registro.

**Arquitetura.** Trata o *snapshot differential* como componente do processo de manutenção de um
data warehouse cuja fonte não emite log de mudanças: recebe dois arquivos-fotografia e produz o
conjunto de inserções, remoções e atualizações. Apresenta uma família de algoritmos — *outer join*
por ordenação, algoritmos de janela que exploram a similaridade de ordenação entre snapshots
consecutivos, e variantes com particionamento por hash — e usa compressão de registros em
assinaturas curtas para reduzir I/O e memória.

**Limitações.** (i) Pressupõe registros com chave estável e comparação por igualdade exata;
(ii) as assinaturas comprimidas introduzem probabilidade não nula de colisão, isto é, de mudança
não detectada; (iii) a avaliação é predominantemente sintética; (iv) as premissas de hardware são
de 1996 — disco magnético, nó único, memória escassa —, o que torna os números não transponíveis;
(v) a saída é um delta relacional bruto, sem tipagem semântica do evento nem contrato de
publicação; (vi) não trata parsing, encoding nem qualidade do dado de origem.

**Uso no trabalho.** É a obra contra a qual o Capítulo de Trabalhos Relacionados se posiciona.
**E1** (Postgres, `FULL OUTER JOIN` por bucket) e **E3** (hash particionado) devem ser
explicitamente mapeados na taxonomia deste artigo, e as limitações (iii), (v) e (vi) são
exatamente as lacunas que este TCC preenche: dado real, evento tipado e contrato versionado.

**Estado da leitura:** ⚪ a ler na íntegra (PDF aberto e gratuito no link acima).

---

### A2 — Andreakis & Papapanagiotou (2020)

**Referência (ABNT).** ANDREAKIS, Andreas; PAPAPANAGIOTOU, Ioannis. **DBLog: a watermark based
change-data-capture framework**. arXiv:2010.12597, 2020. Disponível em:
https://arxiv.org/abs/2010.12597.

**Metodologia.** Relato de projeto de sistema (*systems paper*) com descrição do algoritmo e
avaliação por experiência operacional em produção, na Netflix, e não por benchmark controlado.

**Arquitetura.** CDC baseado no log de transações do banco de origem, com o problema específico de
combinar o *dump* do estado atual com o fluxo contínuo de eventos do log. A solução são
*watermarks*: o processo grava marcas em uma tabela auxiliar para delimitar uma janela no log,
seleciona a tabela em blocos e deduplica as linhas do bloco contra os eventos daquela janela — o
que permite intercalar dump e log sem travar tabelas nem interromper o log. Alta disponibilidade
por arquitetura ativo-passivo; saída para Kafka ou outro destino.

**Limitações.** (i) **Exige log de transações e permissão de escrita na origem** (a tabela de
watermarks); (ii) implementação acoplada a MySQL e PostgreSQL; (iii) avaliação sem benchmark
público reproduzível; (iv) não se aplica a fontes que publicam apenas fotografias periódicas.

**Uso no trabalho.** É o **contraponto que justifica o problema**. A limitação (i) é precisamente a
condição que a Receita Federal não oferece: não há banco acessível, não há log, não há como
escrever nada na origem. Este artigo permite escrever, na Introdução, a frase "o CDC canônico
supõe X; aqui X não existe" com respaldo bibliográfico em vez de afirmação nossa.

**Estado da leitura:** ⚪ a ler (arXiv, acesso livre).

---

### A3 — Kreps, Narkhede & Rao (2011)

**Referência (ABNT).** KREPS, Jay; NARKHEDE, Neha; RAO, Jun. Kafka: a distributed messaging system
for log processing. In: WORKSHOP ON NETWORKING MEETS DATABASES (NetDB), 6., 2011, Atenas.
*Proceedings* […]. [S. l.: s. n.], 2011. p. 1–7.

**Metodologia.** Descrição de sistema seguida de comparação experimental de vazão de produção e
consumo contra dois sistemas de mensageria estabelecidos, complementada por relato de uso em
produção no LinkedIn.

**Arquitetura.** Tópicos divididos em partições distribuídas entre *brokers*; armazenamento em
arquivos de segmento apenas-anexação; ausência deliberada de metadados por mensagem no broker;
consumidores em modelo *pull*, responsáveis pelo próprio *offset*; uso de `sendfile` para
transferência sem cópia; coordenação e rebalanceamento via ZooKeeper. Entrega **at-least-once**.

**Limitações.** (i) A garantia é *at-least-once* — duplicatas são possíveis em falha, e cabe ao
consumidor lidar com isso; (ii) a ordenação é garantida apenas dentro de uma partição;
(iii) o benchmark é de configuração única e hoje datado; (iv) o artigo não trata de governança de
schema, que é problema real de quem publica eventos.

**Uso no trabalho.** Fundamenta duas decisões de projeto. A limitação (i) é a razão direta de
`event_id` ser determinístico (`uuid_v5`, decisão D3 da spec): idempotência no consumo sai de graça.
A limitação (ii) é a razão de a chave de partição ser `cnpj_basico` — assim a ordenação por empresa
está garantida, que é a única ordenação que o domínio exige. A limitação (iv) é o que o nosso
JSON Schema versionado endereça.

**Estado da leitura:** ⚪ a ler.

---

### A4 — Melnik et al. (2010)

**Referência (ABNT).** MELNIK, Sergey *et al.* Dremel: interactive analysis of web-scale datasets.
*Proceedings of the VLDB Endowment*, v. 3, n. 1–2, p. 330–339, 2010.
DOI: 10.14778/1920841.1920886.

**Metodologia.** Descrição de sistema com avaliação experimental sobre cargas reais de produção do
Google, incluindo comparação de desempenho com uma abordagem MapReduce equivalente.

**Arquitetura.** Formato de armazenamento colunar para dados **aninhados**, com níveis de repetição
e definição que permitem decompor e reconstruir registros sem perda; execução de consultas por
árvore de servidores em múltiplos níveis, com agregação parcial nos nós intermediários.

**Limitações.** (i) Voltado a consultas analíticas de leitura e agregação, não a comparação
registro a registro nem a atualização; (ii) sistema proprietário, avaliado sobre dados internos —
os resultados não são reproduzíveis externamente; (iii) o custo de escrita colunar é maior que o
de escrita em linhas, ponto pouco explorado no artigo.

**Uso no trabalho.** É a fundamentação teórica do **Parquet**, que implementa este modelo em
formato aberto. Sustenta na monografia a escolha da camada canônica: a projeção de colunas permite
ler apenas os campos comparáveis no estágio de diff, e a compressão por coluna é o que faz duas
competências caberem no disco disponível (restrição de 216 GB, spec §6.1). A limitação (i) é
honesta e deve ser declarada: usamos o formato pelo armazenamento, não pelo motor de consulta.

**Estado da leitura:** ⚪ a ler (PDF público no Google Research).

---

### A5 — Armbrust et al. (2020)

**Referência (ABNT).** ARMBRUST, Michael *et al.* Delta Lake: high-performance ACID table storage
over cloud object stores. *Proceedings of the VLDB Endowment*, v. 13, n. 12, p. 3411–3424, 2020.
DOI: 10.14778/3415478.3415560.

**Metodologia.** Descrição de sistema, avaliação com cargas de benchmark padronizadas e relato de
casos de uso em produção.

**Arquitetura.** Camada de log de transações (arquivos JSON com *checkpoints* em Parquet) sobre
arquivos Parquet armazenados em object storage, oferecendo ACID, *time travel*, *merge/upsert* e
consumo em streaming da tabela como fluxo de mudanças.

**Limitações.** (i) O changelog só existe porque o sistema **controla o caminho de escrita** — é
inaplicável quando a origem entrega fotografias opacas; (ii) fortemente orientado a object storage
e ao ecossistema Spark, ambos fora da nossa stack (spec §5.3); (iii) avaliação conduzida pelos
próprios autores/fornecedor; (iv) sobrecarga de metadados relevante em volumes pequenos.

**Uso no trabalho.** Reforça o argumento central da Introdução: as soluções modernas de changelog
pressupõem propriedade sobre a escrita. Quem consome dado público não tem essa propriedade, e por
isso o *snapshot differencing* — tratado como recurso de último caso pela literatura — volta a ser
a única via. Também sustenta a decisão de manter Spark fora do escopo.

**Estado da leitura:** ⚪ a ler.

---

### A6 — Vassiliadis (2009)

**Referência (ABNT).** VASSILIADIS, Panos. A survey of extract–transform–load technology.
*International Journal of Data Warehousing and Mining*, v. 5, n. 3, p. 1–27, 2009.
DOI: 10.4018/jdwm.2009070101.

**Metodologia.** Revisão de literatura, organizando o estado da arte de ETL em torno das fases de
modelagem conceitual, lógica e física, e da otimização de fluxos.

**Arquitetura.** O processo de ETL como grafo de atividades entre fontes e o data warehouse,
cobrindo extração (incluindo o problema de identificar o que mudou na fonte), transformação e
carga, com discussão de otimização e de tratamento de falhas.

**Limitações.** (i) É survey — não produz medição própria; (ii) assume o modelo clássico de janela
noturna de carga em lote, anterior à consolidação de arquiteturas de streaming; (iii) não trata de
contrato de eventos, versionamento de schema nem publicação para consumidores externos;
(iv) volumes considerados são de outra ordem de grandeza.

**Uso no trabalho.** Fornece vocabulário canônico (extração, staging, área de transformação, carga)
para descrever os cinco estágios da nossa arquitetura sem inventar terminologia. A limitação (iii)
delimita a contribuição do TCC: onde o ETL clássico termina na carga, este trabalho segue até o
evento publicado e consumido.

**Estado da leitura:** ⚪ a ler.

---

### A7 — Rahm & Do (2000)

**Referência (ABNT).** RAHM, Erhard; DO, Hong Hai. Data cleaning: problems and current approaches.
*IEEE Data Engineering Bulletin*, v. 23, n. 4, p. 3–13, 2000.

**Metodologia.** Artigo de classificação e revisão: constrói uma taxonomia dos problemas de
qualidade de dados e mapeia as abordagens existentes contra ela.

**Arquitetura.** Separa problemas de fonte única e de múltiplas fontes, e, dentro de cada um,
problemas de nível de schema e de nível de instância. Descreve o *data cleaning* como fluxo em
etapas — análise, definição do fluxo de transformação, verificação, transformação e retorno do
dado limpo à origem.

**Limitações.** (i) Não apresenta avaliação quantitativa; (ii) o foco está na integração de fontes
heterogêneas, não em fotografias sucessivas da mesma fonte; (iii) anterior às ferramentas e escalas
atuais; (iv) não quantifica o efeito de erros de limpeza sobre análises a jusante.

**Uso no trabalho.** Fundamenta a regra **R4 — falhar ruidosamente, nunca mentir baixinho**: os
contadores de linhas lidas e rejeitadas por arquivo e o limite de rejeição de **0,1%** que faz o
estágio falhar. Dá respaldo bibliográfico ao argumento de que diff sobre dado silenciosamente
corrompido produz evento falso, e evento falso invalida o experimento inteiro.

**Estado da leitura:** ⚪ a ler.

---

### A8 — Fellegi & Sunter (1969) · *apenas para a Expansão*

**Referência (ABNT).** FELLEGI, Ivan P.; SUNTER, Alan B. A theory for record linkage. *Journal of
the American Statistical Association*, v. 64, n. 328, p. 1183–1210, 1969.
DOI: 10.1080/01621459.1969.10501049.

**Metodologia.** Trabalho teórico-formal: modela o pareamento de registros como problema de decisão
sobre vetores de comparação e demonstra a otimalidade de uma regra de limiar dados níveis fixos de
erro dos dois tipos.

**Arquitetura.** Espaço de comparação entre pares de registros, padrões de concordância,
probabilidades condicionais de concordância entre pares verdadeiros e falsos, razão de
verossimilhança e três regiões de decisão — parear, não parear e a zona de revisão manual.

**Limitações.** (i) Supõe independência condicional entre os campos comparados, hipótese
frequentemente violada; (ii) a estimação dos parâmetros é difícil sem dado rotulado;
(iii) sem *blocking*, o número de comparações é quadrático; (iv) prevê revisão humana, inviável em
escala automatizada.

**Uso no trabalho.** Só é acionado se a **Expansão X2 (tabela Sócios)** ocorrer. O problema P2 é que
a tabela de sócios não tem chave estável, e sem pareamento correto o pipeline emite pares falsos de
`socio.excluido` + `socio.incluido` — os "eventos fantasma". Se X2 não entrar, esta referência
aparece apenas na seção de trabalhos futuros, justificando por que o corte foi feito.

**Estado da leitura:** ⚪ a ler.

---

### Obras de apoio (livros — não entram como "artigos", mas são citadas)

- KLEPPMANN, Martin. **Designing data-intensive applications.** Sebastopol: O'Reilly, 2017.
  Capítulos sobre sistemas derivados e *streams*: fonte do vocabulário de log como fonte da verdade.
- KIMBALL, Ralph; CASERTA, Joe. **The data warehouse ETL toolkit.** Indianapolis: Wiley, 2004.
- CHRISTEN, Peter. **Data matching.** Berlim: Springer, 2012. (Apoio à A8, só na Expansão.)
- **Debezium** — documentação de projeto. Origem do vocabulário de envelope `before`/`after`
  adotado pelo nosso contrato de eventos. Referência de engenharia, não acadêmica.

---

## 4. Introdução (texto)

O Cadastro Nacional da Pessoa Jurídica é o maior registro empresarial aberto do Brasil. A Receita
Federal o publica mensalmente sob a forma de fotografia completa: dezenas de arquivos compactados
contendo CSVs, algo em torno de 85 GB descompactados, distribuídos sem interface programática de
consulta e sem qualquer indicação do que mudou em relação ao mês anterior. Cada publicação é um
retrato íntegro do cadastro naquele instante, e apenas isso.

A consequência prática é que **não existe, hoje, forma aberta de saber o que mudou**. Quem depende
do delta — jornalismo de dados investigativo, órgãos de controle e fiscalização, pesquisa
acadêmica, análise de risco de crédito, verificação de cadeias de fornecedores — dispõe de duas
alternativas, ambas ruins. A primeira é reprocessar as dezenas de gigabytes por conta própria a
cada mês, o que exige infraestrutura, tempo e competência técnica que a maior parte desses atores
não tem. A segunda é contratar um serviço comercial fechado que faz esse processamento e não
publica nem o método empregado nem os números obtidos, o que transfere para um terceiro a
confiança sobre um dado que é, por definição, público.

A área de sistemas de dados tem um nome para o problema de propagar mudanças a partir de uma
fonte: *Change Data Capture*. A solução canônica lê o log de transações do banco de origem, como
descrevem Andreakis e Papapanagiotou (2020), e as arquiteturas modernas de *lakehouse* (ARMBRUST
et al., 2020) oferecem changelog nativo pela mesma razão — controlam o caminho de escrita do dado.
Nenhuma dessas condições vale aqui. Não há banco acessível, não há log, não há como escrever
qualquer marca na origem: há duas fotografias grandes e opacas. A literatura chama essa situação de
*snapshot differencing* e, desde Labio e Garcia-Molina (1996), a trata predominantemente como
recurso de último caso — a via a que se recorre quando nada melhor está disponível.

Este trabalho parte da observação de que, no universo dos dados abertos governamentais, o "último
caso" **é o caso normal**. Publicação periódica de fotografias completas é o padrão dominante de
disseminação, e não a exceção. Isso justifica tratar o *snapshot differencing* como objeto de
primeira classe: projetar para ele, medi-lo e publicar o método.

A pergunta de pesquisa que orienta a investigação é, portanto: **é viável construir, com recursos
computacionais de commodity, um pipeline reprodutível que converta snapshots mensais do CNPJ em um
fluxo de eventos de mudança semanticamente tipado — e qual o custo comparativo entre uma estratégia
relacional e uma estratégia particionada por hash para o cálculo desse delta?**

O objetivo geral é projetar, implementar e avaliar experimentalmente um protótipo desse pipeline,
publicando método, código e medições de forma aberta e reprodutível. Dele decorrem seis objetivos
específicos: (1) caracterizar quantitativamente os snapshots, em volume por tabela, distribuição de
registros por partição e taxa real de mudança mensal por tipo de evento; (2) especificar uma camada
canônica em formato colunar, com schema explícito e versionado, resistente às armadilhas conhecidas
da fonte — codificação legada, CSV posicional com delimitadores embutidos, particionamento instável
de arquivos e o CNPJ alfanumérico vigente desde julho de 2026; (3) implementar e comparar dois
motores de diff sob uma mesma interface, um baseline relacional e um particionado por hash;
(4) especificar um contrato de eventos versionado, de semântica inspirada no vocabulário Debezium,
e publicá-lo em Apache Kafka; (5) medir tempo, memória de pico, I/O, vazão e custo projetado de cada
motor sob protocolo experimental documentado e repetível; e (6) validar a corretude dos eventos sem
dispor de gabarito oficial.

Esse último objetivo merece destaque porque define a principal dificuldade metodológica do trabalho:
**não existe delta verdadeiro publicado contra o qual conferir o resultado**. A corretude precisa,
então, ser construída — por validação cruzada entre duas implementações independentes, por
amostragem contra oráculo externo e por invariantes de sanidade sobre os agregados. A metodologia
dessa validação é, ela própria, uma contribuição do trabalho, e é o que separa este protótipo de um
script que produz números plausíveis sem que ninguém possa dizer se estão certos.

As contribuições esperadas são quatro: um pipeline funcional e aberto que converte snapshots do CNPJ
em eventos de mudança; um contrato de eventos versionado e publicado; medições comparativas entre
duas estratégias de diff em hardware de commodity, com protocolo repetível; e a caracterização
empírica da taxa mensal de mudança do cadastro, que não está publicamente documentada.

O restante da monografia está organizado da seguinte forma. O Capítulo 2 revisa a literatura de
*snapshot differencing*, CDC, armazenamento colunar e qualidade de dados em ETL. O Capítulo 3
apresenta a metodologia e o protocolo experimental. O Capítulo 4 descreve a arquitetura do pipeline
e as decisões congeladas que a sustentam. O Capítulo 5 trata da estratégia de validação de
corretude. O Capítulo 6 apresenta e discute os resultados. O Capítulo 7 declara as ameaças à
validade, e o Capítulo 8 conclui e aponta trabalhos futuros.

---

## 5. Metodologia (texto)

### 5.1 Classificação da pesquisa

Trata-se de pesquisa **aplicada**, de natureza **quantitativa** e **experimental**, conduzida por
meio de construção de artefato — um protótipo funcional de pipeline — seguida de avaliação empírica
sob protocolo controlado. As hipóteses são declaradas antes da execução e formuladas de modo
**falsificável**: um resultado negativo é resultado publicável, desde que os números expliquem o
porquê.

Três hipóteses orientam a avaliação. **H1**: o particionamento estável por
`hash(cnpj_basico) mod N` permite executar o diff de snapshots multi-gigabyte com memória de pico
aproximadamente constante em relação ao volume total, limitada pelo maior bucket; aceita-se H1 se a
memória de pico do motor particionado variar menos de 20% entre processar 1 e 16 buckets enquanto a
do baseline cresce monotonicamente. **H2**: um ciclo completo — ingestão, normalização, diff e
publicação — de uma competência é executável em menos de 24 horas em hardware de commodity (até
8 vCPU, 32 GB de RAM, SSD). **H3**: a estratégia particionada apresenta speedup mediano de ao menos
2,0 sobre o baseline relacional, para o mesmo conjunto de buckets, ao longo de 5 repetições.

### 5.2 Objeto e materiais

O objeto empírico são duas competências consecutivas dos Dados Abertos do CNPJ, **2026-06** e
**2026-07**. As competências são congeladas logo na primeira semana: cada arquivo tem seu SHA-256
calculado e registrado em um `manifesto.json` versionado no repositório, junto de nome, tamanho,
origem e data de coleta. Esse congelamento cumpre dois papéis — garante que o experimento seja
reproduzível sem exigir a redistribuição de dezenas de gigabytes, e o protege contra alterações
posteriores de layout ou remoção de arquivos pela fonte, algo que já ocorreu com projetos da
comunidade em anos anteriores.

Escopo de dados do núcleo: as tabelas **Empresas** e **Estabelecimentos** e as tabelas de domínio
(CNAEs, Municípios, Motivos, Naturezas Jurídicas, Qualificações e Países). As tabelas de Sócios e do
Simples ficam fora do núcleo por decisão de escopo registrada, e retornam apenas se a fase de
expansão ocorrer.

A stack é fixa: Java 21, Spring Boot, Apache Parquet como camada canônica, Apache Kafka para os
eventos com JSON Schema versionado, PostgreSQL para o motor baseline e para o changelog do consumer,
Micrometer para instrumentação e Maven como ferramenta de construção.

### 5.3 Procedimentos

O pipeline é organizado em cinco estágios, acoplados apenas por arquivos Parquet e tópicos Kafka.

**Ingestão.** Download das duas competências, cálculo de SHA-256 por arquivo e produção do manifesto.
Um teste de contrato valida o layout dos arquivos e falha ruidosamente caso a fonte tenha mudado.

**Normalização.** Cada ZIP é lido em streaming e convertido diretamente em Parquet, **sem jamais
materializar o CSV descompactado em disco**. Essa não é uma otimização, e sim um requisito de
viabilidade: a máquina de referência dispõe de 216 GB livres e as duas competências descompactadas
ocupariam cerca de 170 GB, sem contar o Parquet resultante. O schema é explícito e versionado, nunca
inferido. Todo campo de código permanece string, porque zeros à esquerda são significativos, e o
próprio CNPJ é string em toda a stack — desde 31 de julho de 2026 novas inscrições podem conter
letras nas oito primeiras posições, o que invalida qualquer tratamento numérico e qualquer
particionamento por faixa. A saída é particionada em 256 buckets por
`murmur3_32(utf8(cnpj_basico)) mod 256`, função escolhida por ser estável entre execuções,
plataformas e versões de JVM.

**Diff.** Dois motores implementam a mesma interface e produzem o mesmo tipo de registro de mudança
a partir de um par de snapshots e um identificador de bucket. O motor **E1** carrega os dois lados de
um bucket em tabelas temporárias no PostgreSQL e executa `FULL OUTER JOIN` pela chave natural;
existe para ser obviamente correto, não para ser rápido, e opera sempre bucket a bucket, de modo que
o banco nunca veja o conjunto inteiro. O motor **E3** calcula um hash de conteúdo por registro,
compara os dois lados do bucket em memória por chave natural e classifica cada diferença em
inserção, remoção ou alteração, comparando campo a campo apenas no último caso para preencher a
lista de campos alterados; os buckets são processados em paralelo.

**Eventos e consumo.** Cada registro de mudança é convertido em um envelope tipado e versionado,
publicado em Kafka com chave igual ao CNPJ básico, o que garante ordenação por empresa. A semântica
de entrega é *at-least-once*; a idempotência no consumo é obtida por um `event_id` determinístico,
gerado como UUID versão 5 sobre a concatenação de competência de origem, competência de destino,
chave da entidade, tipo de evento e lista ordenada de campos alterados. Um consumer materializa o
changelog em PostgreSQL, servindo simultaneamente como prova de utilidade e como ferramenta de
inspeção da própria equipe.

### 5.4 Protocolo experimental

**Variáveis independentes:** motor de diff (E1, E3) e escala, medida em número de buckets
processados (1, 4, 8 e 16). **Variáveis dependentes:** tempo de parede, memória de pico (RSS e
heap), bytes lidos e escritos, vazão em eventos por segundo e tempo de CPU. **Variáveis
controladas:** mesma máquina, mesmo par de snapshots, mesmos buckets e ausência de carga
concorrente.

O ambiente é integralmente documentado — modelo e número de núcleos da CPU, memória, tipo de disco,
sistema operacional, versão e flags da JVM, versões de PostgreSQL e Kafka. O pipeline é executado
**nativo na JVM**; apenas PostgreSQL e Kafka rodam em contêiner. A razão é metodológica: o Docker
Desktop sobre WSL2 acrescenta overhead de I/O real e variável, que contaminaria justamente as
medições do estágio de normalização, o mais sensível a disco.

Cada configuração é executada **cinco vezes, descartando-se a primeira**, para absorver o
aquecimento do compilador JIT e do cache do sistema operacional. Reporta-se **mediana e amplitude
interquartil**, nunca a média: distribuições de tempo dominadas por I/O são assimétricas e a média
descreve mal a tendência central. O estado do cache de páginas é declarado explicitamente — no
Windows, limpá-lo de forma confiável entre execuções é impraticável, de modo que todas as medições
ocorrem com **cache quente**, e o texto diz isso em vez de omitir.

Três medidas de ganho são calculadas. O **speedup**, razão entre as medianas de tempo dos dois
motores sobre os mesmos buckets. A **escalabilidade de memória**, isto é, a memória de pico em
função do número de buckets processados; se o motor particionado permanecer aproximadamente
constante enquanto o baseline cresce, este é o resultado mais forte do trabalho, mais relevante que
o speedup, porque é o que sustenta a afirmação de que o processamento cabe em máquina comum. E o
**custo projetado**, extrapolação de 16 para 256 buckets convertida em custo equivalente de
instância em nuvem, com intervalo de confiança.

O experimento roda sobre 16 dos 256 buckets, cerca de 6% do total. A normalização, porém, escreve
os 256, o que torna conhecida a distribuição completa de tamanhos e permite **verificar e reportar**
a aderência da amostra ao todo, em vez de apenas supô-la.

### 5.5 Validação de corretude

Como não existe gabarito oficial do diff verdadeiro, a corretude é construída em três camadas
independentes.

A primeira é a **validação cruzada entre implementações**: E1 e E3 devem produzir exatamente o mesmo
conjunto de eventos. A métrica é a cardinalidade da diferença simétrica dos conjuntos de `event_id`
dividida pela cardinalidade da união, e a meta é zero. É o determinismo do `event_id` que torna essa
comparação uma operação de conjuntos, em vez de uma comparação semântica campo a campo. A validação
roda como teste automatizado, não apenas como experimento, e qualquer divergência é bug de uma das
implementações e bloqueia o marco.

A segunda é a **validação amostral contra oráculo externo**: 200 eventos sorteados aleatoriamente
com semente registrada são conferidos contra consulta pública, reportando-se a taxa de concordância
e analisando-se individualmente cada desacordo.

A terceira são as **invariantes de sanidade** sobre agregados — por exemplo, a diferença entre a
contagem de eventos de criação e de encerramento de empresas deve aproximar a variação do número de
registros entre as duas competências, dentro de tolerância declarada. Violação de invariante bloqueia
a publicação dos resultados.

Sustentando as três camadas, o estágio de normalização mantém contadores de linhas lidas e
rejeitadas por arquivo, comparados com a contagem esperada. Taxa de rejeição acima de **0,1%** faz o
estágio falhar. O princípio é falhar ruidosamente em vez de mentir baixinho: diff sobre dado
corrompido gera evento falso, e evento falso invalida o experimento inteiro.

### 5.6 Ameaças à validade

Cinco ameaças são declaradas abertamente. A **representatividade da amostra**, já que o experimento
cobre 16 dos 256 buckets — mitigada, mas não eliminada, pela verificação de aderência descrita em
5.4. O **ambiente único**, uma só máquina, o que impede generalizar entre hardwares distintos. O uso
de **apenas duas competências**, que não captura variação sazonal da taxa de mudança. O **cache
quente**, que impede isolar efeitos do cache de páginas do sistema operacional. E o **oráculo
parcial**, pois a validação amostral cobre 200 eventos e não o universo, e o próprio oráculo deriva
da mesma fonte de dados, o que limita sua independência.

---

## 6. Referências

ANDREAKIS, Andreas; PAPAPANAGIOTOU, Ioannis. **DBLog: a watermark based change-data-capture
framework**. arXiv:2010.12597, 2020.

ARMBRUST, Michael *et al.* Delta Lake: high-performance ACID table storage over cloud object stores.
*Proceedings of the VLDB Endowment*, v. 13, n. 12, p. 3411–3424, 2020.

CHRISTEN, Peter. **Data matching**: concepts and techniques for record linkage, entity resolution,
and duplicate detection. Berlim: Springer, 2012.

FELLEGI, Ivan P.; SUNTER, Alan B. A theory for record linkage. *Journal of the American Statistical
Association*, v. 64, n. 328, p. 1183–1210, 1969.

KIMBALL, Ralph; CASERTA, Joe. **The data warehouse ETL toolkit**. Indianapolis: Wiley, 2004.

KLEPPMANN, Martin. **Designing data-intensive applications**. Sebastopol: O'Reilly, 2017.

KREPS, Jay; NARKHEDE, Neha; RAO, Jun. Kafka: a distributed messaging system for log processing.
In: WORKSHOP ON NETWORKING MEETS DATABASES (NetDB), 6., 2011, Atenas. *Proceedings* […].
[S. l.: s. n.], 2011. p. 1–7.

LABIO, Wilburt J.; GARCIA-MOLINA, Hector. Efficient snapshot differential algorithms for data
warehousing. In: INTERNATIONAL CONFERENCE ON VERY LARGE DATA BASES (VLDB), 22., 1996, Bombaim.
*Proceedings* […]. San Francisco: Morgan Kaufmann, 1996. p. 63–74.

MELNIK, Sergey *et al.* Dremel: interactive analysis of web-scale datasets. *Proceedings of the
VLDB Endowment*, v. 3, n. 1–2, p. 330–339, 2010.

RAHM, Erhard; DO, Hong Hai. Data cleaning: problems and current approaches. *IEEE Data Engineering
Bulletin*, v. 23, n. 4, p. 3–13, 2000.

VASSILIADIS, Panos. A survey of extract–transform–load technology. *International Journal of Data
Warehousing and Mining*, v. 5, n. 3, p. 1–27, 2009.

---

## 7. Pendências antes de submeter

| # | Pendência | Responsável | Prazo |
|---|---|---|---|
| 1 | **Ler as 8 obras na fonte original.** Regra da spec §15: não se cita o que não foi lido | dividir 2 por integrante | 24/ago |
| 2 | Ratificar o título (ADR-003 está como *proposto*) | todas as frentes | 24/ago |
| 3 | Conferir cada referência ABNT contra a norma exigida pela Univesp | — | antes da submissão |
| 4 | Preencher nomes completos e RAs dos 4 integrantes no cabeçalho | — | antes da submissão |
| 5 | Ajustar formatação ao modelo oficial da Univesp (fonte, espaçamento, capa) | — | antes da submissão |
| 6 | Confirmar a afirmação "a taxa mensal de mudança não está publicamente documentada" (§4) ou suavizá-la | — | antes da submissão |
