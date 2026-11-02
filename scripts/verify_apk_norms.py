"""Verify bundled official PDFs and fiche index in a compiled Android APK."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path


def verify(apk_path, catalog_path, ranges_path):
    documents = json.loads(Path(catalog_path).read_text(encoding="utf-8"))
    ranges = json.loads(Path(ranges_path).read_text(encoding="utf-8"))
    if not isinstance(documents, list) or not documents:
        raise ValueError("Empty or invalid PDF catalog")
    if not isinstance(ranges, list) or not ranges:
        raise ValueError("Empty or invalid reference index")
    filenames = [doc["file"] for doc in documents]
    if len(filenames) != len(set(filenames)):
        raise ValueError("Duplicate PDF filenames")
    if len({entry["code"] for entry in ranges}) != len(ranges):
        raise ValueError("Duplicate reference codes")
    with zipfile.ZipFile(apk_path) as apk:
        names = set(apk.namelist())
        for doc in documents:
            entry = "assets/norms/" + doc["file"]
            if entry not in names:
                raise ValueError("Missing PDF in APK: " + entry)
            digest = hashlib.sha256(apk.read(entry)).hexdigest()
            if digest.lower() != doc["sha256"].lower():
                raise ValueError("PDF digest mismatch: " + entry)
        for name in ("assets/norms/pdf-catalog.json", "assets/norms/reference-page-ranges.json"):
            if name not in names:
                raise ValueError("Missing metadata in APK: " + name)
        bundled_docs = json.loads(apk.read("assets/norms/pdf-catalog.json"))
        bundled_ranges = json.loads(apk.read("assets/norms/reference-page-ranges.json"))
        if bundled_docs != documents or bundled_ranges != ranges:
            raise ValueError("Bundled metadata differs from repository source")
    return len(documents), len(ranges)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit("usage: verify_apk_norms.py APK CATALOG RANGES")
    try:
        count, indexed = verify(*sys.argv[1:])
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as exc:
        raise SystemExit("APK offline verification FAILED: " + str(exc))
    print(f"APK offline verification PASS: {count} PDFs, {indexed} indexed fiches")
