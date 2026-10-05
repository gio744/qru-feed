import hashlib,json
from .db import connection

def canonical(v):
    return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(",",":"))

def append_audit(cur,event_type,actor_subject,entity_type,entity_id,data,dossier_id=None):
    cur.execute("SELECT event_hash FROM audit_events ORDER BY sequence DESC LIMIT 1")
    row=cur.fetchone()
    prev=row[0] if row else ""
    payload={"event_type":event_type,"actor":actor_subject,"entity_type":entity_type,
             "entity_id":str(entity_id),"data":data,"previous_hash":prev}
    event_hash=hashlib.sha256(canonical(payload).encode()).hexdigest()
    cur.execute("""INSERT INTO audit_events(event_type,entity_type,entity_id,event_data,previous_hash,event_hash,dossier_id)
                   VALUES(%s,%s,%s,%s::jsonb,%s,%s,%s) RETURNING sequence""",
                (event_type,entity_type,str(entity_id),json.dumps({"actor_subject":actor_subject,**data}),
                 prev,event_hash,dossier_id))
    return cur.fetchone()[0],event_hash
