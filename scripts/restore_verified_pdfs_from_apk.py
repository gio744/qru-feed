#!/usr/bin/env python3
"""Restore only SHA-256-approved official PDF assets from a previously built APK."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
catalog = json.loads((root / "android/app/src/main/assets/norms/pdf-catalog.json").read_text(encoding="utf-8"))
apk, destination = Path(sys.argv[1]), Path(sys.argv[2])
destination.mkdir(parents=True, exist_ok=True)
restored = 0
with zipfile.ZipFile(apk) as archive:
    entries = set(archive.namelist())
    for document in catalog:
        name = document["file"]
        entry = "assets/norms/" + name
        if entry not in entries:
            continue
        data = archive.read(entry)
        digest = hashlib.sha256(data).hexdigest()
        if digest != document["sha256"]:
            raise SystemExit(f"REFUSING APK RESTORE: hash mismatch for {name}")
        target = destination / name
        if target.exists():
            if hashlib.sha256(target.read_bytes()).hexdigest() == digest:
                continue
        target.write_bytes(data)
        restored += 1
print(f"Restored {restored} catalog-verified PDFs from prior APK")
