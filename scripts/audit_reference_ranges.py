#!/usr/bin/env python3
"""Audit the integrity of QRU's offline PDF page mapping (no legal validation)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'android/app/src/main/assets/norms'


def main():
    catalog = json.loads((BASE / 'pdf-catalog.json').read_text(encoding='utf-8'))
    ranges = json.loads((BASE / 'reference-page-ranges.json').read_text(encoding='utf-8'))
    files = {entry['file']: entry for entry in catalog}
    errors = []
    seen = set()
    for item in ranges:
        code = item.get('code')
        if not code or code in seen:
            errors.append(f'duplicate or missing code: {code}')
        seen.add(code)
        pdf = files.get(item.get('file'))
        if pdf is None:
            errors.append(f'{code}: PDF not in catalog')
            continue
        if item.get('sha256') != pdf.get('sha256'):
            errors.append(f'{code}: PDF SHA mismatch')
        first, last = item.get('firstPage'), item.get('lastPage')
        if not isinstance(first, int) or not isinstance(last, int) or first < 1 or first > last or last > pdf['pageCount']:
            errors.append(f'{code}: invalid page range {first}-{last}')
    print(f'Checked {len(ranges)} reference ranges across {len(files)} PDFs; {len(errors)} errors')
    for error in errors:
        print('ERROR:', error)
    if errors:
        raise SystemExit(1)
    print('Structural integrity passed. Legal and editorial review is still required.')


if __name__ == '__main__':
    main()
