"""Exercise activation with other gates passing and persist blocked attempts."""
from contextlib import contextmanager
from copy import deepcopy
import json

import pytest
from fastapi import HTTPException
from app import dossier, releases
from app.release_readiness import content_readiness_gate


def package():
    return {"schema_version": 1, "content_version": "test", "jurisdiction": "BR",
            "ready": True, "items": [{"stable_key": "551-71", "title": "Example", "ready": True}]}


@pytest.mark.parametrize("value", [False, None, "true", "false", 1, 0])
def test_package_flag_must_be_literal_true(value):
    payload = package()
    payload["ready"] = value
    assert content_readiness_gate(payload)[0] is False


@pytest.mark.parametrize("value", [False, None, "true", 1])
def test_item_flag_must_be_literal_true(value):
    payload = package()
    payload["items"][0]["ready"] = value
    assert content_readiness_gate(payload)[0] is False


def test_missing_flags_are_not_approval():
    payload = package()
    del payload["ready"]
    del payload["items"][0]["ready"]
    ok, details = content_readiness_gate(payload)
    assert not ok
    assert details["pending_item_count"] == 1


@pytest.mark.parametrize("items", [[], None, {}, [None], ["ready"]])
def test_empty_or_malformed_items_fail_closed(items):
    payload = package()
    payload["items"] = items
    assert content_readiness_gate(payload)[0] is False


@pytest.mark.parametrize("payload", [None, [], "ready"])
def test_missing_or_invalid_payload_fails_closed(payload):
    assert content_readiness_gate(payload)[0] is False


def test_valid_declarations_pass_without_mutating_input():
    payload = package()
    before = deepcopy(payload)
    ok, details = content_readiness_gate(payload)
    assert ok and details["pending_item_count"] == 0
    assert payload == before


def test_one_pending_item_blocks_other_approved_items():
    payload = package()
    payload["items"].append({"stable_key": "other", "ready": False})
    ok, details = content_readiness_gate(payload)
    assert not ok
    assert details["pending_items_sample"] == [{"index": 1, "stable_key": "other"}]


def test_large_pending_catalog_has_bounded_diagnostic_sample():
    payload = package()
    payload["items"] = [{"stable_key": str(n), "ready": False} for n in range(418)]
    ok, details = content_readiness_gate(payload)
    assert not ok and details["pending_item_count"] == 418
    assert len(details["pending_items_sample"]) == 20


class FakeCursor:
    def __init__(self, payload, basic=None, impact=True, regression=None):
        self.payload = payload
        self.basic = [(True,), (True,)] if basic is None else basic
        self.impact = impact
        self.regression = [(True, True)] if regression is None else regression
        self.sql = []
        self.last = ""

    def execute(self, sql, args=None):
        self.last = sql
        self.sql.append((sql, args))

    def fetchone(self):
        if self.last.startswith("SELECT status,effective_from,package_payload"):
            return ("CANDIDATE", None, self.payload)
        if self.last.startswith("SELECT passed FROM release_impact_reviews"):
            return (True,) if self.impact else None
        if "INSERT INTO legal_releases" in self.last:
            return ("candidate-id",)
        raise AssertionError("Unexpected fetchone: " + self.last)

    def fetchall(self):
        if self.last.startswith("SELECT passed FROM release_gate_results"):
            return self.basic
        if self.last.startswith("SELECT critical,passed"):
            return self.regression
        raise AssertionError("Unexpected fetchall: " + self.last)

    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False


class FakeConnection:
    def __init__(self, cursor):
        self.cur = cursor
        self.committed = False

    def cursor(self):
        return self.cur

    @contextmanager
    def transaction(self):
        yield
        self.committed = True


def install_database(monkeypatch, payload, **options):
    cursor = FakeCursor(payload, **options)
    conn = FakeConnection(cursor)
    events, attempts = [], []

    @contextmanager
    def connection():
        yield conn

    def create_dossier(cur, rid, actor, status, activation_checks=None):
        attempts.append((status, activation_checks))
        return "dossier-id", 1, {}

    monkeypatch.setattr(releases, "connection", connection)
    monkeypatch.setattr(releases, "create_dossier", create_dossier)
    monkeypatch.setattr(releases, "append_audit", lambda *args: events.append(args))
    return cursor, conn, attempts, events


def test_activation_blocks_pending_content_even_if_all_reviews_pass(monkeypatch):
    payload = package()
    payload["ready"] = False
    payload["items"][0]["ready"] = False
    cur, conn, attempts, events = install_database(monkeypatch, payload)
    with pytest.raises(HTTPException) as exc:
        releases.activate("candidate-id", actor={"subject": "reviewer"})
    assert exc.value.status_code == 409
    assert exc.value.detail == "Content readiness gate failed"
    assert conn.committed
    assert attempts[0][0] == "BLOCKED"
    assert attempts[0][1]["content_readiness"]["details"]["pending_item_count"] == 1
    assert events[0][1] == "RELEASE_ACTIVATION_BLOCKED"
    assert not any("UPDATE legal_releases SET status=" in sql for sql, _ in cur.sql)


def test_legacy_candidate_without_payload_is_blocked(monkeypatch):
    cur, conn, attempts, _ = install_database(monkeypatch, None)
    with pytest.raises(HTTPException) as exc:
        releases.activate("old-candidate", actor={"subject": "reviewer"})
    assert exc.value.status_code == 409
    assert attempts[0][0] == "BLOCKED" and conn.committed


def test_ready_flags_do_not_bypass_impact_review(monkeypatch):
    cur, conn, attempts, _ = install_database(monkeypatch, package(), impact=False)
    with pytest.raises(HTTPException) as exc:
        releases.activate("candidate-id", actor={"subject": "reviewer"})
    assert exc.value.detail == "Impact review not passed"
    assert attempts[0][0] == "BLOCKED"
    assert not any("SET status='ACTIVE'" in sql for sql, _ in cur.sql)


@pytest.mark.parametrize("options", [{"basic": []}, {"basic": [(False,)]},
                                    {"regression": []}, {"regression": [(True, False)]}])
def test_other_failed_gates_still_prevent_activation(monkeypatch, options):
    cur, _, attempts, _ = install_database(monkeypatch, package(), **options)
    with pytest.raises(HTTPException) as exc:
        releases.activate("candidate-id", actor={"subject": "reviewer"})
    assert exc.value.status_code == 409 and attempts[0][0] == "BLOCKED"
    assert not any("SET status='ACTIVE'" in sql for sql, _ in cur.sql)


def test_approved_candidate_can_activate_after_all_gates_pass(monkeypatch):
    cur, conn, attempts, events = install_database(monkeypatch, package())
    result = releases.activate("candidate-id", actor={"subject": "reviewer"})
    assert result["status"] == "ACTIVE" and conn.committed
    assert attempts[0] == ("APPROVED", {"content_readiness": {"passed": True,
        "details": {"errors": [], "item_count": 1, "pending_item_count": 0, "pending_items_sample": []}}})
    assert any("SET status='ACTIVE'" in sql for sql, _ in cur.sql)
    assert events[0][1] == "RELEASE_ACTIVATED"


def test_pending_signed_package_is_still_accepted_for_review(monkeypatch):
    payload = package()
    payload["ready"] = False
    payload["items"][0]["ready"] = False
    cur, _, _, _ = install_database(monkeypatch, payload)
    monkeypatch.setattr(releases.settings, "release_public_key_b64", "configured")
    monkeypatch.setattr(releases, "verify_ed25519", lambda *args: True)
    result = releases.create_release(releases.ReleaseCandidate(version="test", package=payload,
        signature="test-signature"), actor={"subject": "editor"})
    assert result["status"] == "CANDIDATE"
    assert not any("status='REJECTED'" in sql for sql, _ in cur.sql)


def test_dossier_records_activation_checks_without_changing_basic_gates(monkeypatch):
    class Cursor:
        def __init__(self): self.sql = []
        def execute(self, sql, args=None): self.sql.append((sql, args))
        def fetchone(self): return (1,) if len(self.sql) == 1 else ("dossier-id",)
    cur = Cursor()
    monkeypatch.setattr(dossier, "build_snapshot", lambda *args: {"basic_gates": [{"passed": True}]})
    monkeypatch.setattr(dossier, "append_audit", lambda *args: None)
    checks = {"content_readiness": {"passed": False, "details": {"errors": ["package_not_ready"]}}}
    _, _, snapshot = dossier.create_dossier(cur, "id", "reviewer", "BLOCKED", activation_checks=checks)
    saved = json.loads(cur.sql[1][1][-1])
    assert saved["activation_checks"] == checks == snapshot["activation_checks"]
    assert saved["basic_gates"] == [{"passed": True}]
