#!/usr/bin/env python3
"""Audit missing CONTRAN PDFs without changing the Android offline catalog.

Usage: python3 scripts/audit_pending_contran_pdfs.py
Outputs JSON report to stdout; exit 1 when any document fails verification.
"""
import hashlib
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "android/app/src/main/assets/norms/pending-official-2026.json"


def inspect(entry):
    url = entry.get("official_page")
    if not url:
        return {"number": entry["number"], "status": "missing_official_pdf_url"}
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "QRU-Norms-Audit/1.0"})
        with urllib.request.urlopen(request, timeout=35) as response:
            data = response.read(25 * 1024 * 1024 + 1)
            final_url = response.geturl()
            content_type = response.headers.get("Content-Type", "unknown")
        if len(data) > 25 * 1024 * 1024:
            return {"number": entry["number"], "status": "file_too_large"}
        if not data.startswith(b"%PDF-") or b"%%EOF" not in data[-4096:]:
            return {"number": entry["number"], "status": "not_a_complete_pdf", "url": final_url, "content_type": content_type, "bytes": len(data), "header_hex": data[:16].hex()}
        return {
            "number": entry["number"],
            "status": "pdf_verified_for_download",
            "url": final_url,
            "bytes": len(data),
            "content_type": content_type,
            "sha256": hashlib.sha256(data).hexdigest(),
        }
    except Exception as exc:
        return {"number": entry["number"], "status": "download_failed", "error": str(exc)}


def main():
    items = json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]
    results = [inspect(item) for item in items]
    print(json.dumps({"results": results}, ensure_ascii=False, indent=2))
    return int(any(item["status"] != "pdf_verified_for_download" for item in results))


if __name__ == "__main__":
    sys.exit(main())
