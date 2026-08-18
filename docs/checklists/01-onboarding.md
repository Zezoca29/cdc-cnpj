# Checklist 01 — Onboarding

Para quem entra no projeto. Leva ~45 minutos e evita os erros que custam semanas.
Marque cada item de fato; não confie na memória.

## Parte 1 — Entender o projeto (20 min)

- [ ] Li o [`README.md`](../../README.md)
- [ ] Li o [`CLAUDE.md`](../../CLAUDE.md) **inteiro**, com atenção às sete regras invioláveis
- [ ] Li a spec: [`docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md`](../superpowers/specs/2026-08-18-cdc-cnpj-design.md)
      — pelo menos as seções 1 a 7 (contexto, pergunta, hipóteses, escopo, arquitetura, contratos)
- [ ] Li o [`docs/ESCOPO.md`](../ESCOPO.md) e sei dizer quantos itens do Núcleo faltam
- [ ] Li o [`docs/STATUS.md`](../STATUS.md) e sei em que semana o projeto está
- [ ] Li o [`docs/CONTRATOS.md`](../CONTRATOS.md)

## Parte 2 — Confirmar que entendi (5 min)

Responda para si mesmo. Se não souber alguma, volte ao documento correspondente:

- [ ] Por que o CNPJ **nunca** pode ser armazenado como número?
- [ ] O que acontece se alguém mudar a função de partição no meio do projeto?
- [ ] Por que o CSV descompactado nunca pode ser gravado em disco?
- [ ] Qual a diferença entre `detected_at` e a data real da mudança no mundo?
- [ ] O que preciso fazer antes de trazer um item da Expansão para o Núcleo?
- [ ] Qual a data do congelamento de escopo e o que acontece nela?

## Parte 3 — Ambiente (20 min)

- [ ] Java 21 instalado (`java --version`)
- [ ] Maven instalado (`mvn --version`)
- [ ] Docker funcionando (`docker --version`)
- [ ] Python 3 disponível (para `scripts/harness_check.py`)
- [ ] Docker Compose sobe Postgres e Kafka sem erro
- [ ] `python scripts/harness_check.py` passa
- [ ] Tenho **pelo menos 250 GB livres** no disco onde os dados vão morar
      (o Parquet das duas competências não cabe em espaço apertado)

## Parte 4 — Assumir uma frente

- [ ] Sei qual frente é minha: **A** ingestão · **B** diff · **C** eventos · **D** bancada
- [ ] Sei em que a minha frente **não** mexe (tabela de fronteiras no `CLAUDE.md` §5)
- [ ] Registrei meu nome na frente correspondente em [`docs/ESCOPO.md`](../ESCOPO.md)
- [ ] Adicionei minha primeira entrada no diário de bordo do [`docs/STATUS.md`](../STATUS.md)

## Parte 5 — Leituras técnicas da sua frente

- [ ] **Frente A:** `cnpj-metadados.pdf` (layout oficial) e o ETL do `cuducos/minha-receita` —
      ler o código antes de escrever o nosso parser economiza semanas
- [ ] **Frente B:** spec §8 (motores) e §10 (validação de corretude)
- [ ] **Frente C:** spec §7 (contratos) e a documentação de design do Debezium (vocabulário de envelope)
- [ ] **Frente D:** spec §9 (protocolo experimental) e §14 (ameaças à validade)

---

**Onboarding concluído?** Vá para [`02-antes-de-codar.md`](02-antes-de-codar.md).
