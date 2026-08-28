# Fixtures sintéticas (D1)

Geradas pelo módulo `cdc-cnpj-bench` para destravar as frentes B, C e D antes do download real.

## Gerar localmente

```powershell
# Na raiz do repositório
mvn -pl cdc-cnpj-bench exec:java
```

Saída (gitignored): `data/fixtures/competencia=2026-06|2026-07/tabela=.../bucket=.../part-0.parquet`

O arquivo `data/fixtures/manifesto.json` documenta **9 mudanças esperadas** entre as competências
sintéticas `2026-06` → `2026-07` (3 empresas + 6 estabelecimentos no bucket `000`).

## Conteúdo controlado

| Tipo | Quantidade |
|---|---|
| `INSERT` empresas | 1 |
| `DELETE` empresas | 1 |
| `UPDATE` empresas | 1 (`capital_social`) |
| `UPDATE` estabelecimentos (situação) | 1 |
| `UPDATE` estabelecimentos (endereço) | 1 |
| `UPDATE` estabelecimentos (CNAE) | 1 |
| `UPDATE` estabelecimentos (misto) | 1 |
| `INSERT` estabelecimentos | 1 |
| `DELETE` estabelecimentos | 1 |

Os Parquet **não** entram no git. Versione o manifesto após regenerar, se necessário.
