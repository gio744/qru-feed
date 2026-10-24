#!/usr/bin/env python3
"""Validate each cached offline PDF against the reviewed catalog before caching."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
catalog = json.loads((root / "android/app/src/main/assets/norms/pdf-catalog.json").read_text(encoding="utf-8"))
directory = root / "android/app/build/generated/normAssets/norms"
verified = 0
missing = []
for document in catalog:
    path = directory / document["file"]
    if not path.exists():
        missing.append(document["file"])
        continue
    if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != document["sha256"]:
        raise SystemExit(f"REFUSING CACHE: hash mismatch: {path.name}")
    verified += 1
if verified == 0:
    raise SystemExit("REFUSING CACHE: no verified documents")
print(f"Validated {verified}/{len(catalog)} cached PDFs; missing: {', '.join(missing) if missing else 'none'}")
