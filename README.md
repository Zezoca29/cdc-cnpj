# CDC-CNPJ

Transformar o maior cadastro empresarial aberto do Brasil — hoje disponível apenas como
fotografia mensal de ~85 GB — em um **changelog de eventos** que qualquer sistema consome,
medindo cientificamente o custo de cada estratégia de fazê-lo, e publicando tudo aberto.

TCC · Univesp · ago–nov/2026 · equipe de 4 pessoas

---

## Comece por aqui

**Novo no projeto?** Percorra [`docs/checklists/01-onboarding.md`](docs/checklists/01-onboarding.md).
Leva ~45 minutos e evita os erros que custam semanas.

**Já fez o onboarding?** Antes de cada sessão de trabalho:

```bash
python scripts/harness_check.py
```

Depois percorra [`docs/checklists/02-antes-de-codar.md`](docs/checklists/02-antes-de-codar.md).

---

## O problema em três parágrafos

A Receita Federal publica o CNPJ inteiro todo mês, como ZIPs de CSV. Não há API, não há delta,
não há log de transações. Quem precisa saber **o que mudou** tem duas opções: reprocessar 85 GB
por conta própria, ou pagar por um serviço fechado que não publica nem o método nem os números.

CDC (Change Data Capture) clássico resolveria isso lendo o log do banco de origem. Aqui não há
banco: há duas fotografias gigantes. A literatura chama isso de **snapshot differencing** e
historicamente trata como último recurso.

Este projeto trata como objeto de primeira classe: constrói o pipeline, publica os eventos, e
mede o custo comparativo de duas estratégias de diff — uma relacional e uma particionada por
hash — em hardware de commodity.

## O pipeline

```
ZIPs mensais  ->  Parquet canônico  ->  diff (E1 | E3)  ->  eventos  ->  changelog
   ingestão        normalização          motores            Kafka        consumer
   SHA-256         256 buckets           por bucket         JSON Schema   Postgres
```

## Stack

Java 21 · Spring Boot · Apache Parquet · Apache Kafka · PostgreSQL · Docker Compose · Maven

```powershell
# Infra local (Postgres + Kafka)
docker compose up -d

# Fixtures sintéticas
mvn -pl cdc-cnpj-bench exec:java

# Testes
mvn -pl cdc-cnpj-common,cdc-cnpj-bench,cdc-cnpj-diff test
```

Ver [`infra/README.md`](infra/README.md) e [`cdc-cnpj-bench/fixtures/README.md`](cdc-cnpj-bench/fixtures/README.md).

O pipeline roda **nativo na JVM**; só Postgres e Kafka são conteinerizados, porque o overhead
de I/O do Docker sobre WSL2 contaminaria as medições.

## As quatro frentes

| Frente | Responsabilidade |
|---|---|
| **A** | Ingestão, manifesto, parser, normalização para Parquet |
| **B** | Motores de diff E1 (baseline relacional) e E3 (hash particionado) |
| **C** | Contrato de eventos, produtor Kafka, consumer changelog |
| **D** | Bancada de medição, gerador sintético, análise |

As frentes se comunicam **apenas por arquivos Parquet e tópicos Kafka**.

## Documentos

| Arquivo | O que é |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | **Regras invioláveis.** Leia antes de codar |
| [`docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md`](docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md) | **Fonte da verdade.** Design, hipóteses, protocolo experimental |
| [`docs/ESCOPO.md`](docs/ESCOPO.md) | Escopo vivo — atualize ao concluir tarefa |
| [`docs/STATUS.md`](docs/STATUS.md) | Estado semanal — atualize a cada sessão |
| [`docs/CONTRATOS.md`](docs/CONTRATOS.md) | Contratos congelados |
| [`docs/DECISOES.md`](docs/DECISOES.md) | Registro de decisões (ADR) |
| [`docs/checklists/`](docs/checklists/) | Onboarding, antes de codar, antes do PR, fechamento de marco |

## Datas que não se movem

| Data | O quê |
|---|---|
| 21/set | Primeiro evento real no Kafka — se não houver, corta para uma entidade |
| **12/out** | **Congelamento de escopo.** O que não roda vira trabalho futuro |
| **02/nov** | **Fim do código.** Só escrita e defesa a partir daqui |
| 16/nov | Entrega |

## As sete regras, em uma linha cada

1. **CNPJ é String.** Sempre, em todo lugar. Nunca número.
2. **Função de partição congelada:** `murmur3_32 mod 256`. Nunca `hashCode()`.
3. **Nunca gravar CSV descompactado em disco.** Streaming direto para Parquet.
4. **Falhar ruidosamente, nunca mentir baixinho.** Contadores de rejeição obrigatórios.
5. **Snapshot é imutável.** Reprocessa-se da origem; nunca se conserta no lugar.
6. **`event_id` é determinístico.** Nunca `randomUUID()`.
7. **Dependência nova só com ADR.**

O detalhe de cada uma está no [`CLAUDE.md`](CLAUDE.md).
