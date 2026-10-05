import base64
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding,PublicFormat
from app.release_crypto import canonical_bytes,verify_ed25519
def test_valid_and_tampered_signature():
    p={"schema_version":1,"content_version":"x","jurisdiction":"BR","items":[]}
    k=Ed25519PrivateKey.generate()
    pub=base64.b64encode(k.public_key().public_bytes(Encoding.Raw,PublicFormat.Raw)).decode()
    sig=base64.b64encode(k.sign(canonical_bytes(p))).decode()
    assert verify_ed25519(p,sig,pub)
    p["content_version"]="tampered"
    assert not verify_ed25519(p,sig,pub)
