import os
os.environ["QRU_RELEASE_SIGNING_SECRET"]="unit-secret"
from app.releases import structural_gate
from app.release_crypto import canonical_bytes, fingerprint

def package():
    return {"schema_version":1,"content_version":"demo","jurisdiction":"BR","items":[]}

def test_fingerprint_is_deterministic():
    a=package()
    b={"items":[],"jurisdiction":"BR","content_version":"demo","schema_version":1}
    assert fingerprint(a)==fingerprint(b)

def test_canonical_bytes_preserve_unicode():
    assert canonical_bytes({"text":"trânsito"}) == '{"text":"trânsito"}'.encode("utf-8")

def test_structural_gate_passes_valid_package():
    ok,details=structural_gate(package())
    assert ok is True
    assert details["missing"]==[]

def test_structural_gate_rejects_missing_items():
    ok,details=structural_gate({"schema_version":1,"content_version":"x","jurisdiction":"BR"})
    assert ok is False
    assert "items" in details["missing"]


def test_structural_gate_rejects_version_outside_signature():
    ok, details = structural_gate(package(), "other-version")
    assert ok is False
    assert details["version_matches"] is False

def test_structural_gate_accepts_matching_version():
    assert structural_gate(package(), "demo")[0] is True


def test_rejects_unsupported_schema_and_blank_identity():
    for field, value in [("schema_version", 2), ("schema_version", True), ("content_version", " "), ("jurisdiction", "")]:
        candidate = package()
        candidate[field] = value
        assert structural_gate(candidate)[0] is False


def test_rejects_duplicate_or_incomplete_items():
    for items in [[{}], [{"stable_key": "", "title": "Título"}],
                  [{"stable_key": "x", "title": "A"}, {"stable_key": "x", "title": "B"}],
                  [{"stable_key": "x", "title": "A", "article": 162}], [None]]:
        candidate = package()
        candidate["items"] = items
        assert structural_gate(candidate)[0] is False


def test_accepts_searchable_item():
    candidate = package()
    candidate["items"] = [{"stable_key": "x", "title": "Ficha técnica", "search_text": "consulta", "article": "162", "code": "501-00"}]
    assert structural_gate(candidate)[0] is True
