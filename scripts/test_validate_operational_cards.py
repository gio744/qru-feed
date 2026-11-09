#!/usr/bin/env python3
"""Regression tests for the editorial validator (not legal correctness)."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR = ROOT / "scripts/validate_operational_cards.py"
SCHEMA = ROOT / "docs/OPERACIONAL-FICHA-SCHEMA-v1.json"

class OperationalCardValidationTests(unittest.TestCase):
    def check_card(self, card):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "scripts").mkdir()
            (root / "docs").mkdir()
            (root / "content/operational-cards").mkdir(parents=True)
            (root / "scripts/validate_operational_cards.py").write_text(
                VALIDATOR.read_text(encoding="utf-8"), encoding="utf-8"
            )
            (root / "docs/OPERACIONAL-FICHA-SCHEMA-v1.json").write_text(
                SCHEMA.read_text(encoding="utf-8"), encoding="utf-8"
            )
            (root / "content/operational-cards/test.json").write_text(
                json.dumps(card, ensure_ascii=False), encoding="utf-8"
            )
            return subprocess.run(
                [sys.executable, str(root / "scripts/validate_operational_cards.py")],
                capture_output=True, text=True, check=False
            )

    def base_card(self):
        return {
            "codigo": "TESTE", "artigo": "Artigo de teste",
            "descricao": "Apenas teste automatizado",
            "quandoAutuar": "Texto de teste", "quandoNaoAutuar": "Texto de teste",
            "constatacao": "Texto de teste", "abordagem": "Texto de teste",
            "responsavel": "Texto de teste", "medidasAdministrativas": "Texto de teste",
            "fontes": [{"norma": "Fonte fictícia", "url": "https://example.org/norma", "vigencia": "2026-01-01"}],
            "revisao": {"status": "validada", "revisor": "Revisor fictício", "data": "2026-11-09"}
        }

    def test_complete_metadata_passes_structural_check(self):
        self.assertEqual(self.check_card(self.base_card()).returncode, 0)

    def test_placeholder_cannot_be_validated(self):
        card = self.base_card()
        card["quandoAutuar"] = "RASCUNHO — verificar"
        self.assertNotEqual(self.check_card(card).returncode, 0)

    def test_invalid_review_date_is_rejected(self):
        card = self.base_card()
        card["revisao"]["data"] = "ontem"
        self.assertNotEqual(self.check_card(card).returncode, 0)

    def test_non_https_source_is_rejected(self):
        card = self.base_card()
        card["fontes"][0]["url"] = "http://example.org/norma"
        self.assertNotEqual(self.check_card(card).returncode, 0)

    def test_pending_card_does_not_claim_validation(self):
        card = self.base_card()
        card["revisao"] = {"status": "pendente", "revisor": "", "data": ""}
        card["fontes"] = []
        self.assertEqual(self.check_card(card).returncode, 0)

if __name__ == "__main__":
    unittest.main()
