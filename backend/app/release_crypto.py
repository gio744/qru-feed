import base64, hashlib, json
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey

def canonical_bytes(package: dict) -> bytes:
    return json.dumps(package, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")

def fingerprint(package: dict) -> str:
    return hashlib.sha256(canonical_bytes(package)).hexdigest()

def verify_ed25519(package: dict, signature_b64: str, public_key_b64: str) -> bool:
    try:
        public_key = Ed25519PublicKey.from_public_bytes(base64.b64decode(public_key_b64))
        public_key.verify(base64.b64decode(signature_b64), canonical_bytes(package))
        return True
    except Exception:
        return False
