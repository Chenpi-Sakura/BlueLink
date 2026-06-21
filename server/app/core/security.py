"""安全工具 — Fernet 加密解密（V2.0 §9.6）"""

import os
from pathlib import Path
from cryptography.fernet import Fernet

_FERNET_KEY_FILE = Path(__file__).resolve().parent.parent.parent / ".fernet_key"


def _load_or_generate_key() -> bytes:
    env_key = os.getenv("BLUELINK_FERNET_KEY")
    if env_key:
        return env_key.encode()
    if _FERNET_KEY_FILE.exists():
        return _FERNET_KEY_FILE.read_text().strip().encode()
    key = Fernet.generate_key()
    _FERNET_KEY_FILE.write_text(key.decode())
    return key


_fernet = Fernet(_load_or_generate_key())


def encrypt(plaintext: str) -> bytes:
    return _fernet.encrypt(plaintext.encode("utf-8"))


def decrypt(token: bytes) -> str:
    return _fernet.decrypt(token).decode("utf-8")
