import hashlib,json
def canonical(v): return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(",",":"))
def h(event,prev):
    payload={**event,"previous_hash":prev}
    return hashlib.sha256(canonical(payload).encode()).hexdigest()
def test_chain_changes_if_event_changes():
    e={"event_type":"A","actor":"u","entity_type":"x","entity_id":"1","data":{"ok":True}}
    a=h(e,"")
    b=h({**e,"data":{"ok":False}},"")
    assert a!=b
    assert h(e,a)!=h(e,"")
