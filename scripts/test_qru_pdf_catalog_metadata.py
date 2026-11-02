import unittest

from check_qru_pdf_catalog_metadata import validate


class CatalogMetadataTests(unittest.TestCase):
    def setUp(self):
        self.item = {
            "file": "norma.pdf",
            "title": "Resolução 1/2026",
            "sha256": "a" * 64,
            "pageCount": 2,
            "annexPage": 1,
            "source": "https://www.gov.br/example/norma.pdf",
        }

    def test_valid_entry(self):
        self.assertEqual(validate([self.item]), [])

    def test_rejects_duplicate_filename(self):
        duplicate = dict(self.item, title="Resolução 2/2026", source="https://gov.br/example/2.pdf")
        self.assertTrue(any("duplicate filename" in error for error in validate([self.item, duplicate])))

    def test_rejects_invalid_sha(self):
        self.assertTrue(any("SHA-256" in error for error in validate([dict(self.item, sha256="bad")])))

    def test_rejects_annex_page_out_of_bounds(self):
        self.assertTrue(any("annexPage" in error for error in validate([dict(self.item, annexPage=3)])))

    def test_rejects_insecure_source(self):
        self.assertTrue(any("gov.br HTTPS" in error for error in validate([dict(self.item, source="http://example.com/norma.pdf")])))

    def test_rejects_empty_catalog(self):
        self.assertTrue(validate([]))


if __name__ == "__main__":
    unittest.main()
