"""向量存储层单元测试（Task 0）

测试环境使用 numpy 回退实现（conftest 已配内存 SQLite）。
"""

import tempfile

import numpy as np
import pytest

from app.vectors.base import SearchResult
from app.vectors.numpy_store import NumpyVectorStore
from app.vectors import create_vector_store


# ===== Fixtures =====

@pytest.fixture
def store():
    """每个测试一个临时目录的 NumpyVectorStore"""
    with tempfile.TemporaryDirectory() as tmp:
        yield NumpyVectorStore(storage_dir=tmp)


# ===== 保存与检索 =====

class TestSaveAndSearch:
    def test_save_and_search_returns_results(self, store: NumpyVectorStore):
        """保存向量后能检索到"""
        vec = [0.1] * 1024
        store.save_segment_vectors("doc-1", [("seg-1", vec)])

        results = store.search([0.1] * 1024, top_k=10)
        assert len(results) == 1
        assert results[0].doc_id == "doc-1"
        assert results[0].segment_id == "seg-1"
        assert results[0].score > 0.99  # 完全相同向量

    def test_search_returns_top_k(self, store: NumpyVectorStore):
        """检索只返回 top-k 个结果"""
        for i in range(5):
            store.save_segment_vectors(f"doc-{i}", [(f"seg-{i}", [float(i)] * 1024)])

        results = store.search([0.0] * 1024, top_k=3)
        assert len(results) == 3

    def test_search_empty_store(self, store: NumpyVectorStore):
        """空库搜索返回空列表"""
        results = store.search([0.0] * 1024, top_k=10)
        assert results == []

    def test_similarity_order(self, store: NumpyVectorStore):
        """结果按相似度降序排列"""
        store.save_segment_vectors("doc-close", [("seg-close", [0.9] * 1024)])
        store.save_segment_vectors("doc-far", [("seg-far", [0.1] * 1024)])

        results = store.search([0.9] * 1024, top_k=10)
        assert len(results) == 2
        assert results[0].doc_id == "doc-close"
        assert results[0].score > results[1].score

    def test_save_empty_list(self, store: NumpyVectorStore):
        """保存空列表不创建文件"""
        store.save_segment_vectors("doc-empty", [])
        results = store.search([0.0] * 1024, top_k=10)
        assert results == []

    def test_multiple_segments_per_doc(self, store: NumpyVectorStore):
        """同一文档多个切片都能检索到"""
        store.save_segment_vectors("doc-1", [
            ("seg-1", [0.9] * 1024),
            ("seg-2", [0.1] * 1024),
        ])
        results = store.search([0.9] * 1024, top_k=10)
        assert len(results) == 2
        # seg-1 应该排前面
        assert results[0].segment_id == "seg-1"


# ===== 删除 =====

class TestDelete:
    def test_delete_removes_vectors(self, store: NumpyVectorStore):
        """删除后向量消失"""
        store.save_segment_vectors("doc-1", [("seg-1", [0.5] * 1024)])
        store.delete_doc_vectors("doc-1")
        results = store.search([0.5] * 1024, top_k=10)
        assert results == []

    def test_delete_nonexistent_doc(self, store: NumpyVectorStore):
        """删除不存在的文档不报错"""
        store.delete_doc_vectors("doc-not-exist")  # 不应抛异常
        assert True

    def test_delete_preserves_other_docs(self, store: NumpyVectorStore):
        """删除文档不影响其他文档"""
        store.save_segment_vectors("doc-keep", [("seg-keep", [0.5] * 1024)])
        store.save_segment_vectors("doc-del", [("seg-del", [0.1] * 1024)])
        store.delete_doc_vectors("doc-del")

        results = store.search([0.5] * 1024, top_k=10)
        assert len(results) == 1
        assert results[0].doc_id == "doc-keep"


# ===== 工厂 =====

class TestFactory:
    def test_create_with_sqlite_url_returns_numpy(self, monkeypatch):
        """sqlite DATABASE_URL 返回 NumpyVectorStore"""
        monkeypatch.setattr("app.core.config.settings.DATABASE_URL", "sqlite:///./test.db")
        store = create_vector_store()
        assert isinstance(store, NumpyVectorStore)

    def test_result_dataclass(self):
        """SearchResult 数据类字段正确"""
        r = SearchResult(doc_id="d", segment_id="s", score=0.95)
        assert r.doc_id == "d"
        assert r.segment_id == "s"
        assert r.score == 0.95
