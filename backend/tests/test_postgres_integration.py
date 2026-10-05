"""Execute only against the disposable qru_test database."""
import os, uuid, base64
import pytest
from fastapi.testclient import TestClient
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat
from app.main import app
from app.config import settings
from app.auth import create_dev_token
from app.db import connection
from app.release_crypto import canonical_bytes, fingerprint

pytestmark = pytest.mark.skipif(not os.getenv('QRU_RUN_INTEGRATION'), reason='Requires disposable PostgreSQL')

def test_publish_review_activate_and_download(monkeypatch):
    assert settings.env == 'test'
    assert settings.database_url.rstrip('/').endswith('/qru_test')
    subject = 'integration-' + uuid.uuid4().hex
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute('INSERT INTO users(external_subject,display_name) VALUES(%s,%s) RETURNING id', (subject, subject))
            user_id = cur.fetchone()[0]
            cur.execute("INSERT INTO user_roles(user_id,role_id) VALUES(%s,'ADMIN')", (user_id,))
    key = Ed25519PrivateKey.generate()
    public = key.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    monkeypatch.setattr(settings, 'release_public_key_b64', base64.b64encode(public).decode())
    version = 'integration-' + uuid.uuid4().hex
    package = {'schema_version': 1, 'content_version': version, 'jurisdiction': 'BR', 'items': []}
    signature = base64.b64encode(key.sign(canonical_bytes(package))).decode()
    client = TestClient(app)
    headers = {'Authorization': 'Bearer ' + create_dev_token(subject)}
    response = client.post('/admin/releases', headers=headers, json={'version': version, 'package': package, 'signature': signature})
    assert response.status_code == 200, response.text
    assert response.json()['status'] == 'CANDIDATE'
    rid = response.json()['id']
    # A candidate without legal review must not become active.
    response = client.post(f'/admin/releases/{rid}/activate', headers=headers)
    assert response.status_code == 409
    assert client.post(f'/admin/releases/{rid}/impact-review', headers=headers, json={'passed': True}).status_code == 200
    assert client.post(f'/admin/releases/{rid}/regression', headers=headers, json={'test_id': 'technical-empty-package', 'passed': True, 'critical': True}).status_code == 200
    response = client.post(f'/admin/releases/{rid}/activate', headers=headers)
    assert response.status_code == 200, response.text
    downloaded = client.get('/legal/releases/current/package')
    assert downloaded.status_code == 200, downloaded.text
    assert downloaded.content == canonical_bytes(package)
    assert downloaded.headers['x-qru-fingerprint'] == fingerprint(package)
    assert downloaded.headers['x-qru-signature'] == signature
    dossiers = client.get(f'/admin/releases/{rid}/dossiers', headers=headers)
    assert dossiers.status_code == 200
    assert [d['status'] for d in dossiers.json()] == ['BLOCKED', 'APPROVED']
    assert client.post(f'/admin/releases/{rid}/activate', headers=headers).status_code == 409
    assert client.post('/admin/releases', json={}).status_code == 401
