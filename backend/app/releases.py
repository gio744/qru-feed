import json
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from .db import connection
from .config import settings
from .rbac import require_roles
from .release_crypto import fingerprint, verify_ed25519
from .dossier import create_dossier
from .audit import append_audit

router=APIRouter(prefix="/admin/releases",tags=["releases"])

class ReleaseCandidate(BaseModel):
    version:str
    effective_from:datetime|None=None
    package:dict
    signature:str

class GateReview(BaseModel):
    passed:bool
    details:dict={}

class RegressionResult(BaseModel):
    test_id:str
    critical:bool=True
    passed:bool
    details:dict={}

def structural_gate(package, expected_version=None):
    required=["schema_version","content_version","jurisdiction","items"]
    missing=[x for x in required if x not in package]
    version_matches = expected_version is None or package.get("content_version") == expected_version
    return len(missing)==0 and isinstance(package.get("items"),list) and version_matches,{"missing":missing,"version_matches":version_matches}

@router.post("")
def create_release(body:ReleaseCandidate,actor=Depends(require_roles("EDITOR","ADMIN"))):
    fp=fingerprint(body.package)
    sig_ok=bool(settings.release_public_key_b64) and verify_ed25519(body.package,body.signature,settings.release_public_key_b64)
    struct_ok,details=structural_gate(body.package, body.version)
    gates=[("SIGNATURE_ED25519",sig_ok,{}),("STRUCTURE",struct_ok,details)]
    with connection() as conn:
        with conn.transaction():
            with conn.cursor() as cur:
                cur.execute("""INSERT INTO legal_releases(version,status,fingerprint,effective_from,package_payload,package_signature)
                  VALUES(%s,'CANDIDATE',%s,%s,%s::jsonb,%s) RETURNING id::text""",
                  (body.version,fp,body.effective_from,json.dumps(body.package),body.signature))
                rid=cur.fetchone()[0]
                for name,passed,d in gates:
                    cur.execute("INSERT INTO release_gate_results(release_id,gate_name,passed,details) VALUES(%s,%s,%s,%s::jsonb)",
                                (rid,name,passed,json.dumps(d)))
                if not all(x[1] for x in gates):
                    cur.execute("UPDATE legal_releases SET status='REJECTED' WHERE id=%s",(rid,))
                append_audit(cur,"RELEASE_CANDIDATE_CREATED",actor["subject"],"legal_release",rid,{"version":body.version,"fingerprint":fp})
    return {"id":rid,"fingerprint":fp,"status":"CANDIDATE" if all(x[1] for x in gates) else "REJECTED","gates":[{"name":x[0],"passed":x[1]} for x in gates]}

@router.post("/{release_id}/impact-review")
def impact_review(release_id:str,body:GateReview,actor=Depends(require_roles("REVIEWER","ADMIN"))):
    with connection() as conn:
        with conn.transaction():
            with conn.cursor() as cur:
                cur.execute("SELECT id FROM users WHERE external_subject=%s",(actor["subject"],))
                u=cur.fetchone()
                cur.execute("""INSERT INTO release_impact_reviews(release_id,reviewed_by,passed,details)
                  VALUES(%s,%s,%s,%s::jsonb)
                  ON CONFLICT(release_id) DO UPDATE SET reviewed_by=EXCLUDED.reviewed_by,passed=EXCLUDED.passed,
                  details=EXCLUDED.details,reviewed_at=now()""",(release_id,u[0] if u else None,body.passed,json.dumps(body.details)))
    return {"release_id":release_id,"impact_review":body.passed}

@router.post("/{release_id}/regression")
def regression(release_id:str,body:RegressionResult,actor=Depends(require_roles("REVIEWER","ADMIN"))):
    with connection() as conn:
        with conn.transaction():
            with conn.cursor() as cur:
                cur.execute("""INSERT INTO release_regression_results(release_id,test_id,critical,passed,details)
                 VALUES(%s,%s,%s,%s,%s::jsonb)
                 ON CONFLICT(release_id,test_id) DO UPDATE SET critical=EXCLUDED.critical,passed=EXCLUDED.passed,
                 details=EXCLUDED.details,checked_at=now()""",(release_id,body.test_id,body.critical,body.passed,json.dumps(body.details)))
    return {"release_id":release_id,"test_id":body.test_id,"passed":body.passed}

@router.post("/{release_id}/activate")
def activate(release_id:str,actor=Depends(require_roles("ADMIN"))):
    now=datetime.now(timezone.utc)
    with connection() as conn:
        with conn.transaction():
            with conn.cursor() as cur:
                cur.execute("SELECT status,effective_from FROM legal_releases WHERE id=%s FOR UPDATE",(release_id,))
                row=cur.fetchone()
                if not row: raise HTTPException(404,"Release not found")
                failure=None
                if row[0]!="CANDIDATE": failure="Not an activatable candidate"
                elif row[1] is not None and row[1]>now: failure="Release is not effective yet"
                cur.execute("SELECT passed FROM release_gate_results WHERE release_id=%s",(release_id,))
                basic=[x[0] for x in cur.fetchall()]
                if failure is None and (not basic or not all(basic)): failure="Basic gates failed"
                cur.execute("SELECT passed FROM release_impact_reviews WHERE release_id=%s",(release_id,))
                impact=cur.fetchone()
                if failure is None and (not impact or not impact[0]): failure="Impact review not passed"
                cur.execute("SELECT critical,passed FROM release_regression_results WHERE release_id=%s",(release_id,))
                tests=cur.fetchall()
                if failure is None and (not tests or any(c and not p for c,p in tests)):
                    failure="Critical regression gate failed or missing"
                dossier_id,attempt,_=create_dossier(cur,release_id,actor["subject"],"BLOCKED" if failure else "APPROVED")
                if failure:
                    append_audit(cur,"RELEASE_ACTIVATION_BLOCKED",actor["subject"],"legal_release",release_id,
                                 {"reason":failure,"attempt_no":attempt},dossier_id)
                    # commit dossier/audit, then return conflict after transaction
                else:
                    cur.execute("UPDATE legal_releases SET status='SUPERSEDED',superseded_at=now() WHERE status='ACTIVE'")
                    cur.execute("UPDATE legal_releases SET status='ACTIVE',activated_at=now() WHERE id=%s",(release_id,))
                    append_audit(cur,"RELEASE_ACTIVATED",actor["subject"],"legal_release",release_id,
                                 {"attempt_no":attempt},dossier_id)
    if failure:
        raise HTTPException(409,failure)
    return {"id":release_id,"status":"ACTIVE","dossier_id":dossier_id}
