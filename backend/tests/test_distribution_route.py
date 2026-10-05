import base64
from contextlib import contextmanager
from unittest.mock import patch
from fastapi.testclient import TestClient
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat
from app.main import app
from app.release_crypto import canonical_bytes, fingerprint
from app.config import settings

client = TestClient(app)

@contextmanager
def database(row):
    class Cursor:
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def execute(self, sql):
            assert "status='ACTIVE'" in sql
            assert 'effective_from<=now()' in sql
        def fetchone(self): return row
    class Connection:
        def cursor(self): return Cursor()
    yield Connection()

def request(row):
    with patch('app.package_distribution.connection', lambda: database(row)):
        return client.get('/legal/releases/current/package')

def test_no_active_release():
    assert request(None).status_code == 404

def test_signed_package_exact_bytes_and_headers(monkeypatch):
    package = {'items': [], 'content_version': 'test-1', 'text': 'Fiscalização'}
    key = Ed25519PrivateKey.generate()
    public = key.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    monkeypatch.setattr(settings, 'release_public_key_b64', base64.b64encode(public).decode())
    signature = base64.b64encode(key.sign(canonical_bytes(package))).decode()
    response = request((package, fingerprint(package), signature, 'test-1'))
    assert response.status_code == 200
    assert response.content == canonical_bytes(package)
    assert response.headers['x-qru-signature'] == signature
    assert response.headers['x-qru-fingerprint'] == fingerprint(package)

def test_corrupt_fingerprint_is_not_distributed():
    assert request(({'items': []}, 'bad', '', 'test-1')).status_code == 500

def test_invalid_signature_is_not_distributed(monkeypatch):
    package = {'items': [], 'content_version': 'test-1'}
    monkeypatch.setattr(settings, 'release_public_key_b64', 'invalid')
    assert request((package, fingerprint(package), 'invalid', 'test-1')).status_code == 500


def test_version_mismatch_is_not_distributed():
    assert request(({"items": [], "content_version": "old"}, "unused", "", "new")).status_code == 500
