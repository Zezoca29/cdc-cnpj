# Checklist 02 — Antes de codar

Percorra isto **no início de cada sessão de trabalho**. Leva 3 minutos.

## Contexto (obrigatório, sem pular)

- [ ] Rodei `python scripts/harness_check.py` e passou
- [ ] Li o [`docs/STATUS.md`](../STATUS.md) — sei o que mudou desde a minha última sessão
- [ ] Confirmei em [`docs/ESCOPO.md`](../ESCOPO.md) que a minha tarefa está no **Núcleo**
- [ ] A tarefa não está bloqueada (`[!]`) e nenhuma dependência dela está pendente
- [ ] Marquei a tarefa como `[~]` em andamento no `ESCOPO.md`

## A tarefa é legítima?

- [ ] Está no Núcleo. **Se está na Expansão: pare.** Nada da Expansão começa antes do Núcleo
      inteiro estar `[x]`
- [ ] Está na minha frente. Se toca outra frente, conversei antes
- [ ] Não exige alterar [`docs/CONTRATOS.md`](../CONTRATOS.md). Se exige: abra ADR primeiro e
      espere aprovação das quatro frentes — **não comece a codar antes**
- [ ] Não exige dependência nova. Se exige: ADR primeiro (R7)

## As sete regras — releia se for tocar nestes pontos

- [ ] **R1** Vou tratar CNPJ? Então é `String`. Nunca `long`/`int`/`BIGINT`/`parseInt`
- [ ] **R2** Vou calcular bucket? `murmur3_32 mod 256`. Nunca `String.hashCode()`
- [ ] **R3** Vou ler ZIP? Streaming direto para Parquet. Nunca gravar CSV intermediário
- [ ] **R4** Vou parsear? Contadores de linha lida/rejeitada. Nunca `catch` silencioso
- [ ] **R5** Vou escrever dado? Snapshot é imutável. Nunca reescrever Parquet no lugar
- [ ] **R6** Vou gerar `event_id`? `uuid_v5` determinístico. Nunca `randomUUID()`
- [ ] **R7** Dependência nova? ADR primeiro

## Teste antes do código

- [ ] Sei qual teste prova que a tarefa está pronta, e vou escrevê-lo primeiro
- [ ] Se a tarefa produz eventos: existe um **teste de propriedade** cobrindo-a
      (par de snapshots sintéticos com mudanças conhecidas → conjunto de eventos esperado)
- [ ] Se a tarefa toca E1 ou E3: o teste de equivalência E1 ≡ E3 continua rodando

## Orçamento

- [ ] Estimei quantas horas isto leva
- [ ] Se passar de **2× a estimativa**, vou registrar em `STATUS.md` e pedir ajuda em vez de
      insistir sozinho. Com 9 h/semana, teimosia é caro

---

**Pronto para codar.** Ao terminar: [`03-antes-do-pr.md`](03-antes-do-pr.md).
