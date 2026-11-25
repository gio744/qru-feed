import unittest
from import_contran_catalog import parse

def row(number="1033", href="https://www.gov.br/test.pdf", situation=""):
    return f'<tr><td><a href="{href}">{number}</a></td><td>04/11/2026</td><td>DOU</td><td>Assunto de teste</td><td>{situation}</td></tr>'

def table(*rows):
    return "<table>" + "".join(rows) + "</table>"

class ImportCatalogTest(unittest.TestCase):
    def test_blank_status_is_pending(self):
        record = parse(table(row()))[0]
        self.assertEqual(record["status"], "pending_review")
        self.assertEqual(record["year"], 2026)
        self.assertEqual(record["number"], 1033)

    def test_original_status_preserved(self):
        record = parse(table(row(situation="Revogada parcialmente")))[0]
        self.assertEqual(record["officialSituation"], "Revogada parcialmente")
        self.assertEqual(record["status"], "pending_review")

    def test_duplicate_rejected(self):
        with self.assertRaises(ValueError):
            parse(table(row(), row()))

    def test_untrusted_primary_rejected(self):
        with self.assertRaises(ValueError):
            parse(table(row(href="https://example.org/test.pdf")))

    def test_empty_rejected(self):
        with self.assertRaises(ValueError):
            parse("<html></html>")

    def test_order(self):
        self.assertEqual([r["number"] for r in parse(table(row("1032"), row("1033")))], [1033, 1032])

if __name__ == "__main__":
    unittest.main()
