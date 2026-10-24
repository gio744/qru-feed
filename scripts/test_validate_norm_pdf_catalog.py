"""Unit tests for the offline official PDF catalog gate."""
import copy
import json
import unittest
from pathlib import Path

from validate_norm_pdf_catalog import CATALOG, validate


class CatalogValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.entries = json.loads(CATALOG.read_text(encoding="utf-8"))

    def test_bundled_catalog_is_valid(self):
        self.assertEqual(len(self.entries), validate(self.entries))

    def test_duplicate_filename_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            validate([self.entries[0], copy.deepcopy(self.entries[0])])

    def test_modified_hash_is_rejected(self):
        entries = copy.deepcopy(self.entries[:1])
        entries[0]["sha256"] = "0" * 63
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            validate(entries)

    def test_unapproved_source_is_rejected(self):
        entries = copy.deepcopy(self.entries[:1])
        entries[0]["source"] = "https://example.com/document.pdf"
        with self.assertRaisesRegex(ValueError, "Unapproved"):
            validate(entries)

    def test_invalid_annex_page_is_rejected(self):
        entries = copy.deepcopy(self.entries[:1])
        entries[0]["annexPage"] = entries[0]["pageCount"] + 1
        with self.assertRaisesRegex(ValueError, "annex"):
            validate(entries)

    def test_invalid_pdf_filename_is_rejected(self):
        entries = copy.deepcopy(self.entries[:1])
        entries[0]["file"] = "../unsafe.pdf"
        with self.assertRaisesRegex(ValueError, "filename"):
            validate(entries)


if __name__ == "__main__":
    unittest.main()
