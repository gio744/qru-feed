#!/usr/bin/env python3
"""Fail closed when offline PDF catalog entries are inconsistent or unpinned."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "android/app/src/main/assets/norms/pdf-catalog.json"

def validate(entries):
    if not isinstance(entries, list) or not entries:
        raise ValueError("PDF catalog must be a non-empty list")
    names = set()
    for index, doc in enumerate(entries):
        if not isinstance(doc, dict):
            raise ValueError(f"Entry {index} is not an object")
        name = doc.get("file")
        if not isinstance(name, str) or not re.fullmatch(r"[A-Za-z0-9]+\.pdf", name):
            raise ValueError(f"Invalid PDF filename at entry {index}")
        if name in names:
            raise ValueError(f"Duplicate PDF filename: {name}")
        names.add(name)
        digest = doc.get("sha256")
        if not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest):
            raise ValueError(f"Missing or invalid SHA-256 for {name}")
        source = doc.get("source")
        if not isinstance(source, str) or not re.fullmatch(r"https://www\\.gov\\.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/[A-Za-z0-9._-]+\\.pdf/@@display-file/file", source):
            raise ValueError(f"Unapproved PDF source for {name}")
        pages = doc.get("pageCount")
        if type(pages) is not int or pages < 1:
            raise ValueError(f"Invalid page count for {name}")
        annex = doc.get("annexPage")
        if annex is not None and (type(annex) is not int or not 1 <= annex <= pages):
            raise ValueError(f"Invalid annex page for {name}")
    return len(names)

if __name__ == "__main__":
    print(f"Validated {validate(json.loads(CATALOG.read_text(encoding='utf-8')))} SHA-pinned PDF entries")
