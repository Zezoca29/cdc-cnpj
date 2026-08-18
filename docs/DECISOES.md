# DECISÕES ARQUITETURAIS (ADR) — CDC-CNPJ

Registro cronológico das decisões do projeto. Entrada mais recente no topo.

## Quando registrar um ADR

Registre se **qualquer** destas for verdadeira:

- Você decidiu algo que outra pessoa da equipe poderia razoavelmente ter decidido diferente.
- Você quer alterar algo em [`CONTRATOS.md`](CONTRATOS.md) — **obrigatório**, sem exceção.
- Você quer trazer um item de *Fora de escopo* para o Núcleo — **obrigatório**, e o ADR precisa
  dizer explicitamente **o que sai em troca**. O orçamento de 468 h é fixo.
- Você quer adicionar uma dependência nova (R7 do CLAUDE.md) — **obrigatório**.
- Você descobriu que uma premissa da spec está errada.

Não registre: escolhas locais de implementação sem efeito sobre outras frentes.

## Formato

```markdown
## ADR-NNN — Título curto e afirmativo

**Data:** YYYY-MM-DD · **Autor:** nome · **Frente:** A/B/C/D
**Estado:** proposto | aceito | rejeitado | substituído por ADR-MMM
**Aprovação necessária:** (todas as frentes, se toca CONTRATOS.md ou escopo)

**Contexto.** Qual situação forçou a decisão. Fatos, não opiniões.

**Decisão.** O que foi decidido, no imperativo.

**Custo.** Horas estimadas. Se traz item de fora do escopo: o que sai em troca.

**Consequências.** O que fica mais fácil, o que fica mais difícil, o que passa a ser proibido.

**Alternativas descartadas.** O que mais foi considerado e por que não.
```

---

## ADR-002 — Normalizar os 256 buckets desde o núcleo, mas diferenciar apenas 16

**Data:** 2026-08-18 · **Autor:** definição inicial · **Frente:** todas
**Estado:** aceito

**Contexto.** A abordagem A restringe o experimento a uma amostra de buckets para caber em
~320 h de engenharia. A formulação inicial reduzia também a normalização a 16 buckets.

**Decisão.** A normalização escreve **todos os 256 buckets**; apenas o **diff** roda sobre 16.

**Custo.** Praticamente nulo. O passe de leitura da normalização é O(n) sobre o arquivo de
origem; particionar a saída em 256 em vez de 16 não altera o custo de leitura nem de parsing.

**Consequências.** Ganha-se de graça: (i) a distribuição completa de tamanhos de bucket, que é
o que permite **provar que a amostra de 16 é representativa** — mitigação direta de uma ameaça
à validade; (ii) a execução de escala completa da Expansão (X1) não precisa re-normalizar nada,
o que reduz o custo daquele item de dias para horas.

**Alternativas descartadas.** Normalizar só 16 buckets — economizaria espaço em disco, mas
tornaria a amostra indefensável na banca e encareceria X1.

---

## ADR-001 — Abordagem A (vertical fino com escala amostral)

**Data:** 2026-08-18 · **Autor:** definição inicial · **Frente:** todas
**Estado:** aceito

**Contexto.** O dossiê técnico original propõe um cronograma de 21+ semanas com três motores de
diff, escala completa, Kafka com Schema Registry e Avro, Spark como comparativo e record linkage
de sócios. A capacidade real do time é de 13 semanas × 4 pessoas × ~9 h = **468 h**, das quais
~120–140 h vão para a escrita da monografia. O escopo original demanda algo entre 3 e 4 vezes a
capacidade disponível, e o cronograma proposto deixaria a redação inteiramente fora do prazo.

**Decisão.** Adotar a **abordagem A**: pipeline completo de ponta a ponta, com o experimento
rodando sobre 16 dos 256 buckets, **dois** motores de diff (E1 baseline e E3 particionado), e
Kafka com JSON Schema versionado.

Ficam fora, declarados como trabalhos futuros: motor E2 (sort-merge externo), Apache Spark,
Schema Registry e Avro, retenção longa e replay histórico.

Sócios (P2), Simples, quarentena de deletes (P3) e a execução de escala completa passam à
**Expansão**, iniciada apenas se o Núcleo inteiro estiver concluído.

**Custo.** Núcleo 288 h (S1–S8), Expansão e fechamento 180 h (S9–S13).

**Consequências.** A afirmação central da monografia deixa de ser *"medimos exaustivamente três
estratégias em escala completa"* e passa a ser *"construímos um protótipo funcional de CDC sobre
snapshots, demonstramos sua viabilidade em hardware commodity e medimos o ganho de uma
estratégia particionada sobre o baseline relacional"*.

É uma afirmação menor, mas sustentável com evidência em 13 semanas. Numa defesa, uma afirmação
menor bem sustentada vale mais que uma promessa grande entregue pela metade.

**Alternativas descartadas.**

*Abordagem B — escala completa com um motor só.* Provaria H2 por medição direta e teria
resultado mais impactante, mas perderia a contribuição comparativa, que é o coração científico
do trabalho. Além disso, muitas horas virariam espera de I/O e depuração de escala — tempo que
não se converte em texto de monografia.

*Abordagem C — bancada primeiro, sobre dados sintéticos.* Daria controle experimental perfeito
e gráficos excelentes, mas sacrificaria P4 (CNPJ alfanumérico), P5 (encoding e parsing) e P3 —
exatamente os diferenciais do trabalho. A banca poderia legitimamente ler como fuga do problema
real. Um pedaço dela foi aproveitado: o gerador sintético entra no Núcleo, para testes e para
destravar as frentes B, C e D na semana 2.
