"""Environment-configured administrator login; candidates remain gated."""
import base64
import hashlib
import hmac
import secrets
import threading
import time
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field

from .config import settings
from .db import connection

router = APIRouter()
_attempts = {}
_lock = threading.Lock()


def password_hash(password):
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 600_000)
    return 'pbkdf2_sha256$600000$' + base64.b64encode(salt).decode() + '$' + base64.b64encode(digest).decode()


def verify_password(password, encoded):
    try:
        algorithm, iterations, salt, digest = encoded.split('$')
        iterations = int(iterations)
        if algorithm != 'pbkdf2_sha256' or not 600_000 <= iterations <= 1_000_000:
            return False
        salt = base64.b64decode(salt, validate=True)
        digest = base64.b64decode(digest, validate=True)
        if len(salt) != 16 or len(digest) != 32:
            return False
        actual = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, iterations)
        return hmac.compare_digest(actual, digest)
    except (ValueError, TypeError):
        return False


class Login(BaseModel):
    username: str = Field(min_length=1, max_length=100)
    password: str = Field(min_length=1, max_length=256)


@router.post('/admin/auth/login')
def login(body: Login, request: Request):
    if not settings.admin_username or not settings.admin_password_hash or len(settings.jwt_secret) < 32:
        raise HTTPException(503, 'Administrator login is not configured')
    # Global process limit bounds password-hash work and does not trust proxy headers.
    now = time.monotonic()
    with _lock:
        recent = [t for t in _attempts.get('login', []) if now - t < 60]
        if len(recent) >= 10:
            raise HTTPException(429, 'Try again in one minute', headers={'Retry-After': '60'})
        _attempts['login'] = recent + [now]
    valid_password = verify_password(body.password, settings.admin_password_hash)
    if not hmac.compare_digest(body.username.encode(), settings.admin_username.encode()) or not valid_password:
        raise HTTPException(401, 'Invalid username or password')
    subject = 'publisher:' + settings.admin_username
    with connection() as conn:
        with conn.transaction():
            with conn.cursor() as cur:
                cur.execute("INSERT INTO users(external_subject,display_name) VALUES(%s,%s) ON CONFLICT(external_subject) DO NOTHING", (subject, settings.admin_username))
                cur.execute('SELECT id,active FROM users WHERE external_subject=%s FOR UPDATE', (subject,))
                account = cur.fetchone()
                if not account or not account[1]:
                    raise HTTPException(403, 'Administrator account is disabled')
                cur.execute("INSERT INTO user_roles(user_id,role_id) VALUES(%s,'ADMIN') ON CONFLICT DO NOTHING", (account[0],))
    issued = datetime.now(timezone.utc)
    token = jwt.encode({'sub': subject, 'iss': settings.jwt_issuer, 'iat': issued, 'exp': issued + timedelta(minutes=15)}, settings.jwt_secret, algorithm='HS256')
    from fastapi.responses import JSONResponse
    return JSONResponse({'access_token': token, 'token_type': 'bearer', 'expires_in': 900}, headers={'Cache-Control': 'no-store'})


@router.get('/publicador', response_class=HTMLResponse)
def publisher():
    nonce = secrets.token_urlsafe(24)
    page = r'''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Publicador QRU</title>
<style nonce="NONCE">body{background:#0d0b10;color:#eee;font:17px system-ui;max-width:650px;margin:32px auto;padding:20px}h1{color:#a15cff}input,button{display:block;box-sizing:border-box;width:100%;margin:12px 0;padding:14px;font:inherit;border-radius:10px}button{background:#974fff;color:white;border:0}button:disabled{opacity:.5}#status{white-space:pre-wrap}section{background:#242027;padding:20px;border-radius:16px;margin:20px 0}</style>
<h1>QRU? Publicador</h1><p>Envie pacotes assinados para conferência. O envio não ativa o conteúdo no aplicativo.</p>
<section><form id="login"><label>Usuário<input id="username" autocomplete="username" required maxlength="100"></label><label>Senha<input id="password" type="password" autocomplete="current-password" required maxlength="256"></label><button id="enter">Entrar</button></form><button id="logout" hidden>Sair</button></section>
<section><label>Pacote candidato JSON<input id="file" type="file" accept=".json,application/json"></label><button id="send" disabled>Enviar para conferência</button></section><p id="status" role="status">Entre para enviar um pacote.</p>
<script nonce="NONCE">let token='',expires=0;const el=id=>document.getElementById(id);const message=t=>el('status').textContent=t;
el('login').onsubmit=async e=>{e.preventDefault();el('enter').disabled=true;try{const r=await fetch('/admin/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:el('username').value,password:el('password').value})});const d=await r.json();if(!r.ok)throw Error(r.status===401?'Usuário ou senha incorretos.':r.status===503?'Acesso administrativo ainda não configurado.':r.status===429?'Aguarde um minuto e tente novamente.':'Não foi possível entrar.');token=d.access_token;expires=Date.now()+d.expires_in*1000;el('password').value='';el('send').disabled=false;el('logout').hidden=false;message('Conectado. Selecione o pacote candidato JSON.');}catch(e){message(e.message)}finally{el('enter').disabled=false}};
el('logout').onclick=()=>{token='';expires=0;el('send').disabled=true;el('logout').hidden=true;message('Sessão encerrada.')};
el('send').onclick=async()=>{if(!token||Date.now()>=expires){el('logout').click();message('Sessão expirada. Entre novamente.');return}const f=el('file').files[0];if(!f){message('Selecione o arquivo JSON.');return}if(f.size>5000000){message('O arquivo deve ter até 5 MB.');return}el('send').disabled=true;try{const b=JSON.parse(await f.text());const r=await fetch('/admin/releases',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+token},body:JSON.stringify({version:b.version,package:b.package,signature:b.signature})});const d=await r.json();if(!r.ok)throw Error(r.status===401?'Sessão expirada. Entre novamente.':r.status===403?'Conta sem permissão para enviar.':'Envio falhou. Código '+r.status);message(d.status==='CANDIDATE'?'Pacote recebido para conferência. ID: '+d.id+'\nAinda não está ativo no aplicativo.':'Pacote rejeitado. Confira assinatura e estrutura.');}catch(e){message(e.message)}finally{el('send').disabled=!token}};
</script></html>'''.replace('NONCE', nonce)
    return HTMLResponse(page, headers={'Cache-Control': 'no-store', 'Content-Security-Policy': "default-src 'none'; script-src 'nonce-" + nonce + "'; style-src 'nonce-" + nonce + "'; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'", 'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer'})
