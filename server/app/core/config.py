"""应用配置 — 从环境变量加载所有设置（V2.1 §5.1）"""

import os
from pathlib import Path
from dotenv import load_dotenv

_env_file = Path(__file__).resolve().parent.parent.parent / ".env"
if _env_file.exists():
    load_dotenv(_env_file)


class Settings:
    # ====== LLM（OpenAI 兼容协议，适配任何厂商） ======
    LLM_API_KEY: str = os.getenv("LLM_API_KEY", "")
    LLM_BASE_URL: str = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")
    LLM_CHAT_MODEL: str = os.getenv("LLM_CHAT_MODEL", "deepseek-chat")
    LLM_EMBED_MODEL: str = os.getenv("LLM_EMBED_MODEL", "embedding-v1")
    LLM_EMBED_BASE_URL: str | None = os.getenv("LLM_EMBED_BASE_URL") or None
    LLM_EMBED_API_KEY: str | None = os.getenv("LLM_EMBED_API_KEY") or None

    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))
    LLM_TIMEOUT_SEC: int = int(os.getenv("LLM_TIMEOUT_SEC", "30"))

    # ====== 数据库 & 存储 ======
    DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./bluelink.db")
    VECTORS_DIR: str = os.getenv("VECTORS_DIR", "./vectors")
    UPLOADS_DIR: str = os.getenv("UPLOADS_DIR", "./uploads")

    # ====== 安全 ======
    BLUELINK_FERNET_KEY: str = os.getenv("BLUELINK_FERNET_KEY", "")

    # ====== 服务 ======
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8000"))
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    ENV: str = os.getenv("ENV", "development")

    # ====== CORS ======
    CORS_ORIGINS: str = os.getenv("CORS_ORIGINS", "http://localhost:3000,http://localhost:5173")

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.CORS_ORIGINS.split(",") if o.strip()]


settings = Settings()
