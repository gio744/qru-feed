"""Regression tests for CONTRAN download auditing (no network)."""
import importlib.util
from pathlib import Path
from unittest.mock import patch
import unittest

MODULE = Path(__file__).with_name("audit_pending_contran_pdfs.py")
spec = importlib.util.spec_from_file_location("audit_pending_contran_pdfs", MODULE)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class FakeResponse:
    headers = {"Content-Type": "application/pdf"}
    def __enter__(self):
        return self
    def __exit__(self, *args):
        return False
    def read(self, size):
        return self.data
    def geturl(self):
        return "https://www.gov.br/official.pdf"


class AuditTests(unittest.TestCase):
    def test_summary_lists_pending_documents(self):
        report = audit.summarize([
            {"number": 1026, "status": "pdf_verified_for_download"},
            {"number": 1030, "status": "missing_official_pdf_url"},
        ])
        self.assertEqual(report["total"], 2)
        self.assertEqual(report["verified"], 1)
        self.assertEqual(report["pending"], 1)
        self.assertEqual(report["pending_numbers"], [1030])

    def test_missing_direct_url_fails_closed(self):
        result = audit.inspect({"number": 1030, "official_index": "https://www.gov.br/index"})
        self.assertEqual(result["status"], "missing_official_pdf_url")

    def test_html_is_not_accepted_as_pdf(self):
        response = FakeResponse()
        response.data = b"<html>not a pdf</html>"
        with patch.object(audit.urllib.request, "urlopen", return_value=response):
            result = audit.inspect({"number": 1030, "official_pdf_url": "https://www.gov.br/official.pdf"})
        self.assertEqual(result["status"], "not_a_complete_pdf")

    def test_pdf_integrity_success_is_not_legal_approval(self):
        response = FakeResponse()
        response.data = b"%PDF-1.4\nexample\n%%EOF\n"
        with patch.object(audit.urllib.request, "urlopen", return_value=response):
            result = audit.inspect({"number": 1030, "official_pdf_url": "https://www.gov.br/official.pdf"})
        self.assertEqual(result["status"], "pdf_verified_for_download")
        self.assertIn("sha256", result)
        self.assertNotIn("legal_status", result)


if __name__ == "__main__":
    unittest.main()
