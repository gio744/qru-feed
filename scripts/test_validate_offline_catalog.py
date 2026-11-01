import unittest
from validate_offline_catalog import validate


def sample():
    return {
        "file": "Resolucao9132022.pdf",
        "title": "Resolução 913/2022",
        "sha256": "a" * 64,
        "pageCount": 4,
        "source": "https://www.gov.br/transportes/resolucao.pdf",
    }


class OfflineCatalogValidationTests(unittest.TestCase):
    def test_valid(self):
        self.assertEqual(validate([sample()]), [])

    def test_out_of_order_resolutions(self):
        newer = sample()
        newer["title"] = "Resolução 969/2022"
        newer["file"] = "resolucao9692022.pdf"
        self.assertTrue(any("chronological" in error for error in validate([newer, sample()])))

    def test_duplicate_resolution_number_with_distinct_title(self):
        other = sample()
        other["file"] = "another.pdf"
        other["title"] = "Resolução 913/2022"
        self.assertTrue(any("duplicate resolution numbers" in error for error in validate([sample(), other])))

    def test_duplicate_file(self):
        other = sample()
        other["title"] = "Resolução 914/2022"
        self.assertTrue(any("duplicate filename" in error for error in validate([sample(), other])))

    def test_invalid_digest(self):
        entry = sample()
        entry["sha256"] = "123"
        self.assertTrue(any("SHA-256" in error for error in validate([entry])))

    def test_non_official_source(self):
        entry = sample()
        entry["source"] = "https://www.gov.br.evil.example/pdf"
        self.assertTrue(any("official" in error for error in validate([entry])))

    def test_annex_page_outside_document(self):
        entry = sample()
        entry["annexPage"] = 5
        self.assertTrue(any("annex page" in error for error in validate([entry])))

    def test_valid_annex_page(self):
        entry = sample()
        entry["annexPage"] = 4
        self.assertEqual(validate([entry]), [])

    def test_pending_resolution_matches_dotted_title(self):
        entry = sample()
        entry["title"] = "Resolução 1.026/2026"
        pending = [{"number": 1026, "year": 2026}]
        self.assertEqual(validate([entry], pending_items=pending), [])

    def test_pending_resolution_missing(self):
        pending = [{"number": 1026, "year": 2026}]
        self.assertTrue(any("1026/2026" in error for error in validate([sample()], pending_items=pending)))

    def test_missing_text_index_is_rejected(self):
        entry = sample()
        entry["textIndex"] = "missing_index_9132022.json"
        self.assertTrue(any("missing or invalid text index" in error for error in validate([entry])))

    def test_unsafe_text_index_path_is_rejected(self):
        entry = sample()
        entry["textIndex"] = "../untrusted.json"
        self.assertTrue(any("invalid text index filename" in error for error in validate([entry])))

    def test_invalid_page_count(self):
        entry = sample()
        entry["pageCount"] = 0
        self.assertTrue(any("page count" in error for error in validate([entry])))


if __name__ == "__main__":
    unittest.main()
