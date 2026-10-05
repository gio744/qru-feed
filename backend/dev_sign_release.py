import base64,json,sys
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from app.release_crypto import canonical_bytes,fingerprint
package=json.load(open(sys.argv[1],encoding="utf-8"))
key=Ed25519PrivateKey.from_private_bytes(base64.b64decode(sys.argv[2]))
sig=key.sign(canonical_bytes(package))
print(json.dumps({"fingerprint":fingerprint(package),"signature":base64.b64encode(sig).decode()},indent=2))
