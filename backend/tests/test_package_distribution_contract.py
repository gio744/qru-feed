import hashlib,json
def canonical(payload):
    return json.dumps(payload,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode("utf-8")
def test_exact_bytes_are_deterministic():
    a={"z":2,"a":"ç","items":[]}
    b={"items":[],"a":"ç","z":2}
    assert canonical(a)==canonical(b)
    assert hashlib.sha256(canonical(a)).hexdigest()==hashlib.sha256(canonical(b)).hexdigest()
