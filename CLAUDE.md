# CDC-CNPJ — Regras do Projeto

**Leia este arquivo inteiro antes de escrever qualquer linha de código.** Ele vale para
pessoas e para agentes de IA. São ~5 minutos que evitam semanas de retrabalho.

Prazo total: **18/ago/2026 a 16/nov/2026**. Time de 4 pessoas, ~9 h/semana cada.
Não há folga no cronograma. Erro que exige refazer a normalização custa o projeto.

---

## 1. Antes de começar QUALQUER tarefa

Nesta ordem, sem pular:

1. Leia [`docs/ESCOPO.md`](docs/ESCOPO.md) — confirme que sua tarefa está no **Núcleo** e
   que o item não está bloqueado.
2. Leia [`docs/STATUS.md`](docs/STATUS.md) — saiba em que semana o projeto está e o que as
   outras frentes estão fazendo agora.
3. Leia [`docs/CONTRATOS.md`](docs/CONTRATOS.md) — os contratos são **congelados**. Seu código
   se adapta a eles, nunca o contrário.
4. Rode `python scripts/harness_check.py`. Se falhar, **corrija antes de codar**.
5. Percorra [`docs/checklists/02-antes-de-codar.md`](docs/checklists/02-antes-de-codar.md).

Se você é um agente de IA: os passos acima não são opcionais e não podem ser resumidos a
partir do que você "já sabe" do projeto. Leia os arquivos.

## 2. Ao terminar QUALQUER tarefa — obrigatório

Nenhuma tarefa está concluída sem isto. Uma entrega sem estes três passos é uma entrega
incompleta, mesmo que o código funcione:

1. **Atualize [`docs/ESCOPO.md`](docs/ESCOPO.md)** — mude o estado do item que você tocou
   (`[ ]` pendente, `[~]` em andamento, `[x]` concluído, `[!]` bloqueado).
2. **Atualize [`docs/STATUS.md`](docs/STATUS.md)** — o que avançou, o que travou, o que a
   próxima pessoa precisa saber. Uma linha honesta vale mais que um parágrafo otimista.
3. **Registre decisões em [`docs/DECISOES.md`](docs/DECISOES.md)** — se você decidiu algo que
   outra pessoa poderia razoavelmente ter decidido diferente, registre. Se mudou algo em
   `CONTRATOS.md`, o ADR é obrigatório.

Depois percorra [`docs/checklists/03-antes-do-pr.md`](docs/checklists/03-antes-do-pr.md).

---

## 3. As sete regras invioláveis

Quebrar qualquer uma destas custa dias de retrabalho. Elas vêm da spec
[`docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md`](docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md)
e não podem ser alteradas sem ADR aprovado por todas as quatro frentes.

### R1. CNPJ é STRING. Sempre. Em todo lugar.

Nunca `long`, `int`, `BIGINT`, `Integer.parseInt`. Desde 31/07/2026 o CNPJ pode conter letras
nas 8 primeiras posições. Ordenação é lexicográfica. O dígito verificador usa a regra do
formato misto (valor do caractere = ASCII − 48).

Isto vale também para: chave do Kafka, coluna do Postgres (`VARCHAR(14)`), campos de código
(CNAE, município, natureza jurídica, situação cadastral — zeros à esquerda são significativos).

### R2. A função de partição é congelada.

`bucket(cnpj_basico) = murmur3_32(utf8(cnpj_basico)) mod 256`, normalizada para não-negativo.

**Nunca use `String.hashCode()`.** Se esta função mudar, todo o Parquet já normalizado vira
lixo incomparável e a frente A perde semanas. Se você acha que precisa mudá-la, pare e abra
um ADR.

### R3. Nunca materialize o CSV descompactado em disco.

Restrição física: 216 GB livres, e o CSV das duas competências ocuparia ~170 GB. A normalização
lê o ZIP em streaming e escreve Parquet direto. Qualquer código que faça `unzip` para um
arquivo intermediário está errado e vai encher o disco de todo mundo.

### R4. Falhar ruidosamente, nunca mentir baixinho.

Dado corrompido gera evento falso, e evento falso invalida o experimento inteiro. Todo estágio
de parsing conta linhas lidas, linhas rejeitadas e compara com o esperado. Rejeição acima de
**0,1%** faz o estágio falhar. Nunca use `try { ... } catch { /* ignora */ }` em parsing.

### R5. Snapshot é imutável; mudança é evento.

Nada no pipeline reescreve um snapshot já normalizado. Se algo deu errado, reprocessa-se a
partir do ZIP original (que tem SHA-256 registrado no manifesto). Nunca "conserte" um Parquet
no lugar.

### R6. `event_id` é determinístico.

`uuid_v5(namespace, competencia_de | competencia_para | entity_key | event_type | campos_alterados_ordenados)`.

Nunca `UUID.randomUUID()`. Isto é o que dá idempotência ao consumo E o que faz a validação
E1≡E3 ser uma comparação de conjuntos em vez de dias de trabalho.

### R7. Não instale dependência nova sem ADR.

A stack está definida (§4). Cada dependência nova é superfície de risco e tempo de setup
multiplicado por 4 pessoas. Se acha que precisa de uma, abra ADR explicando o que ela resolve
que a stack atual não resolve.

---

## 4. Stack fixa

Java 21 · Spring Boot (ingestão e consumer) · Apache Parquet (camada canônica) ·
Apache Kafka (eventos, **sem** Schema Registry — usamos JSON Schema versionado) ·
PostgreSQL (motor E1 e changelog do consumer) · Docker Compose (só Postgres e Kafka) ·
Micrometer (métricas) · Maven.

**O pipeline roda nativo na JVM, não em contêiner.** Docker Desktop sobre WSL2 adiciona
overhead de I/O variável que contaminaria as medições. Só Postgres e Kafka são conteinerizados.

**Fora da stack, por decisão registrada:** Apache Spark, Schema Registry, Avro, Kotlin,
Gradle. Não introduza.

---

## 5. As quatro frentes

| Frente | Responsabilidade | Não mexe em |
|---|---|---|
| **A** | Ingestão, download, manifesto, parser, normalização para Parquet | motores de diff, Kafka |
| **B** | Motores E1 e E3 sob a interface `DiffEngine` | ingestão, produtor Kafka |
| **C** | Contrato de eventos, produtor Kafka, consumer changelog | parser, motores |
| **D** | Bancada de medição, gerador sintético, análise, gráficos | tudo o mais — D mede, não implementa pipeline |

As frentes se comunicam **apenas por arquivos Parquet e tópicos Kafka**. Se você precisa
chamar código de outra frente diretamente, provavelmente está resolvendo o problema no lugar
errado — converse antes de acoplar.

---

## 6. O que fazer quando algo dá errado

| Situação | O que fazer |
|---|---|
| O contrato não atende o meu caso | **Não altere o contrato.** Abra ADR em `docs/DECISOES.md`, avise as 4 frentes, espere. Contrato quebrado no meio do projeto para todo mundo. |
| Minha tarefa está maior do que parecia | Registre em `STATUS.md` **hoje**, não na véspera do marco. O plano tem critérios de corte justamente para isso. |
| Quero implementar algo da Expansão | Não. Confira `ESCOPO.md`: nada da Expansão começa antes do Núcleo inteiro estar `[x]`. |
| Achei um jeito melhor de fazer X | Ótimo — registre como ADR **com o custo em horas**. Se custa mais de 4 h e não está no Núcleo, provavelmente é trabalho futuro. |
| O teste E1≡E3 falhou | **Isso bloqueia o marco.** Não contorne, não marque como flaky. Uma das duas implementações tem bug e precisa ser encontrado. |
| Estou travado há mais de 2 h | Escreva em `STATUS.md` e peça ajuda. Com 9 h/semana, 2 h travado é 22% da sua semana. |

---

## 7. Convenções de código

- Código, identificadores, nomes de arquivo e mensagens de commit em **inglês**.
  Documentação e comentários de domínio em **português**.
- Um teste de propriedade vale mais que dez unitários — priorize.
- Nada de números mágicos: `256`, `16`, `0.1%`, `200` vêm de constantes nomeadas que
  referenciam a seção da spec.
- Build e teste rodam **no módulo**, nunca na raiz.

---

## 8. Mapa dos documentos

| Arquivo | O que é | Frequência de atualização |
|---|---|---|
| [`docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md`](docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md) | **Fonte da verdade.** Design completo, hipóteses, protocolo | Raramente; só por ADR |
| [`docs/ESCOPO.md`](docs/ESCOPO.md) | Escopo vivo com estado de cada item | **A cada tarefa concluída** |
| [`docs/STATUS.md`](docs/STATUS.md) | Estado semanal, marcos, bloqueios | **A cada sessão de trabalho** |
| [`docs/CONTRATOS.md`](docs/CONTRATOS.md) | Contratos congelados (schema, evento, hash) | Só por ADR |
| [`docs/DECISOES.md`](docs/DECISOES.md) | Registro de decisões (ADR) | Quando houver decisão |
| [`docs/checklists/`](docs/checklists/) | Checklists de onboarding, código, PR e marco | Raramente |

Em caso de divergência entre documentos, **a spec vence**.
