"""向量存储抽象接口 — 不绑定任何后端（Task 0）

生产环境 → PgVectorStore（PostgreSQL + pgvector）
本地开发 → NumpyVectorStore（numpy npy 文件回退）

用法：
    store = create_vector_store()
    store.save_segment_vectors("doc-1", [("seg-1", [0.1, 0.2, ...])])
    results = store.search(query_vector=[0.1, 0.2, ...], top_k=10)
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    import numpy as np


@dataclass
class SearchResult:
    """向量检索结果"""
    doc_id: str
    segment_id: str
    score: float          # 余弦相似度，0~1，越大越相关


class VectorStore(ABC):
    """向量存储抽象接口"""

    @abstractmethod
    def save_segment_vectors(
        self,
        doc_id: str,
        segment_vectors: list[tuple[str, list[float]]],
    ) -> None:
        """保存文档所有切片的向量

        Args:
            doc_id: 文档 ID
            segment_vectors: [(segment_id, vector_1024d), ...]
        """
        ...

    @abstractmethod
    def search(
        self,
        query_vector: list[float] | np.ndarray,
        top_k: int = 20,
    ) -> list[SearchResult]:
        """余弦相似度检索 top-k

        Args:
            query_vector: 查询向量
            top_k: 返回 top-k 结果

        Returns:
            按相似度降序排列的检索结果
        """
        ...

    @abstractmethod
    def delete_doc_vectors(self, doc_id: str) -> None:
        """删除某文档所有向量"""
        ...


def create_vector_store() -> VectorStore:
    """根据 DATABASE_URL 自动选择向量存储实现

    postgresql:// → PgVectorStore
    其他（含 sqlite://）= NumpyVectorStore（开发/测试用）
    """
    from app.core.config import settings
    from app.vectors.numpy_store import NumpyVectorStore

    if settings.DATABASE_URL.startswith("postgresql"):
        from app.vectors.pgvector_store import PgVectorStore
        return PgVectorStore()

    return NumpyVectorStore(storage_dir=settings.VECTORS_DIR)
