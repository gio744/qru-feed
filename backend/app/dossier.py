import json
from .audit import append_audit

def build_snapshot(cur,release_id):
    cur.execute("SELECT version,status,fingerprint,effective_from FROM legal_releases WHERE id=%s",(release_id,))
    rel=cur.fetchone()
    cur.execute("SELECT gate_name,passed,details FROM release_gate_results WHERE release_id=%s ORDER BY id",(release_id,))
    gates=[{"name":r[0],"passed":r[1],"details":r[2]} for r in cur.fetchall()]
    cur.execute("SELECT passed,details,reviewed_at FROM release_impact_reviews WHERE release_id=%s",(release_id,))
    impact=cur.fetchone()
    cur.execute("SELECT test_id,critical,passed,details FROM release_regression_results WHERE release_id=%s ORDER BY test_id",(release_id,))
    regression=[{"test_id":r[0],"critical":r[1],"passed":r[2],"details":r[3]} for r in cur.fetchall()]
    return {"release":{"version":rel[0],"status":rel[1],"fingerprint":rel[2],"effective_from":str(rel[3])},
            "basic_gates":gates,
            "impact":None if not impact else {"passed":impact[0],"details":impact[1],"reviewed_at":str(impact[2])},
            "regression":regression}

def create_dossier(cur,release_id,actor_subject,status):
    cur.execute("SELECT COALESCE(MAX(attempt_no),0)+1 FROM release_dossiers WHERE release_id=%s",(release_id,))
    attempt=cur.fetchone()[0]
    snapshot=build_snapshot(cur,release_id)
    cur.execute("""INSERT INTO release_dossiers(release_id,attempt_no,actor_subject,status,snapshot)
                   VALUES(%s,%s,%s,%s,%s::jsonb) RETURNING id::text""",
                (release_id,attempt,actor_subject,status,json.dumps(snapshot)))
    dossier_id=cur.fetchone()[0]
    append_audit(cur,"RELEASE_ATTEMPT",actor_subject,"legal_release",release_id,
                 {"attempt_no":attempt,"status":status},dossier_id)
    return dossier_id,attempt,snapshot
