"""Recheck candidate identities against supplied PDF text; never changes signed input.

Usage: python audit_reference_identity.py CANDIDATE MANUAL_PDF MANUAL_TXT TABLE_PDF TABLE_TXT OUT_JSON
PDF text must come from pdftotext -layout. Outputs evidence, not legal readiness.
"""
import hashlib
import json
import re
import sys
import unicodedata
from pathlib import Path


def norm_article(value):
    value = unicodedata.normalize('NFKD', value).casefold()
    value = re.sub(r'\b(?:artigo|art)\.?', '', value)
    return re.sub(r'[^a-z0-9§]', '', value)


def inspect(candidate, manual_text, table_text):
    manual = {}
    for page, text in enumerate(manual_text.split('\f'), 1):
        # Restrict matches to fiche headers; cross-references in body are not evidence.
        match = re.search(r'Tipifica[çc][ãa]o Resumida:(.*?)Tipifica[çc][ãa]o do Enquadramento:', text, re.S | re.I)
        if not match:
            continue
        header = match.group(1)
        article = re.search(r'Amparo Legal:\s*(.*?)(?:\n|$)', header)
        if not article:
            continue
        for code in set(re.findall(r'(?<!\d)\d{3}\s*[-–]\s*\d{2}(?!\d)', header)):
            code = re.sub(r'\s+', '', code).replace('–', '-')
            manual.setdefault(code, []).append({'page': page, 'article': article.group(1).strip()})
    table = {}
    for page, text in enumerate(table_text.split('\f'), 1):
        lines = text.splitlines()
        for n, line in enumerate(lines):
            # Code + desdobramento occupy the first two columns.
            match = re.match(r'^\s*(\d{4})\s*(\d)\s+(.*)', line)
            if not match:
                continue
            code = match.group(1)[:3] + '-' + match.group(1)[3] + match.group(2)
            row = match.group(3)
            for extra in lines[n + 1:n + 5]:
                if re.match(r'^\s*\d{4}\s*\d\s+', extra):
                    break
                row += ' ' + extra
            # Article column follows at least two spaces in layout output.
            art = re.search(r'\s{2,}(\d{2,3}(?:\s*[-–]\s*[A-Z])?(?:,\s*(?:[IVXLCDM]+|§\s*\d+[º°o]?|[a-z]))*)\s{2,}', row)
            if art:
                table.setdefault(code, []).append({'page': page, 'article': art.group(1).strip()})
    rows = []
    for item in candidate['package']['items']:
        code = item['code']
        article = norm_article(item['article'])
        m = [x for x in manual.get(code, []) if norm_article(x['article']) == article]
        t = [x for x in table.get(code, []) if norm_article(x['article']) == article]
        anchor = re.search(r'#page=(\d+)', item.get('source', ''))
        anchor_page = int(anchor.group(1)) if anchor and item.get('source','').split('#')[0].endswith('/mbvt20222.pdf') else None
        rows.append({'stable_key': item['stable_key'], 'code': code, 'article': item['article'],
                     'manual_header_evidence': m, 'table_2022_evidence': t,
                     'source_page': anchor_page,
                     'source_page_matches_header': anchor_page in [x['page'] for x in m] if anchor_page else None,
                     'identity_found_in_supplied_sources': bool(m or t),
                     'operational_ready': False})
    return rows


def supplement(rows, candidate, manifest_path):
    docs = json.loads(Path(manifest_path).read_text())
    for row, item in zip(rows, candidate['package']['items']):
        source = item.get('source', '')
        for doc in docs:
            name = doc['source_file']
            if not source.split('#')[0].endswith('/' + name):
                continue
            text = Path(doc['text_path']).read_text()
            # OCR evidence is only a lead for visual review, never automatic approval.
            header = text.split('Amparo Legal:')[0]
            art = re.search(r'Amparo Legal:\s*([^\n]+)', text)
            if row['code'] in header and art and norm_article(art.group(1)) == norm_article(row['article']):
                row['annex_ocr_evidence'] = {'source_file': name, 'page': doc['page'], 'article': art.group(1).strip(), 'requires_visual_review': True}
                row['identity_found_in_supplied_sources'] = True
                anchor = re.search(r'#page=(\d+)', source)
                row['source_page'] = int(anchor.group(1)) if anchor else None
                row['source_page_matches_header'] = row['source_page'] == doc['page'] if anchor else None
    return rows


def main():
    candidate_path, manual_pdf, manual_txt, table_pdf, table_txt, output = map(Path, sys.argv[1:7])
    candidate_bytes = candidate_path.read_bytes()
    candidate = json.loads(candidate_bytes)
    rows = inspect(candidate, manual_txt.read_text(), table_txt.read_text())
    if len(sys.argv) > 7:
        rows = supplement(rows, candidate, sys.argv[7])
    result = {'scope': 'Historical code/article identity only; no current validity or operational approval',
              'content_version': candidate['package']['content_version'],
              'candidate_file_sha256': hashlib.sha256(candidate_bytes).hexdigest(),
              'manual_pdf_sha256': hashlib.sha256(manual_pdf.read_bytes()).hexdigest(),
              'table_pdf_sha256': hashlib.sha256(table_pdf.read_bytes()).hexdigest(),
              'summary': {'items': len(rows), 'manual_header_matches': sum(bool(x['manual_header_evidence']) for x in rows),
                          'annex_ocr_matches': sum('annex_ocr_evidence' in x for x in rows),
                          'table_matches': sum(bool(x['table_2022_evidence']) for x in rows),
                          'identity_matches': sum(x['identity_found_in_supplied_sources'] for x in rows),
                          'source_page_mismatches': sum(x['source_page_matches_header'] is False for x in rows),
                          'operationally_approved': 0}, 'items': rows}
    Path(output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result['summary']))
    for x in rows:
        if not x['identity_found_in_supplied_sources'] or x['source_page_matches_header'] is False:
            print(json.dumps(x, ensure_ascii=False))


if __name__ == '__main__':
    main()
