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
    package = {'schema_version': 1, 'content_version': version, 'jurisdiction': 'BR', 'ready': True,
               'items': [{'stable_key': 'integration-example', 'title': 'Technical test fixture', 'ready': True}]}
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
    assert client.post(f'/admin/releases/{rid}/regression', headers=headers, json={'test_id': 'technical-approved-package', 'passed': True, 'critical': True}).status_code == 200
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


@pytest.mark.parametrize("package_ready,item_ready", [(False, True), (True, False)])
def test_pending_content_cannot_replace_active_release(monkeypatch, package_ready, item_ready):
    assert settings.env == 'test'
    assert settings.database_url.rstrip('/').endswith('/qru_test')
    subject = 'pending-integration-' + uuid.uuid4().hex
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute('INSERT INTO users(external_subject,display_name) VALUES(%s,%s) RETURNING id',
                        (subject, subject))
            user_id = cur.fetchone()[0]
            cur.execute("INSERT INTO user_roles(user_id,role_id) VALUES(%s,'ADMIN')", (user_id,))
            cur.execute("SELECT id::text FROM legal_releases WHERE status='ACTIVE' ORDER BY id")
            active_before = cur.fetchall()
    key = Ed25519PrivateKey.generate()
    public = key.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    monkeypatch.setattr(settings, 'release_public_key_b64', base64.b64encode(public).decode())
    version = 'pending-integration-' + uuid.uuid4().hex
    package = {'schema_version': 1, 'content_version': version, 'jurisdiction': 'BR',
               'ready': package_ready,
               'items': [{'stable_key': 'pending-example', 'title': 'Technical fixture', 'ready': item_ready}]}
    signature = base64.b64encode(key.sign(canonical_bytes(package))).decode()
    client = TestClient(app)
    headers = {'Authorization': 'Bearer ' + create_dev_token(subject)}
    received = client.post('/admin/releases', headers=headers,
                           json={'version': version, 'package': package, 'signature': signature})
    assert received.status_code == 200 and received.json()['status'] == 'CANDIDATE'
    rid = received.json()['id']
    assert client.post(f'/admin/releases/{rid}/impact-review', headers=headers,
                       json={'passed': True}).status_code == 200
    assert client.post(f'/admin/releases/{rid}/regression', headers=headers,
                       json={'test_id': 'otherwise-passing', 'passed': True, 'critical': True}).status_code == 200
    blocked = client.post(f'/admin/releases/{rid}/activate', headers=headers)
    assert blocked.status_code == 409
    assert blocked.json()['detail'] == 'Content readiness gate failed'
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id::text FROM legal_releases WHERE status='ACTIVE' ORDER BY id")
            assert cur.fetchall() == active_before
            cur.execute("SELECT status,package_payload FROM legal_releases WHERE id=%s", (rid,))
            status, stored_package = cur.fetchone()
            assert status == 'CANDIDATE' and stored_package == package
            cur.execute("SELECT status,snapshot FROM release_dossiers WHERE release_id=%s ORDER BY attempt_no", (rid,))
            dossier_status, snapshot = cur.fetchone()
            assert dossier_status == 'BLOCKED'
            assert snapshot['activation_checks']['content_readiness']['passed'] is False
            assert all(g['passed'] for g in snapshot['basic_gates'])
            assert snapshot['impact']['passed'] is True
            assert all(t['passed'] for t in snapshot['regression'])
            cur.execute("SELECT event_type FROM audit_events WHERE entity_id=%s ORDER BY sequence", (rid,))
            assert 'RELEASE_ACTIVATION_BLOCKED' in [r[0] for r in cur.fetchall()]
