"""pytest 共享配置 — PostgreSQL + TestClient

使用 DATABASE_URL 配置的数据库（默认 PostgreSQL）。
每个测试在事务中运行，测试结束后回滚，不污染数据。
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import sessionmaker

from app.main import app
from app.models.database import engine, Base, get_db

# 使用与生产相同的引擎
TestSession = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(autouse=True)
def setup_db():
    """每个测试在一个事务中执行，测试完回滚"""
    conn = engine.connect()
    trans = conn.begin()
    # 创建所有表（非 pgvector 的 Vector 类型，由 init-db.sql 在建库时处理）
    Base.metadata.create_all(bind=conn)
    yield
    trans.rollback()
    conn.close()


def _override_get_db():
    """返回连接到事务内数据库的会话"""
    db = TestSession()
    try:
        yield db
    finally:
        db.close()


@pytest.fixture
def client():
    app.dependency_overrides[get_db] = _override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
