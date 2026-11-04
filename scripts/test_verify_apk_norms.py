"""Regression checks for offline reference validation in the APK."""
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from verify_apk_norms import verify


class VerifyApkNormsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.pdf = b"%PDF-1.4\nfixture"
        self.digest = hashlib.sha256(self.pdf).hexdigest()
        self.docs = [{"file": "manual.pdf", "sha256": self.digest, "pageCount": 3}]
        self.refs = [{"code": "672-61", "file": "manual.pdf", "sha256": self.digest,
                      "firstPage": 2, "lastPage": 3}]
        self.apk = self.root / "app.apk"
        self.catalog = self.root / "catalog.json"
        self.ranges = self.root / "ranges.json"

    def check(self):
        self.catalog.write_text(json.dumps(self.docs), encoding="utf-8")
        self.ranges.write_text(json.dumps(self.refs), encoding="utf-8")
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("assets/norms/manual.pdf", self.pdf)
            archive.writestr("assets/norms/pdf-catalog.json", json.dumps(self.docs))
            archive.writestr("assets/norms/reference-page-ranges.json", json.dumps(self.refs))
        return verify(self.apk, self.catalog, self.ranges)

    def test_valid_reference(self):
        self.assertEqual((1, 1), self.check())

    def test_page_outside_document_is_rejected(self):
        self.refs[0]["lastPage"] = 4
        with self.assertRaisesRegex(ValueError, "outside PDF"):
            self.check()

    def test_reversed_pages_are_rejected(self):
        self.refs[0]["firstPage"] = 3
        self.refs[0]["lastPage"] = 2
        with self.assertRaisesRegex(ValueError, "outside PDF"):
            self.check()

    def test_unknown_document_is_rejected(self):
        self.refs[0]["file"] = "missing.pdf"
        with self.assertRaisesRegex(ValueError, "uncatalogued"):
            self.check()

    def test_wrong_reference_digest_is_rejected(self):
        self.refs[0]["sha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "digest mismatch"):
            self.check()


    def test_duplicate_reference_code_is_rejected(self):
        self.refs.append(dict(self.refs[0]))
        with self.assertRaisesRegex(ValueError, "Duplicate reference codes"):
            self.check()

    def test_missing_pdf_inside_apk_is_rejected(self):
        self.check()
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("assets/norms/pdf-catalog.json", json.dumps(self.docs))
            archive.writestr("assets/norms/reference-page-ranges.json", json.dumps(self.refs))
        with self.assertRaisesRegex(ValueError, "Missing PDF in APK"):
            verify(self.apk, self.catalog, self.ranges)

    def test_corrupted_pdf_inside_apk_is_rejected(self):
        self.check()
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("assets/norms/manual.pdf", b"altered")
            archive.writestr("assets/norms/pdf-catalog.json", json.dumps(self.docs))
            archive.writestr("assets/norms/reference-page-ranges.json", json.dumps(self.refs))
        with self.assertRaisesRegex(ValueError, "PDF digest mismatch"):
            verify(self.apk, self.catalog, self.ranges)

    def test_bundled_metadata_mismatch_is_rejected(self):
        self.check()
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("assets/norms/manual.pdf", self.pdf)
            archive.writestr("assets/norms/pdf-catalog.json", json.dumps(self.docs))
            archive.writestr("assets/norms/reference-page-ranges.json", "[]")
        with self.assertRaisesRegex(ValueError, "Bundled metadata differs"):
            verify(self.apk, self.catalog, self.ranges)

if __name__ == "__main__":
    unittest.main()
