#!/usr/bin/env python3
"""
harness_check.py - Verificacao do harness de projeto do CDC-CNPJ.

Roda sem dependencias externas. Use antes de comecar a codar e antes de entregar.

    python scripts/harness_check.py            # verificacao completa
    python scripts/harness_check.py --quiet    # so os problemas

Codigo de saida: 0 se nao ha ERRO, 1 caso contrario.
"""

import argparse
import re
import sys
from datetime import date, datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Documentos que o harness exige e a frequencia esperada de atualizacao (em dias).
# None = nao ha expectativa de atualizacao frequente.
REQUIRED_DOCS = {
    "CLAUDE.md": None,
    "README.md": None,
    "docs/ESCOPO.md": 7,
    "docs/STATUS.md": 7,
    "docs/CONTRATOS.md": None,
    "docs/DECISOES.md": None,
    "docs/checklists/01-onboarding.md": None,
    "docs/checklists/02-antes-de-codar.md": None,
    "docs/checklists/03-antes-do-pr.md": None,
    "docs/checklists/04-fechamento-de-marco.md": None,
    "docs/superpowers/specs/2026-08-18-cdc-cnpj-design.md": None,
}

# Marcos do projeto, para o lembrete de prazo.
DEADLINE = date(2026, 11, 16)
MILESTONES = [
    (date(2026, 8, 24), "Contratos v1 + fixtures"),
    (date(2026, 9, 7), "Um bucket ponta a ponta (sintetico)"),
    (date(2026, 9, 21), "PRIMEIRO EVENTO REAL NO KAFKA"),
    (date(2026, 10, 5), "E1 = E3, diferenca simetrica zero"),
    (date(2026, 10, 12), "CONGELAMENTO DE ESCOPO"),
    (date(2026, 11, 2), "FIM DO CODIGO"),
    (DEADLINE, "Entrega"),
]

# Padroes proibidos no codigo-fonte. (regex, regra, explicacao)
FORBIDDEN = [
    (
        re.compile(r"\.hashCode\s*\(\s*\)"),
        "R2",
        "hashCode() nao e estavel entre JVMs. Use murmur3_32 (ver CONTRATOS.md 1).",
    ),
    (
        re.compile(r"UUID\s*\.\s*randomUUID\s*\(\s*\)"),
        "R6",
        "event_id deve ser uuid_v5 deterministico, nunca aleatorio.",
    ),
    (
        re.compile(r"(?i)cnpj[a-z_]*\s*(?:=|\)|,).*?(?:Long\.parseLong|Integer\.parseInt)"),
        "R1",
        "CNPJ e String em toda a stack. Nunca converta para numero.",
    ),
    (
        re.compile(r"(?i)cnpj[a-z_]*\s+(?:BIGINT|INTEGER|INT\b|NUMERIC)"),
        "R1",
        "Coluna de CNPJ deve ser VARCHAR(14), nunca tipo numerico.",
    ),
]

SOURCE_SUFFIXES = {".java", ".kt", ".sql", ".py", ".xml"}
SKIP_DIRS = {"target", "build", "node_modules", ".git", "data", "venv", ".venv"}

errors = []
warnings = []
notes = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def note(msg):
    notes.append(msg)


def read(rel):
    path = ROOT / rel
    if not path.exists():
        return None
    return path.read_text(encoding="utf-8", errors="replace")


def parse_last_update(text):
    """Extrai 'Ultima atualizacao: YYYY-MM-DD' do topo de um documento."""
    m = re.search(r"[Úu]ltima atualiza[çc][ãa]o:\*{0,2}\s*(\d{4}-\d{2}-\d{2})", text)
    if not m:
        return None
    try:
        return datetime.strptime(m.group(1), "%Y-%m-%d").date()
    except ValueError:
        return None


def check_docs():
    """Todos os documentos do harness existem e estao razoavelmente atuais."""
    today = date.today()
    for rel, max_age in REQUIRED_DOCS.items():
        text = read(rel)
        if text is None:
            err(f"documento ausente: {rel}")
            continue
        if len(text.strip()) < 200:
            err(f"documento praticamente vazio: {rel}")
            continue
        if max_age is None:
            continue
        updated = parse_last_update(text)
        if updated is None:
            warn(f"{rel}: nao encontrei a linha 'Ultima atualizacao: YYYY-MM-DD' no topo")
            continue
        age = (today - updated).days
        if age > max_age:
            err(
                f"{rel}: sem atualizacao ha {age} dias (limite {max_age}). "
                "Atualize o escopo e o status antes de continuar."
            )
        elif age > max_age // 2:
            warn(f"{rel}: atualizado ha {age} dias - vale revisar")


def parse_scope():
    """Le docs/ESCOPO.md e devolve (itens_nucleo, itens_expansao) por estado."""
    text = read("docs/ESCOPO.md")
    if text is None:
        return None, None

    def section(title):
        m = re.search(
            r"^##\s+" + title + r".*?$(.*?)(?=^##\s|\Z)",
            text,
            re.MULTILINE | re.DOTALL,
        )
        return m.group(1) if m else ""

    def items(block):
        found = {"pendente": [], "andamento": [], "concluido": [], "bloqueado": []}
        mapping = {" ": "pendente", "~": "andamento", "x": "concluido", "!": "bloqueado"}
        for mark, label in re.findall(r"^\s*-\s*\[([ x~!])\]\s*(.+)$", block, re.MULTILINE):
            key = mapping.get(mark.lower())
            if key:
                found[key].append(label.strip())
        return found

    return items(section(r"N[úu]cleo")), items(section(r"Expans[ãa]o"))


def check_scope():
    """Coerencia do escopo: contador correto e Expansao bloqueada."""
    nucleo, expansao = parse_scope()
    if nucleo is None:
        return  # ausencia ja reportada em check_docs

    total = sum(len(v) for v in nucleo.values())
    done = len(nucleo["concluido"])

    if total == 0:
        err("docs/ESCOPO.md: nenhum item encontrado na secao Nucleo")
        return

    text = read("docs/ESCOPO.md")
    m = re.search(r"Progresso:\s*\*{0,2}(\d+)\s*/\s*(\d+)", text)
    if m:
        declared_done, declared_total = int(m.group(1)), int(m.group(2))
        if (declared_done, declared_total) != (done, total):
            err(
                f"docs/ESCOPO.md: contador diz {declared_done}/{declared_total} mas as caixas "
                f"marcam {done}/{total}. Corrija o contador."
            )
    else:
        warn("docs/ESCOPO.md: nao encontrei a linha 'Progresso: N / M'")

    note(f"Nucleo: {done}/{total} concluidos, {len(nucleo['andamento'])} em andamento, "
         f"{len(nucleo['bloqueado'])} bloqueados")

    for item in nucleo["bloqueado"]:
        warn(f"item bloqueado no Nucleo: {item[:70]}")

    # A regra central do projeto.
    if expansao:
        started = expansao["andamento"] + expansao["concluido"]
        if started and done < total:
            for item in started:
                err(
                    f"REGRA DE ESCOPO VIOLADA: item da Expansao iniciado com o Nucleo "
                    f"incompleto ({done}/{total}) -> {item[:60]}"
                )


def check_source():
    """Varre o codigo-fonte por padroes que violam as regras invioraveis."""
    src = ROOT / "src"
    roots = [src] if src.exists() else [p for p in ROOT.iterdir()
                                        if p.is_dir() and p.name not in SKIP_DIRS
                                        and p.name not in {"docs", "scripts"}]
    scanned = 0
    for base in roots:
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if not path.is_file() or path.suffix not in SOURCE_SUFFIXES:
                continue
            if any(part in SKIP_DIRS for part in path.parts):
                continue
            scanned += 1
            try:
                content = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            for lineno, line in enumerate(content.splitlines(), 1):
                stripped = line.strip()
                if stripped.startswith(("//", "#", "*", "--")):
                    continue
                for pattern, rule, why in FORBIDDEN:
                    if pattern.search(line):
                        rel = path.relative_to(ROOT)
                        err(f"[{rule}] {rel}:{lineno} - {why}")

    if scanned == 0:
        note("nenhum arquivo de codigo encontrado ainda (esperado antes da S2)")
    else:
        note(f"{scanned} arquivos de codigo verificados")


def check_contracts():
    """Contratos congelados nao mudam sem ADR."""
    contracts = read("docs/CONTRATOS.md")
    decisions = read("docs/DECISOES.md")
    if contracts is None or decisions is None:
        return

    frozen = re.search(r"ESTADO_CONTRATO:\s*CONGELADO", contracts) is not None
    declared = re.search(r"ESTADO_CONTRATO:\s*(\w+)", contracts)
    if declared is None:
        err("docs/CONTRATOS.md: falta a linha ESTADO_CONTRATO: EM_DEFINICAO | CONGELADO")
        return
    pending = contracts.count("🔶")

    if frozen:
        note("CONTRATOS.md esta CONGELADO - alteracoes exigem ADR aprovado pelas 4 frentes")
    else:
        if date.today() > date(2026, 8, 24):
            err(
                "CONTRATOS.md ainda nao foi congelado, e o prazo era 24/ago (fim da S1). "
                "Sem contrato congelado as frentes divergem."
            )
        else:
            note(f"CONTRATOS.md em definicao - {pending} secoes ainda abertas (prazo: 24/ago)")

    if not re.search(r"^##\s*ADR-\d+", decisions, re.MULTILINE):
        warn("docs/DECISOES.md nao contem nenhum ADR")


def check_timeline():
    """Lembretes de prazo e do proximo marco."""
    today = date.today()
    remaining = (DEADLINE - today).days
    if remaining < 0:
        note("prazo de entrega ja passou")
        return

    weeks = max(remaining / 7, 0)
    capacity = int(weeks * 4 * 9)  # 4 pessoas, 9h/semana
    note(f"{remaining} dias ate a entrega (~{capacity} h de capacidade restante do time)")

    for when, label in MILESTONES:
        if when >= today:
            days = (when - today).days
            note(f"proximo marco: {label} em {days} dia(s) ({when.isoformat()})")
            break

    if today > date(2026, 11, 2):
        warn("passou de 02/nov: FIM DO CODIGO. Nada de features novas - so escrita e defesa.")
    elif today > date(2026, 10, 12):
        warn("passou de 12/out: escopo CONGELADO. O que nao roda vira trabalho futuro.")


def main():
    parser = argparse.ArgumentParser(description="Verificacao do harness do CDC-CNPJ")
    parser.add_argument("--quiet", action="store_true", help="mostrar apenas problemas")
    args = parser.parse_args()

    check_docs()
    check_scope()
    check_source()
    check_contracts()
    check_timeline()

    print()
    print("=" * 72)
    print("  HARNESS CHECK - CDC-CNPJ")
    print("=" * 72)

    if notes and not args.quiet:
        print("\nContexto:")
        for n in notes:
            print(f"  . {n}")

    if warnings:
        print(f"\nAvisos ({len(warnings)}):")
        for w in warnings:
            print(f"  ~ {w}")

    if errors:
        print(f"\nERROS ({len(errors)}):")
        for e in errors:
            print(f"  X {e}")
        print("\nCorrija os erros antes de continuar.")
        print("Em caso de duvida: CLAUDE.md e docs/checklists/02-antes-de-codar.md")
        print("=" * 72)
        return 1

    print("\nOK - harness integro.")
    if not args.quiet:
        print("Lembre-se: ao terminar a tarefa, atualize docs/ESCOPO.md e docs/STATUS.md.")
    print("=" * 72)
    return 0


if __name__ == "__main__":
    sys.exit(main())
