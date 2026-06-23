"""SQLAlchemy 引擎与会话 — PostgreSQL + pgvector（V2.0 §6.3 / V2.1 修订）

依赖环境变量 DATABASE_URL（默认 postgresql://bluelink:bluelink@localhost:5432/bluelink）。
向量检索通过 pgvector 扩展实现，见 vectors/store.py。
"""

from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from app.core.config import settings


def _create_engine():
    return create_engine(
        settings.DATABASE_URL,
        pool_size=settings.DB_POOL_SIZE,
        max_overflow=settings.DB_MAX_OVERFLOW,
        pool_pre_ping=True,
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
    """创建所有表 + 启用 pgvector 扩展 + 向量索引"""
    with engine.connect() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        conn.execute(text(
            "CREATE INDEX IF NOT EXISTS idx_segment_vectors_embedding "
            "ON segment_vectors USING ivfflat (embedding vector_cosine_ops) "
            "WITH (lists = 100)"
        ))
        conn.commit()
    Base.metadata.create_all(bind=engine)
