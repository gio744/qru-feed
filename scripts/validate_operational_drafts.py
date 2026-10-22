#!/usr/bin/env python3
"""Fail closed if editorial draft content is accidentally marked publishable."""
import json
from pathlib import Path

DRAFT = Path("docs/drafts/alcool-recusa-lote-01.json")
EXPECTED = {"516-91", "516-92", "757-91", "757-92"}


def validate(data):
    assert data["schemaVersion"] == "editorial-draft-1"
    assert "NÃO é pacote de publicação" in data["purpose"]
    items = data["items"]
    assert {item["code"] for item in items} == EXPECTED
    assert len(items) == len(EXPECTED)
    required_questions = ("procedimento", "recusa", "sinais", "enquadramento", "medidas")
    for item in items:
        assert item["publication"] == "BLOCKED_PENDING_LEGAL_REVIEW", item["code"]
        assert item["review"]["status"] == "PENDING", item["code"]
        assert item["review"]["unresolvedIssues"], item["code"]
        progress = item["editorialProgress"]
        gates = (
            "legalTextVerified",
            "mbftIndividualFicheVerified",
            "applicabilityApproved",
            "exceptionsApproved",
            "administrativeMeasuresApproved",
            "observationExamplesApproved",
            "independentReviewCompleted",
        )
        assert all(key in progress for key in gates), item["code"]
        if any(progress[key] for key in gates):
            raise AssertionError(
                f"{item['code']}: review gate must not be self-approved in draft"
            )
        assert not any(item[key] for key in ("whenApplicable", "exceptions", "observations", "measures")), item["code"]
        decision = item["operationalDecisionReview"]
        assert decision["status"] == "PENDING_MBFT_CROSSCHECK", item["code"]
        questions = " ".join(q["question"].lower() for q in decision["questions"])
        assert all(term in questions for term in required_questions), item["code"]
        assert all(q["evidenceRequired"].strip() for q in decision["questions"]), item["code"]
        assert decision["doNotPublishUntil"], item["code"]


if __name__ == "__main__":
    validate(json.loads(DRAFT.read_text(encoding="utf-8")))
    print("Editorial drafts remain blocked pending independent legal approval.")
