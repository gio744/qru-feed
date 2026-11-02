#!/usr/bin/env python3
"""Prepare reviewed QRU Android catalog entries from a GitHub staging artifact.

Usage: python3 scripts/prepare_contran_offline_catalog.py PATH_TO_EXTRACTED_ARTIFACT
Does not modify files or publish anything. Prints verified catalog entries as JSON.
Requires 'pdfinfo' (Poppler) for reliable page counts.
"""
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "android/app/src/main/assets/norms/pdf-catalog.json"
EXPECTED = {
    1026: ("60b60e60e50afff0d108d3a8f68171c1cd1782c7f2deebb6df84daed6b5b8fa3", "RENAVE; conferir retificação"),
    1027: ("ff396591cb2c185ba6e918241799cf84e2b78269094a7a5228222282b27efc75", "Registro, licenciamento e transferência"),
    1028: ("cd3ca7475c7a2bfbbe7307e3838fadddf3068df48335a7dc93c16d72b252f10f", "Transição free flow"),
    1029: ("bd55657e2beeb92e7a1e47e27213df7fb06fa5d0471bb5d479da47f2ba662b11", "Prorrogação de CNH e ACC; conferir vigência"),
}

def main(folder: Path) -> None:
    manifest = json.loads((folder / "manifest-sha256.json").read_text(encoding="utf-8"))
    records = {int(d["number"]): d for d in manifest["documents"] if d.get("number") in EXPECTED}
    assert set(records) == set(EXPECTED), "Manifest missing required documents"
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    entries = []
    for number, (expected_sha, subtitle) in EXPECTED.items():
        record = records[number]
        assert record["status"] == "staged_verified", f"{number}: not staged"
        assert record["sha256"] == expected_sha, f"{number}: manifest hash differs from reviewed hash"
        original = f"resolucao-contran-{number}-2026.pdf"
        assert record["file"] == original, f"{number}: unexpected filename"
        file = folder / original
        assert file.is_file(), f"{number}: missing PDF"
        data = file.read_bytes()
        assert data.startswith(b"%PDF-") and b"%%EOF" in data[-4096:], f"{number}: invalid PDF"
        assert len(data) == record["bytes"], f"{number}: size mismatch"
        assert hashlib.sha256(data).hexdigest() == expected_sha, f"{number}: SHA-256 mismatch"
        info = subprocess.check_output(["pdfinfo", str(file)], text=True)
        match = re.search(r"^Pages:\s*(\d+)\s*$", info, re.MULTILINE)
        assert match and int(match.group(1)) > 0, f"{number}: cannot determine page count"
        destination = f"resolucao{number}2026.pdf"  # Android Gradle requires alphanumeric basename.
        assert not any(d["file"] == destination or d["title"] == f"Resolução {number:,}/2026".replace(",", ".") for d in catalog), f"{number}: already catalogued"
        assert record["source"].startswith("https://www.gov.br/transportes/"), f"{number}: untrusted source"
        entries.append({
            "file": destination,
            "title": f"Resolução {number:,}/2026".replace(",", "."),
            "subtitle": subtitle,
            "sha256": expected_sha,
            "pageCount": int(match.group(1)),
            "annexPage": None,
            "source": record["source"],
        })
    print(json.dumps({"verified": len(entries), "expectedCatalogSize": len(catalog) + len(entries), "catalogEntries": entries}, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: prepare_contran_offline_catalog.py EXTRACTED_ARTIFACT_DIRECTORY")
    main(Path(sys.argv[1]))
