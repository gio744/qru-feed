#!/usr/bin/env python3
"""Validate QRU offline PDF catalog metadata without network access.

This does not prove that PDF bytes are packaged, valid, or legally current.
"""
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "android/app/src/main/assets/norms/pdf-catalog.json"
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def validate(items):
    errors = []
    if not isinstance(items, list) or not items:
        return ["Catalog must be a non-empty JSON array"]
    seen_files, seen_sources, seen_titles = set(), set(), set()
    for index, item in enumerate(items, 1):
        prefix = f"entry {index}"
        if not isinstance(item, dict):
            errors.append(f"{prefix}: expected object")
            continue
        for key in ("file", "title", "sha256", "source"):
            if not isinstance(item.get(key), str) or not item[key].strip():
                errors.append(f"{prefix}: missing or empty {key}")
        name = item.get("file", "")
        if isinstance(name, str):
            if Path(name).name != name or not name.lower().endswith(".pdf"):
                errors.append(f"{prefix}: invalid PDF filename")
            if name.casefold() in seen_files:
                errors.append(f"{prefix}: duplicate filename {name}")
            seen_files.add(name.casefold())
        title = item.get("title", "")
        if isinstance(title, str):
            if title.casefold() in seen_titles:
                errors.append(f"{prefix}: duplicate title {title}")
            seen_titles.add(title.casefold())
        sha = item.get("sha256", "")
        if not isinstance(sha, str) or not SHA256.fullmatch(sha):
            errors.append(f"{prefix}: invalid SHA-256")
        pages = item.get("pageCount")
        if type(pages) is not int or pages < 1:
            errors.append(f"{prefix}: pageCount must be a positive integer")
        annex = item.get("annexPage")
        if annex is not None and (type(annex) is not int or type(pages) is not int or not 1 <= annex <= pages):
            errors.append(f"{prefix}: invalid annexPage")
        source = item.get("source", "")
        if isinstance(source, str):
            parsed = urlparse(source)
            if parsed.scheme != "https" or parsed.hostname not in {"www.gov.br", "gov.br"}:
                errors.append(f"{prefix}: source must use official gov.br HTTPS")
            if source in seen_sources:
                errors.append(f"{prefix}: duplicate source URL")
            seen_sources.add(source)
    return errors


def main():
    items = json.loads(CATALOG.read_text(encoding="utf-8"))
    errors = validate(items)
    for error in errors:
        print("ERROR:", error, file=sys.stderr)
    print(f"Catalog entries: {len(items)}; metadata errors: {len(errors)}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
