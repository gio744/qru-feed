import hashlib
from .release_crypto import canonical_bytes, verify_ed25519
from .config import settings
from fastapi import APIRouter, HTTPException, Response
from .db import connection

router=APIRouter(prefix="/legal/releases",tags=["legal-distribution"])

@router.get("/current/package")
def current_package_bytes():
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT package_payload,fingerprint,package_signature,version
                           FROM legal_releases
                           WHERE status='ACTIVE'
                             AND (effective_from IS NULL OR effective_from<=now())
                           ORDER BY activated_at DESC NULLS LAST, created_at DESC, id DESC
                           LIMIT 1""")
            row=cur.fetchone()
    if not row:
        raise HTTPException(404,"No active legal release")
    payload=row[0]
    if payload.get("content_version") != row[3]:
        raise HTTPException(500,"Stored release version does not match signed package")
    # IMPORTANT: production signing must sign the same canonical byte representation served here.
    raw=canonical_bytes(payload)
    actual=hashlib.sha256(raw).hexdigest()
    if actual != row[1]:
        raise HTTPException(500,"Stored release fingerprint does not match distributable bytes")
    if not settings.release_public_key_b64 or not verify_ed25519(payload,row[2] or "",settings.release_public_key_b64):
        raise HTTPException(500,"Stored release signature is invalid")
    return Response(
        content=raw,
        media_type="application/json",
        headers={
            "X-QRU-Fingerprint":row[1],
            "X-QRU-Signature":row[2] or "",
            "X-QRU-Version":row[3],
            "Cache-Control":"no-transform"
        }
    )
