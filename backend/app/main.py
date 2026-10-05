from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from .db import ping_db, connection
from .auth import require_user, create_dev_token
from .config import settings
from .releases import router as releases_router
from .package_distribution import router as distribution_router

app = FastAPI(title="QRU Trânsito API", version="1.2.0-p1")
app.include_router(releases_router)
app.include_router(distribution_router)

class DevLogin(BaseModel):
    subject: str

@app.get("/health")
def health():
    db = ping_db()
    return {"service": "qru-api", "status": "ok" if db else "degraded", "database": "ok" if db else "unavailable"}

@app.post("/auth/dev-token")
def dev_token(body: DevLogin):
    if settings.env == "production":
        raise HTTPException(status_code=404, detail="Not found")
    return {"access_token": create_dev_token(body.subject), "token_type": "bearer"}

@app.get("/legal/releases/current")
def current_legal_release(user=Depends(require_user)):
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """SELECT id::text, version, status, fingerprint, effective_from
                   FROM legal_releases
                   WHERE status='ACTIVE'
                     AND (effective_from IS NULL OR effective_from <= now())
                   ORDER BY activated_at DESC NULLS LAST, created_at DESC, id DESC
                   LIMIT 1"""
            )
            row = cur.fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="No active legal release")
    return {
        "id": row[0], "version": row[1], "status": row[2],
        "fingerprint": row[3], "effective_from": row[4],
        "requested_by": user["sub"]
    }

@app.get("/admin/releases/{release_id}/dossiers")
def release_dossiers(release_id: str, user=Depends(require_user)):
    with connection() as conn:
        with conn.cursor() as cur:
            cur.execute("""SELECT id::text,attempt_no,actor_subject,status,snapshot,created_at
                           FROM release_dossiers WHERE release_id=%s ORDER BY attempt_no""",(release_id,))
            rows=cur.fetchall()
    return [{"id":r[0],"attempt_no":r[1],"actor_subject":r[2],"status":r[3],"snapshot":r[4],"created_at":r[5]} for r in rows]
