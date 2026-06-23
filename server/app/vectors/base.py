"""向量存储抽象接口 — PostgreSQL + pgvector（Task 0）

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
        doc_ids: list[str] | None = None,
    ) -> list[SearchResult]:
        """余弦相似度检索 top-k

        Args:
            query_vector: 查询向量
            top_k: 返回 top-k 结果
            doc_ids: 限定检索的文档 ID 列表（可选）

        Returns:
            按相似度降序排列的检索结果
        """
        ...

    @abstractmethod
    def delete_doc_vectors(self, doc_id: str) -> None:
        """删除某文档所有向量"""
        ...


def create_vector_store() -> VectorStore:
    """创建 PgVectorStore（PostgreSQL + pgvector）"""
    from app.vectors.pgvector_store import PgVectorStore
    return PgVectorStore()
