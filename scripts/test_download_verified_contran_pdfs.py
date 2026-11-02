#!/usr/bin/env python3
"""Offline regression tests for CONTRAN staging failure handling."""
import contextlib
import importlib.util
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

SCRIPT = Path(__file__).with_name("download_verified_contran_pdfs.py")
spec = importlib.util.spec_from_file_location("download_verified_contran_pdfs", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class StagingSafetyTest(unittest.TestCase):
    def test_no_documents_cannot_report_success(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "pending.json"
            manifest.write_text(json.dumps({"items": []}), encoding="utf-8")
            with patch.object(module, "MANIFEST", manifest), patch.object(module, "OUT", root / "staging"):
                with contextlib.redirect_stdout(io.StringIO()):
                    result = module.main()
                self.assertEqual(result, 1)
                report = json.loads((root / "staging" / "manifest-sha256.json").read_text())
                self.assertEqual(report["documents"], [])

    def test_missing_official_urls_cannot_report_success(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "pending.json"
            manifest.write_text(json.dumps({"items": [{"number": 1026}]}), encoding="utf-8")
            with patch.object(module, "MANIFEST", manifest), patch.object(module, "OUT", root / "staging"):
                with contextlib.redirect_stdout(io.StringIO()):
                    result = module.main()
                self.assertEqual(result, 1)
                report = json.loads((root / "staging" / "manifest-sha256.json").read_text())
                self.assertEqual(report["documents"][0]["status"], "pending_official_url")


if __name__ == "__main__":
    unittest.main()
