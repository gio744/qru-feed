import json, sys
from app.releases import fingerprint, expected_signature
p=json.load(open(sys.argv[1],encoding="utf-8"))
print(json.dumps({"fingerprint":fingerprint(p),"signature":expected_signature(p)},indent=2))
