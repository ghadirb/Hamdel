# decrypt_keys.py
# ابزار توسعه برای تایید محلی اینکه رمزگشایی سمت اپ (Kotlin) دقیقا با encrypt_keys.py هم‌خوان است.
# نیازمندی: pip install cryptography
import sys, base64
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.backends import default_backend

def decrypt_text(blob: bytes, password: str) -> bytes:
    salt = blob[0:16]
    iv = blob[16:28]
    ct = blob[28:]
    kdf = PBKDF2HMAC(algorithm=hashes.SHA256(), length=32, salt=salt, iterations=20000, backend=default_backend())
    key = kdf.derive(password.encode('utf-8'))
    aesgcm = AESGCM(key)
    return aesgcm.decrypt(iv, ct, None)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python decrypt_keys.py <encrypted_base64_file_or_url_download.txt> <password>")
        sys.exit(1)
    infile = sys.argv[1]
    password = sys.argv[2]
    with open(infile, 'rb') as f:
        b64 = f.read().strip()
    blob = base64.b64decode(b64)
    plaintext = decrypt_text(blob, password)
    print("---- DECRYPTED CONTENT ----")
    print(plaintext.decode('utf-8', errors='replace'))
