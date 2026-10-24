#!/usr/bin/env python3
"""Restore reviewed official PDFs from the staging artifact, rejecting any byte drift."""
import hashlib
import json
import sys
from pathlib import Path

EXPECTED = {
    1026: "60b60e60e50afff0d108d3a8f68171c1cd1782c7f2deebb6df84daed6b5b8fa3",
    1027: "ff396591cb2c185ba6e918241799cf84e2b78269094a7a5228222282b27efc75",
    1028: "cd3ca7475c7a2bfbbe7307e3838fadddf3068df48335a7dc93c16d72b252f10f",
    1029: "bd55657e2beeb92e7a1e47e27213df7fb06fa5d0471bb5d479da47f2ba662b11",
}

def main(staging: Path, target: Path) -> None:
    manifest_path = staging / "manifest-sha256.json"
    if not manifest_path.is_file():
        matches = list(staging.rglob("manifest-sha256.json"))
        if len(matches) != 1:
            raise ValueError("Expected exactly one staging manifest")
        manifest_path = matches[0]
    root = manifest_path.parent
    docs = json.loads(manifest_path.read_text(encoding="utf-8"))["documents"]
    by_number = {int(doc["number"]): doc for doc in docs if int(doc["number"]) in EXPECTED}
    if set(by_number) != set(EXPECTED):
        raise ValueError("Missing one or more staged resolutions")
    verified = []
    for number, expected in EXPECTED.items():
        doc = by_number[number]
        filename = f"resolucao-contran-{number}-2026.pdf"
        if doc.get("status") != "staged_verified" or doc.get("file") != filename or doc.get("sha256") != expected:
            raise ValueError(f"Unreviewed manifest entry: {number}")
        data = (root / filename).read_bytes()
        actual = hashlib.sha256(data).hexdigest()
        if actual != expected or len(data) != doc["bytes"] or not data.startswith(b"%PDF-") or b"%%EOF" not in data[-4096:]:
            raise ValueError(f"Invalid or changed staged PDF: {number}")
        verified.append((number, data))
    target.mkdir(parents=True, exist_ok=True)
    for number, data in verified:
        (target / f"resolucao{number}2026.pdf").write_bytes(data)
    print(f"Restored {len(verified)} independently SHA-256-verified PDFs")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: restore_verified_contran_pdfs.py STAGING_DIRECTORY TARGET_DIRECTORY")
    main(Path(sys.argv[1]), Path(sys.argv[2]))
