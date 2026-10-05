import base64
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding,PrivateFormat,PublicFormat,NoEncryption
k=Ed25519PrivateKey.generate()
priv=k.private_bytes(Encoding.Raw,PrivateFormat.Raw,NoEncryption())
pub=k.public_key().public_bytes(Encoding.Raw,PublicFormat.Raw)
print("PRIVATE_B64="+base64.b64encode(priv).decode())
print("PUBLIC_B64="+base64.b64encode(pub).decode())
