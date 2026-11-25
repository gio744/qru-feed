"""Import official CONTRAN table from a locally saved HTML snapshot."""
import argparse
import json
import re
from pathlib import Path
from urllib.parse import urljoin, urlparse
from bs4 import BeautifulSoup

SOURCE = "https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran"

def parse(html):
    soup = BeautifulSoup(html, "html.parser")
    records = {}
    for table in soup.find_all("table"):
        for row in table.find_all("tr"):
            cells = row.find_all("td", recursive=False)
            if len(cells) != 5:
                continue
            link = cells[0].find("a")
            if not link or not re.fullmatch(r"\d{1,4}", link.get_text(strip=True)):
                continue
            number = int(link.get_text(strip=True))
            date, publication, subject, situation = [c.get_text(" ", strip=True) for c in cells[1:]]
            match = re.search(r"\b\d{1,2}[./-]\d{1,2}[./-](\d{4}|\d{2})\b", date)
            if not subject:
                raise ValueError(f"Missing subject: {number}")
            year = int(match[1]) if match else None
            if year is not None:
                year = year if year >= 1900 else (1900 + year if year >= 90 else 2000 + year)
            links = []
            for a in row.find_all("a", href=True):
                url = urljoin(SOURCE, a["href"])
                p = urlparse(url)
                if p.scheme == "https" and p.hostname in {"www.gov.br", "gov.br", "www.in.gov.br", "in.gov.br"}:
                    links.append({"label": a.get_text(" ", strip=True), "url": url})
            primary = urljoin(SOURCE, link.get("href", ""))
            if not any(x["url"] == primary for x in links):
                raise ValueError(f"Unverified primary URL: {number}")
            if not match and "retificação" in subject.casefold():
                key = f"{number}/correction/{len(records)}"
            else:
                key = f"{number}/{year}"
            if key in records:
                raise ValueError(f"Duplicate: {key}")
            records[key] = dict(number=number, year=year, date=date, publication=publication,
                                subject=subject, officialSituation=situation,
                                status="pending_review", source=primary, links=links,
                                documentKind="correction" if not match and "retificação" in subject.casefold() else "resolution")
    if not records:
        raise ValueError("No records found")
    return sorted(records.values(), key=lambda r: (r["year"] or 0, r["number"]), reverse=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("html", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    records = parse(args.html.read_text(encoding="utf-8"))
    args.output.write_text(json.dumps({"source": SOURCE, "records": records}, ensure_ascii=False, indent=2), encoding="utf-8")
