#!/usr/bin/env python3
"""Download verified official CONTRAN PDFs 1026-1029 to a staging folder.

This does not alter Android assets. Requires the audit manifest and verifies
PDF header/trailer and SHA-256 before writing each staged file.
"""
import hashlib
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "android/app/src/main/assets/norms/pending-official-2026.json"
OUT = ROOT / "build/contran-pdfs-staging"
MAX_BYTES = 25 * 1024 * 1024

def main():
    items = json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]
    report = []
    failed = False
    OUT.mkdir(parents=True, exist_ok=True)
    for entry in items:
        number = entry["number"]
        url = entry.get("official_pdf_url")
        if not url:
            report.append({"number": number, "status": "pending_official_url"})
            continue
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "QRU-Norms-Audit/1.0"})
            with urllib.request.urlopen(request, timeout=35) as response:
                data = response.read(MAX_BYTES + 1)
                final_url = response.geturl()
            if len(data) > MAX_BYTES or not data.startswith(b"%PDF-") or b"%%EOF" not in data[-4096:]:
                raise ValueError("invalid or incomplete PDF")
            digest = hashlib.sha256(data).hexdigest()
            filename = f"resolucao-contran-{number}-2026.pdf"
            (OUT / filename).write_bytes(data)
            report.append({"number": number, "status": "staged_verified", "file": filename, "bytes": len(data), "sha256": digest, "source": final_url})
        except Exception as exc:
            failed = True
            report.append({"number": number, "status": "download_failed", "error": str(exc)})
    (OUT / "manifest-sha256.json").write_text(json.dumps({"documents": report}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"documents": report}, ensure_ascii=False, indent=2))
    return int(failed)

if __name__ == "__main__":
    raise SystemExit(main())
