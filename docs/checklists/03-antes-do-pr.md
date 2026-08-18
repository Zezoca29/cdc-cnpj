# Checklist 03 — Antes de entregar (PR / merge)

Nenhuma entrega é aceita sem estes itens. Vale para pessoas e para agentes de IA.

## 1. Atualização do escopo e do status — OBRIGATÓRIO

Este bloco é a razão de o harness existir. Código entregue sem ele deixa o time cego.

- [ ] **[`docs/ESCOPO.md`](../ESCOPO.md)** — mudei o estado do item que toquei
      (`[ ]` → `[~]` → `[x]`, ou `[!]` se travou)
- [ ] **[`docs/ESCOPO.md`](../ESCOPO.md)** — atualizei o contador "Progresso: N / 30"
- [ ] **[`docs/ESCOPO.md`](../ESCOPO.md)** — atualizei a data e o autor no topo do arquivo
- [ ] **[`docs/STATUS.md`](../STATUS.md)** — adicionei entrada no diário de bordo: o que avançou,
      o que travou, o que a próxima pessoa precisa saber
- [ ] **[`docs/STATUS.md`](../STATUS.md)** — atualizei o semáforo da minha frente, com honestidade.
      🟡 e 🔴 declarados cedo salvam o projeto; declarados tarde, não
- [ ] **[`docs/STATUS.md`](../STATUS.md)** — se destravei ou criei um bloqueio, atualizei a tabela
      de bloqueios ativos
- [ ] **[`docs/DECISOES.md`](../DECISOES.md)** — registrei ADR se tomei alguma decisão que outra
      pessoa poderia razoavelmente ter tomado diferente
- [ ] Se avancei uma seção da monografia, atualizei a tabela de progresso da escrita no `STATUS.md`

## 2. Verificação automatizada

- [ ] `python scripts/harness_check.py` passa
- [ ] O build do módulo passa (`mvn -q test` **no módulo**, nunca na raiz)
- [ ] Todos os testes passam — **e eu vi a saída**, não presumi

## 3. As sete regras

- [ ] **R1** Nenhum CNPJ virou número em lugar nenhum do meu código
- [ ] **R2** Nenhum `String.hashCode()`; a função de partição está intacta
- [ ] **R3** Nenhum CSV descompactado é gravado em disco
- [ ] **R4** Parsing tem contadores de rejeição; nenhum `catch` silencioso
- [ ] **R5** Nenhum snapshot é reescrito no lugar
- [ ] **R6** Nenhum `UUID.randomUUID()` em geração de `event_id`
- [ ] **R7** Nenhuma dependência nova sem ADR

## 4. Qualidade

- [ ] Escrevi teste antes ou junto do código, não depois "se der tempo"
- [ ] Se toquei em geração de eventos: o teste de propriedade cobre a mudança
- [ ] Se toquei em E1 ou E3: **o teste E1 ≡ E3 passa com diferença simétrica zero**.
      Falhou? Isso bloqueia o marco. Não contorne, não marque como instável — uma das duas
      implementações tem bug e precisa ser encontrado
- [ ] Sem números mágicos: `256`, `16`, `0.1`, `200` vêm de constantes nomeadas que referenciam
      a seção da spec
- [ ] Código e identificadores em inglês; documentação de domínio em português

## 5. Contratos

- [ ] Não alterei [`docs/CONTRATOS.md`](../CONTRATOS.md). Se alterei, existe ADR aprovado pelas
      quatro frentes e as outras frentes foram avisadas
- [ ] Meu código se adaptou ao contrato, não o contrário

## 6. Última pergunta

- [ ] Se eu sumisse amanhã, outra pessoa conseguiria continuar de onde parei **lendo apenas
      `ESCOPO.md`, `STATUS.md` e o meu código**?

Se a resposta é não, falta escrever alguma coisa. Escreva antes de entregar.
