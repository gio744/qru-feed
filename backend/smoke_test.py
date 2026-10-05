import json, urllib.request

def call(url, method="GET", body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if body is not None: req.add_header("Content-Type","application/json")
    if token: req.add_header("Authorization","Bearer "+token)
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read())

base="http://localhost:8000"
print("HEALTH", call(base+"/health"))
tok=call(base+"/auth/dev-token","POST",{"subject":"p1-smoke"})["access_token"]
print("RELEASE", call(base+"/legal/releases/current", token=tok))
