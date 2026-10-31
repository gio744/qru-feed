#!/usr/bin/env python3
"""Stage a verified official CONTRAN PDF without modifying the offline catalogue.

Usage: python3 scripts/stage_official_pdf.py 887 2021
"""
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path
from urllib.parse import urlparse

MAX_BYTES = 25 * 1024 * 1024


def stage(number: int, year: int):
    if not (1 <= number <= 9999 and 2000 <= year <= 2100):
        raise ValueError("invalid resolution number/year")
    filename = f"resolucao{number}{year}.pdf"
    url = ("https://www.gov.br/transportes/pt-br/assuntos/transito/"
           f"conteudo-contran/resolucoes/{filename}/@@display-file/file")
    request = urllib.request.Request(url, headers={"User-Agent": "QRU-Norms-Audit/1.0"})
    with urllib.request.urlopen(request, timeout=40) as response:
        final = urlparse(response.geturl())
        if final.scheme != "https" or final.hostname != "www.gov.br":
            raise ValueError("unexpected redirect outside official domain")
        data = response.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES or not data.startswith(b"%PDF-") or b"%%EOF" not in data[-4096:]:
        raise ValueError("invalid or incomplete official PDF")
    dest = Path("build/contran-pdfs-staging") / filename
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    print(json.dumps({"file": str(dest), "bytes": len(data),
                      "sha256": hashlib.sha256(data).hexdigest(), "source": url}))
    return dest


if __name__ == "__main__":
    if len(sys.argv) != 3 or not all(re.fullmatch(r"[0-9]{1,4}", a) for a in sys.argv[1:]):
        raise SystemExit("Usage: stage_official_pdf.py NUMBER YEAR")
    stage(int(sys.argv[1]), int(sys.argv[2]))
