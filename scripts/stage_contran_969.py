#!/usr/bin/env python3
"""Stage an official PDF candidate; do not change the offline catalogue."""
import hashlib
import json
import urllib.request
from pathlib import Path

URL = "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/resolucao9692022.pdf/@@display-file/file"
DEST = Path("build/contran-pdfs-staging/resolucao9692022.pdf")
MAX_BYTES = 25 * 1024 * 1024


def main():
    request = urllib.request.Request(URL, headers={"User-Agent": "QRU-Norms-Audit/1.0"})
    with urllib.request.urlopen(request, timeout=40) as response:
        if response.geturl().split("/")[2] != "www.gov.br":
            raise ValueError("unexpected redirect outside official domain")
        data = response.read(MAX_BYTES + 1)
    if len(data) > MAX_BYTES or not data.startswith(b"%PDF-") or b"%%EOF" not in data[-4096:]:
        raise ValueError("invalid or incomplete official PDF")
    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_bytes(data)
    print(json.dumps({"file": str(DEST), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(), "source": URL}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
