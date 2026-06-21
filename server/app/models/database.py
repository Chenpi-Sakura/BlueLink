"""SQLAlchemy 引擎与会话 — 支持 PostgreSQL + SQLite 回退（V2.0 §6.3 / V2.1 修订）

根据 DATABASE_URL 自动选择：
  - postgresql://... → PostgreSQL + 连接池
  - sqlite:///...   → SQLite（开发/测试用）

向量存储由 pgvector 扩展 / numpy 回退两种模式，见 vectors/store.py。
"""

from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from app.core.config import settings


def _create_engine():
    url = settings.DATABASE_URL
    if url.startswith("postgresql"):
        return create_engine(
            url,
            pool_size=settings.DB_POOL_SIZE,
            max_overflow=settings.DB_MAX_OVERFLOW,
            pool_pre_ping=True,          # 自动检测断连
            echo=(settings.ENV == "development"),
        )
    else:
        # SQLite 回退（本地开发 / 测试）
        return create_engine(
            url,
            connect_args={"check_same_thread": False},
            echo=(settings.ENV == "development"),
        )


engine = _create_engine()
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    """FastAPI 依赖注入 — 每次请求一个会话"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """创建所有表（含 pgvector 扩展）"""
    if str(engine.url).startswith("postgresql"):
        # 启用 pgvector 扩展
        with engine.connect() as conn:
            conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
            conn.commit()
    Base.metadata.create_all(bind=engine)
