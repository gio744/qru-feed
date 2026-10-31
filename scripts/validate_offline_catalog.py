#!/usr/bin/env python3
"""Validate offline catalogue metadata without asserting legal validity."""
import json
import re
from pathlib import Path
from urllib.parse import urlparse

CATALOG = Path(__file__).resolve().parents[1] / "android/app/src/main/assets/norms/pdf-catalog.json"


def validate(entries):
    errors = []
    seen_files = set()
    seen_titles = set()
    for index, entry in enumerate(entries):
        label = f"entry {index + 1}"
        filename = entry.get("file", "")
        title = entry.get("title", "")
        digest = entry.get("sha256", "")
        url = entry.get("source", "")
        pages = entry.get("pageCount")
        if not filename or not filename.lower().endswith(".pdf") or "/" in filename or "\\" in filename:
            errors.append(f"{label}: invalid PDF filename")
        if filename.lower() in seen_files:
            errors.append(f"{label}: duplicate filename")
        if title in seen_titles or not title.strip():
            errors.append(f"{label}: duplicate or missing title")
        if not re.fullmatch(r"[a-f0-9]{64}", digest):
            errors.append(f"{label}: invalid SHA-256")
        if type(pages) is not int or pages <= 0:
            errors.append(f"{label}: invalid page count")
        parsed = urlparse(url)
        if parsed.scheme != "https" or parsed.hostname != "www.gov.br":
            errors.append(f"{label}: source must be official gov.br HTTPS")
        seen_files.add(filename.lower())
        seen_titles.add(title)
    if not entries:
        errors.append("catalogue is empty")
    return errors


def main():
    entries = json.loads(CATALOG.read_text(encoding="utf-8"))
    errors = validate(entries)
    print(json.dumps({"documents": len(entries), "errors": errors}, ensure_ascii=False, indent=2))
    return int(bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())
