"""SQLAlchemy 引擎与会话 — 同步 ORM（V2.0 §6.3）"""

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker
from app.core.config import settings

engine = create_engine(
    settings.DATABASE_URL,
    connect_args={"check_same_thread": False}
    if settings.DATABASE_URL.startswith("sqlite")
    else {},
    echo=(settings.ENV == "development"),
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
