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
    url = entry.get("official_pdf_url") or entry.get("official_page")
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


def summarize(results):
    """Provide actionable counts without masking failed documents."""
    counts = {}
    for item in results:
        status = item["status"]
        counts[status] = counts.get(status, 0) + 1
    return {
        "total": len(results),
        "verified": counts.get("pdf_verified_for_download", 0),
        "pending": sum(value for status, value in counts.items() if status != "pdf_verified_for_download"),
        "by_status": counts,
        "pending_numbers": [item["number"] for item in results if item["status"] != "pdf_verified_for_download"],
    }


def main():
    items = json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]
    results = [inspect(item) for item in items]
    print(json.dumps({"summary": summarize(results), "results": results}, ensure_ascii=False, indent=2))
    return int(any(item["status"] != "pdf_verified_for_download" for item in results))


if __name__ == "__main__":
    sys.exit(main())
