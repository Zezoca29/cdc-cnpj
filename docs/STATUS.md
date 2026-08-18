# STATUS — CDC-CNPJ

**Última atualização:** 2026-08-18 · **Semana:** S1 de 13 · **Dias até a entrega:** 90

> **Atualize este arquivo ao fim de cada sessão de trabalho.** Não no fim da semana, não na
> véspera do marco: ao fim da sessão. Uma linha honesta hoje vale mais que um relatório
> completo depois que o problema já custou caro.
>
> Se você trabalhou e não atualizou aqui, para as outras três frentes o trabalho não existiu.

---

## Semáforo do projeto

| Frente | Estado | Comentário |
|---|---|---|
| **A** — Ingestão e normalização | ⚪ não iniciada | Download precisa começar na S1 — é o caminho crítico |
| **B** — Motores de diff | ⚪ não iniciada | Depende de contratos v1 (S1) e fixtures (D1) |
| **C** — Eventos e Kafka | ⚪ não iniciada | Depende do envelope congelado (S1) |
| **D** — Bancada e análise | ⚪ não iniciada | D1 (gerador sintético) destrava B e C — prioridade máxima na S2 |
| **Monografia** | ⚪ não iniciada | Escrita corre em paralelo desde a S1. Não é opcional. |

Legenda: 🟢 no prazo · 🟡 atenção · 🔴 em risco · ⚪ não iniciada

---

## Semana atual — S1 (18–24/ago/2026)

**Tema:** semana conjunta. Ninguém trabalha isolado; o objetivo é congelar os contratos para
que a S2 possa correr em paralelo real.

**Objetivo da semana:**
- [ ] Ler `cnpj-metadados.pdf` (layout oficial) e o ETL do `cuducos/minha-receita`
- [ ] Congelar em `docs/CONTRATOS.md`: schema Parquet canônico, envelope de evento, função de partição
- [ ] Iniciar o download das competências 2026-06 e 2026-07 (roda em segundo plano a semana toda)
- [ ] Subir o Docker Compose com Postgres e Kafka
- [ ] Definir quem assume cada frente (A, B, C, D)

**Marco da semana:** contratos v1 congelados + fixtures sintéticas disponíveis.

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

### 2026-08-18 · — · setup

Repositório criado: **https://github.com/Zezoca29/cdc-cnpj** (privado, branch `main`).
Vira público na defesa, conforme decidido. Falta adicionar os outros 3 integrantes como
colaboradores — sem isso ninguém mais consegue clonar.

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
| Contratos v1 + fixtures | 24/ago | ⚪ pendente | — |
| Um bucket ponta a ponta (sintético) | 07/set | ⚪ pendente | — |
| **Primeiro evento REAL no Kafka** | **21/set** | ⚪ pendente | — |
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
| Introdução e contexto | spec §1–2 | S3 | ⚪ |
| Metodologia | spec §9 | S3 | ⚪ |
| Arquitetura | spec §6–7 | S5 | ⚪ |
| Corretude e validação | spec §10 | S7 | ⚪ |
| Resultados | medições da S8 | S8 | ⚪ |
| Trabalhos relacionados | spec §15 | S9 | ⚪ |
| Ameaças à validade | spec §14 | S10 | ⚪ |
| Conclusão e trabalhos futuros | — | S12 | ⚪ |
