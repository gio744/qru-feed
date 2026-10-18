from contextlib import contextmanager
from unittest.mock import patch
import os
import uuid
import jwt
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings
from app.publisher import password_hash, verify_password, _attempts

client = TestClient(app)

@pytest.fixture
def configured(monkeypatch):
    _attempts.clear()
    monkeypatch.setattr(settings, 'admin_username', 'maycon')
    monkeypatch.setattr(settings, 'admin_password_hash', password_hash('test-password'))
    monkeypatch.setattr(settings, 'jwt_secret', 'test-secret-longer-than-thirty-two-characters')

def test_password_verification():
    digest = password_hash('correct')
    assert verify_password('correct', digest)
    assert not verify_password('wrong', digest)
    assert not verify_password('correct', 'invalid')

def test_login_disabled_without_configuration(monkeypatch):
    monkeypatch.setattr(settings, 'admin_password_hash', '')
    assert client.post('/admin/auth/login', json={'username':'maycon','password':'test'}).status_code == 503

def test_wrong_credentials_do_not_access_database(configured):
    with patch('app.publisher.connection') as connection:
        response = client.post('/admin/auth/login', json={'username':'maycon','password':'wrong'})
    assert response.status_code == 401
    connection.assert_not_called()

@pytest.mark.parametrize('encoded', [
    'plain-text-password',
    'pbkdf2_sha256$600000$invalid$invalid',
    'pbkdf2_sha256$1$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
    'pbkdf2_sha256$1000001$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
    'other$600000$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
])
def test_malformed_password_configuration_is_unavailable(configured, monkeypatch, encoded):
    monkeypatch.setattr(settings, 'admin_password_hash', encoded)
    with patch('app.publisher.connection') as connection:
        response = client.post('/admin/auth/login', json={'username':'maycon','password':'test'})
    assert response.status_code == 503
    assert encoded not in response.text
    assert not _attempts
    connection.assert_not_called()

@contextmanager
def database():
    class Cursor:
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def execute(self, *args): pass
        def fetchone(self): return ('user-id', True)
    class Connection:
        def cursor(self): return Cursor()
        @contextmanager
        def transaction(self): yield
    yield Connection()

def test_login_token_is_compatible_with_release_auth(configured):
    with patch('app.publisher.connection', database):
        response = client.post('/admin/auth/login', json={'username':'maycon','password':'test-password'})
    assert response.status_code == 200
    assert response.headers['cache-control'] == 'no-store'
    data = response.json()
    claims = jwt.decode(data['access_token'], settings.jwt_secret, algorithms=['HS256'], issuer=settings.jwt_issuer)
    assert claims['sub'] == 'publisher:maycon'
    assert claims['exp'] - claims['iat'] == 900

def test_login_rate_limit(configured):
    _attempts['login'] = [__import__('time').monotonic()] * 10
    response = client.post('/admin/auth/login', json={'username':'maycon','password':'wrong'})
    assert response.status_code == 429

def test_publisher_has_no_activation_controls_or_persistent_token_storage():
    response = client.get('/publicador')
    assert response.status_code == 200
    assert 'localStorage' not in response.text
    assert '/activate' not in response.text
    assert 'frame-ancestors' in response.headers['content-security-policy']

@pytest.mark.skipif(not os.getenv('QRU_RUN_INTEGRATION'), reason='Requires disposable PostgreSQL')
def test_login_provisions_admin_in_postgres(configured, monkeypatch):
    from app.db import connection
    assert settings.env == 'test'
    assert settings.database_url.rstrip('/').endswith('/qru_test')
    username = 'publisher-test-' + uuid.uuid4().hex
    monkeypatch.setattr(settings, 'admin_username', username)
    for _ in range(2):
        response = client.post('/admin/auth/login', json={'username':username,'password':'test-password'})
        assert response.status_code == 200, response.text
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT r.role_id FROM users u JOIN user_roles r ON r.user_id=u.id WHERE external_subject=%s", ('publisher:' + username,))
            assert cur.fetchall() == [('ADMIN',)]
            cur.execute('UPDATE users SET active=false WHERE external_subject=%s', ('publisher:' + username,))
    assert client.post('/admin/auth/login', json={'username':username,'password':'test-password'}).status_code == 403
