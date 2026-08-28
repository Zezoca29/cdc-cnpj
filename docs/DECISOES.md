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

---

## ADR-007 — Máquina de referência em VM na nuvem

**Data:** 2026-08-28 · **Autor:** agente · **Frente:** A / infra
**Estado:** aceito

**Contexto.** O notebook de desenvolvimento não dispõe de ~250 GB livres para duas competências
(raw ~85 GB cada + Parquet canônico). A spec já prevê uma máquina de referência separada; o time
precisa executar A11 e medições definitivas sem bloquear o desenvolvimento local (fixtures sintéticas).

**Decisão.** Adotar uma **VM Linux na nuvem** (Hetzner, Oracle Free Tier ou equivalente) como
máquina de referência: roda `IngestionPipelineCli`, guarda `data/canonical/` e executa o protocolo
de medição. O notebook sincroniza apenas artefatos pequenos (`data/measurements/`, manifestos,
amostras de bucket).

**Custo.** ~2–4 h de setup + custo mensal da VM (desligar quando ociosa). Scripts em
`infra/cloud-vm/`.

**Consequências.** A11 deixa de depender do hardware do notebook. Medições definitivas e marco
“primeiro evento real” rodam na VM. Monografia documenta o ambiente da VM como oráculo de medição.

**Alternativas descartadas.** Object storage com streaming (S3/R2) — exige ADR de dependência e
código novo. Plano B só Estabelecimentos — válido como fallback, mas a VM permite o núcleo completo.

---

## ADR-006 — Quarentena de deletes (Expansão X4, P3)

**Data:** 2026-08-27 · **Autor:** agente · **Frente:** C
**Estado:** aceito
**Aprovação necessária:** todas as frentes (comportamento de publicação de eventos)

**Contexto.** No snapshot differencing, um `DELETE` significa ausência na competência destino.
Isso pode ser transitório (republicação da RF, atraso de carga) ou um falso positivo corrigido na
competência seguinte. Publicar `empresa.encerrada` imediatamente gera eventos irreversíveis no
changelog (problema P3).

**Decisão.**

1. `ChangeRecord` com `DELETE` entra em **quarentena** antes da publicação Kafka.
2. `INSERT` com a mesma `entity_key` **cancela** a quarentena (ressurreição).
3. Ao final do lote (`flush`), deletes ainda pendentes são **confirmados** e publicados.
4. **Fast-path:** `DELETE` em `estabelecimentos` com `situacao_cadastral=08` no `before` confirma
   imediatamente (encerramento cadastral explícito na fonte).
5. Constante `CONFIRMATION_COMPETENCIAS = 1` — uma transição completa confirma deletes restantes.

**Custo.** ~6 h. Não retira itens do Núcleo.

**Consequências.** O produtor Kafka deve usar `QuarantineProcessor` antes de publicar. Métricas
`quarantined / cancelled / confirmed` alimentam a análise de qualidade na monografia.

**Alternativas descartadas.** Descartar deletes silenciosamente — viola R4. Quarentena em Postgres
persistente — complexidade desnecessária para duas competências; store in-memory basta.

---

## ADR-005 — Schema Simples e evento de opção (Expansão X3)

**Data:** 2026-08-27 · **Autor:** agente · **Frente:** A/B/C
**Estado:** aceito
**Aprovação necessária:** todas as frentes (estende contratos da Expansão)

**Contexto.** A tabela Simples (`Simples.zip`) traz adesão ao Simples Nacional e ao MEI por
`cnpj_basico`. É um arquivo único (~200 MB), particionado como empresas. A spec define um único
tipo de evento: `simples.opcao_alterada`.

**Decisão.**

1. Schema Parquet `simples` com 6 colunas de negócio do layout RF (7 campos CSV, chave separada).
2. Chave natural: `cnpj_basico` (uma linha por empresa no regime).
3. Particionamento murmur3 mod 256 por `cnpj_basico`.
4. Flags `opcao_pelo_simples` e `opcao_pelo_mei` permanecem `S`/`N`/null (string).
5. Datas convertidas via `ReceitaDateParser` (`00000000` → null).
6. Tópico `cnpj.simples`; todo `ChangeRecord` de `simples` mapeia para `simples.opcao_alterada`.

**Custo.** ~6 h. Não retira itens do Núcleo.

**Consequências.** Diff E1/E3 funciona sem alteração de motor. Empresas sem linha em Simples não
geram bucket — materialização de buckets vazios cobre lacunas.

**Alternativas descartadas.** Booleanos Avro — inconsistente com demais códigos string do projeto.

---

## ADR-004 — Schema Socios e chave de identidade (Expansão X2)

**Data:** 2026-08-27 · **Autor:** agente · **Frente:** A/B/C
**Estado:** aceito
**Aprovação necessária:** todas as frentes (estende contratos de evento e schema canônico da Expansão)

**Contexto.** A tabela Socios da RF não tem chave natural estável: o CPF/CNPJ do sócio vem mascarado
e pode mudar entre competências sem que a pessoa tenha saído da sociedade (problema P2). Diff
ingênuo por `(cnpj_basico, identificador_socio, cpf_cnpj_socio)` gera pares falsos
`socio.excluido` + `socio.incluido` — eventos fantasma (spec §17).

**Decisão.**

1. Adicionar schema Parquet `socios` com 11 colunas de negócio do layout RF + campo derivado
   `socio_identity_key`, particionado por `cnpj_basico` (mesma função murmur3 mod 256).
2. Chave natural do diff **com identidade:** `(cnpj_basico, socio_identity_key)`, onde
   `socio_identity_key = murmur3_128(utf8(cnpj_basico|identificador_socio|nome_normalizado))`.
   - `nome_normalizado`: maiúsculas, trim, espaços colapsados, sem pontuação, **sem acentos** (NFD).
   - O CPF/CNPJ mascarado **não** entra na chave — é instável entre competências (causa raiz de P2).
3. Chave **ingênua** (somente para medir fantasmas): `(cnpj_basico, identificador_socio, cpf_cnpj_socio)`.
4. Tópico Kafka `cnpj.socios`; tipos `socio.incluido`, `socio.excluido`, `socio.alterado`.
5. Métrica de evento fantasma: contar pares DELETE+INSERT do diff ingênuo no mesmo `cnpj_basico`
   cujo `socio_identity_key` coincide (pareamento 1:1 guloso).

**Custo.** ~12 h (parser + diff + eventos + métrica + testes). Não retira itens do Núcleo.

**Consequências.** O diff de produção usa identidade; a métrica de fantasma quantifica o custo de
ignorar P2. Fellegi-Sunter completo fica fora — heurística determinística basta para demonstrar o
problema na monografia.

**Alternativas descartadas.** Fellegi-Sunter com zona de revisão manual — inviável em escala
automatizada (spec §A8). Chave só por nome — colisões excessivas em nomes comuns.

---

## ADR-003 — Título da monografia

**Data:** 2026-08-18 · **Autor:** montagem da Atividade 01 · **Frente:** todas
**Estado:** **aceito** — ratificado em 2026-08-27 para destravar S2; integrantes podem registrar objeção até 31/ago
**Aprovação necessária:** todas as frentes (o título aparece na capa, no repositório e na defesa)

**Contexto.** A Atividade 01 exige um título. Título é decisão difícil de reverter depois que
entra em capa, resumo, repositório e slides, e é exatamente o tipo de escolha que quatro pessoas
fariam de quatro jeitos diferentes. Melhor decidir uma vez, cedo, e registrar.

**Decisão.** Adotar:

> Change Data Capture sobre snapshots de dados abertos: um pipeline reprodutível para detecção e
> publicação de eventos de mudança no cadastro do CNPJ

O título nomeia os três elementos que a banca vai cobrar: o **problema** (CDC sobre snapshots), a
**propriedade defendida** (reprodutibilidade) e o **domínio** (CNPJ). Se a banca preferir ênfase
experimental, acrescenta-se o subtítulo "comparação entre uma estratégia relacional e uma
estratégia particionada por hash em hardware de commodity", registrado como variante no documento
da Atividade 01.

**Custo.** Zero em horas. O custo de não decidir é retrabalho de capa, resumo e slides na S12.

**Consequências.** O título compromete a monografia com "reprodutível": manifesto com SHA-256,
protocolo repetível e dados brutos de medição publicados deixam de ser desejáveis e passam a ser
obrigatórios. Também deixa claro, pela ausência, que escala completa **não** é prometida — o que é
coerente com o ADR-001.

**Alternativas descartadas.** "Big Data aplicado aos dados abertos do CNPJ" (vago, não diz o que
foi construído nem medido); "Pipeline ETL para os dados públicos do CNPJ" (descreve um ETL comum e
esconde a contribuição); "Detecção de mudanças em grandes volumes de dados" (genérico, perde
domínio e reprodutibilidade).

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
