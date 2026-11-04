"""Regression tests for APK registration integrity; no Android SDK required."""
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

SCRIPT = Path(__file__).with_name("register_apk.py")


class RegisterApkTests(unittest.TestCase):
    def run_register(self, apk):
        return subprocess.run([sys.executable, str(SCRIPT), str(apk)], capture_output=True, text=True)

    def test_valid_minimal_apk_is_registered_but_not_device_approved(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "app-debug.apk"
            with zipfile.ZipFile(apk, "w") as z:
                z.writestr("AndroidManifest.xml", b"binary-placeholder")
            result = self.run_register(apk)
            self.assertEqual(result.returncode, 0, result.stderr)
            record = json.loads(Path(str(apk) + ".record.json").read_text())
            self.assertEqual(record["integrity"], "PASS")
            self.assertEqual(record["install_status"], "NOT_YET_DEVICE_TESTED")
            self.assertEqual(len(record["sha256"]), 64)
            self.assertTrue(Path(str(apk) + ".sha256").exists())

    def test_rejects_non_zip(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "bad.apk"
            apk.write_bytes(b"not a zip")
            self.assertNotEqual(self.run_register(apk).returncode, 0)

    def test_rejects_zip_without_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "bad.apk"
            with zipfile.ZipFile(apk, "w") as z:
                z.writestr("file.txt", "test")
            self.assertNotEqual(self.run_register(apk).returncode, 0)

    def test_rejects_missing_apk(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertNotEqual(self.run_register(Path(tmp) / "missing.apk").returncode, 0)


if __name__ == "__main__":
    unittest.main()
