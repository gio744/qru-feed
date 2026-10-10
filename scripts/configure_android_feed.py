"""Configure public feed coordinates; never takes a private signing key."""
import argparse, base64
from pathlib import Path
from urllib.parse import urlparse
from xml.etree import ElementTree as ET
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat


def configure(url, public_key, target):
    parsed = urlparse(url)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.hostname.endswith('.invalid') or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('Provide the real HTTPS API base URL without credentials, query or fragment')
    raw = base64.b64decode(public_key, validate=True)
    key = Ed25519PublicKey.from_public_bytes(raw)
    x509 = base64.b64encode(key.public_bytes(Encoding.DER, PublicFormat.SubjectPublicKeyInfo)).decode()
    tree = ET.parse(target)
    entries = {node.attrib.get('name'): node for node in tree.getroot().findall('string')}
    entries['qru_legal_api_base_url'].text = url.rstrip('/') + '/'
    entries['qru_legal_public_key_x509_b64'].text = x509
    tree.write(target, encoding='utf-8', xml_declaration=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', required=True)
    parser.add_argument('--public-key-base64', required=True)
    args = parser.parse_args()
    target = Path(__file__).resolve().parents[1] / 'android/app/src/main/res/values/qru_config.xml'
    configure(args.url, args.public_key_base64, target)
    print('Android feed configuration updated; rebuild the APK to apply it.')
