#!/usr/bin/env python3
"""Regression tests for fail-closed operational fiche draft validation."""
import copy
import json
import unittest
from pathlib import Path

from validate_operational_drafts import validate

BASE = json.loads(Path("docs/drafts/alcool-recusa-lote-01.json").read_text(encoding="utf-8"))


class DraftValidationTests(unittest.TestCase):
    def test_current_drafts_are_blocked(self):
        validate(copy.deepcopy(BASE))

    def assert_rejected(self, change):
        data = copy.deepcopy(BASE)
        change(data)
        with self.assertRaises((AssertionError, KeyError, TypeError)):
            validate(data)

    def test_reject_approved_publication(self):
        self.assert_rejected(lambda d: d["items"][0].update(publication="APPROVED"))

    def test_reject_approved_editorial_status(self):
        self.assert_rejected(lambda d: d["items"][0]["review"].update(status="APPROVED"))

    def test_reject_unverified_gate_marked_verified(self):
        self.assert_rejected(lambda d: d["items"][0]["editorialProgress"].update(legalTextVerified=True))

    def test_reject_unreviewed_operational_instruction(self):
        self.assert_rejected(lambda d: d["items"][0]["whenApplicable"].append("autuar sempre"))

    def test_reject_missing_decision_evidence(self):
        self.assert_rejected(lambda d: d["items"][0]["operationalDecisionReview"]["questions"][0].update(evidenceRequired=""))

    def test_reject_duplicate_codes(self):
        self.assert_rejected(lambda d: d["items"][1].update(code=d["items"][0]["code"]))

    def test_reject_empty_review_issues(self):
        self.assert_rejected(lambda d: d["items"][0]["review"].update(unresolvedIssues=[]))

    def test_reject_missing_question(self):
        self.assert_rejected(lambda d: d["items"][0]["operationalDecisionReview"].update(questions=[]))


if __name__ == "__main__":
    unittest.main()
