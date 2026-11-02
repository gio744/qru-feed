#!/usr/bin/env python3
"""Validate editorial QRU operational cards without asserting legal correctness."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "docs/OPERACIONAL-FICHA-SCHEMA-v1.json"
CARDS = ROOT / "content/operational-cards"
ALLOWED = {"pendente", "em_revisao", "validada"}

def main():
    spec = json.loads(SCHEMA.read_text(encoding="utf-8"))
    required = set(spec["required"])
    errors = []
    count = 0
    for path in sorted(CARDS.glob("*.json")) if CARDS.exists() else []:
        count += 1
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (ValueError, OSError) as exc:
            errors.append(f"{path.name}: unreadable JSON: {exc}")
            continue
        missing = required - data.keys()
        if missing:
            errors.append(f"{path.name}: missing fields: {', '.join(sorted(missing))}")
            continue
        review = data["revisao"]
        if not isinstance(review, dict) or review.get("status") not in ALLOWED:
            errors.append(f"{path.name}: invalid review status")
            continue
        if review["status"] == "validada":
            if not review.get("revisor") or not review.get("data"):
                errors.append(f"{path.name}: validated without reviewer/date")
            sources = data["fontes"]
            if not isinstance(sources, list) or not sources or any(not isinstance(s, dict) or not s.get("norma") or not s.get("url") or not s.get("vigencia") for s in sources):
                errors.append(f"{path.name}: validated without complete source metadata")
            for field in ("quandoAutuar", "quandoNaoAutuar", "constatacao", "abordagem", "responsavel", "medidasAdministrativas"):
                if not isinstance(data[field], str) or not data[field].strip():
                    errors.append(f"{path.name}: validated with empty {field}")
    if count == 0:
        print("WARNING: no operational cards found; validation coverage is zero.")
    print(f"Operational card structure: {count} files, {len(errors)} errors")
    for error in errors:
        print("ERROR:", error)
    if errors:
        sys.exit(1)
    print("Structure only: this does not certify legal accuracy.")

if __name__ == "__main__":
    main()
