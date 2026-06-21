"""pytest 共享配置 — 内存 SQLite + TestClient"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.main import app
from app.models.database import Base, get_db

_test_engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False})
_test_session = sessionmaker(autocommit=False, autoflush=False, bind=_test_engine)


def _override_get_db():
    db = _test_session()
    try:
        yield db
    finally:
        db.close()


@pytest.fixture(autouse=True)
def setup_db():
    Base.metadata.create_all(bind=_test_engine)
    yield
    Base.metadata.drop_all(bind=_test_engine)


@pytest.fixture
def client():
    app.dependency_overrides[get_db] = _override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
