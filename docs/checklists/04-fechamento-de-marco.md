# Checklist 04 — Fechamento de marco

Percorra **em equipe**, junto, ao fim de cada marco. Leva ~30 minutos e é a única defesa real
contra descobrir em novembro que algo estava quebrado desde setembro.

## Os marcos

| Marco | Data | Critério objetivo de "fechado" |
|---|---|---|
| Contratos v1 + fixtures | 24/ago | `CONTRATOS.md` marcado 🔒 e fixtures rodando nas 4 frentes |
| Um bucket ponta a ponta (sintético) | 07/set | Evento sintético chega ao consumer e é consultável |
| **Primeiro evento REAL no Kafka** | **21/set** | Evento derivado de dado real da Receita, consultável |
| E1 ≡ E3 | 05/out | Diferença simétrica dos `event_id` **igual a zero** |
| **CONGELAMENTO DE ESCOPO** | **12/out** | Medições definitivas coletadas e arquivadas |
| **Fim do código** | **02/nov** | Nenhum commit de feature depois desta data |
| Entrega | 16/nov | Monografia, código, dados e scripts empacotados |

## 1. O marco fechou de verdade?

- [ ] O critério objetivo acima foi **verificado rodando**, não presumido
- [ ] Alguém que não implementou conseguiu reproduzir o resultado
- [ ] A saída foi arquivada (log, print, CSV de medição) — não só "funcionou aqui"

## 2. Escopo — revisão obrigatória

- [ ] [`docs/ESCOPO.md`](../ESCOPO.md) reflete a realidade **item por item**, não o otimismo
- [ ] O contador de progresso do Núcleo está correto
- [ ] Itens `[!]` bloqueados têm dono e ação definidos
- [ ] Nenhum item da Expansão foi iniciado com o Núcleo incompleto.
      **Se foi: pare esse trabalho agora** e volte ao Núcleo
- [ ] Itens que não vão dar tempo foram **movidos para Fora de escopo** com ADR, e não
      deixados pendurados fingindo que ainda cabem

## 3. Critério de corte — a conversa difícil

Esta é a parte que as equipes pulam e depois se arrependem. Façam de verdade:

- [ ] Revisamos os gatilhos de corte da spec §13 e do `ESCOPO.md`
- [ ] Algum gatilho foi atingido? Se sim, **acionamos o plano B agora**, não na semana que vem
- [ ] Recalculamos as horas restantes: `semanas restantes × 4 pessoas × 9 h`
- [ ] O que resta no Núcleo cabe nessas horas? Se não cabe, **cortamos hoje**

> Cortar cedo é decisão de engenharia. Cortar tarde é acidente.

## 4. Escrita da monografia

- [ ] A seção correspondente a este marco está **redigida**, não só planejada
      (S3 → metodologia · S5 → arquitetura · S7 → corretude · S8 → resultados)
- [ ] A tabela de progresso da escrita no [`docs/STATUS.md`](../STATUS.md) foi atualizada
- [ ] Números que entraram no texto vêm de medição arquivada, não de memória

## 5. Riscos

- [ ] Revisamos a tabela de riscos da spec §13 — algum gatilho está próximo?
- [ ] Riscos novos que apareceram foram adicionados ao `STATUS.md`
- [ ] Cada risco 🔴 tem plano B **escrito**, não apenas conversado

## 6. Saúde da equipe

- [ ] Cada pessoa entregou algo desde o último marco? Se alguém não entregou, tratamos agora —
      não na véspera da entrega
- [ ] Alguma frente está sobrecarregada? Dá para redistribuir?
      As interfaces são Parquet e Kafka justamente para permitir isso
- [ ] Alguém está travado há mais de uma semana sem pedir ajuda?

## 7. Registro

- [ ] Atualizamos o registro de marcos no [`docs/STATUS.md`](../STATUS.md) com a **data real**
- [ ] Registramos em [`docs/DECISOES.md`](../DECISOES.md) todo corte ou mudança de rumo decidido aqui
- [ ] Definimos o objetivo da próxima semana no `STATUS.md`
- [ ] `python scripts/harness_check.py` passa
