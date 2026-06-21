"""向量存储 — 抽象接口 + pgvector 实现 + numpy 回退

使用方式：
    from app.vectors import create_vector_store, VectorStore, SearchResult

    store = create_vector_store()
    store.save_segment_vectors("doc-1", [("seg-1", [0.1, ...])])
    hits = store.search([0.1, ...], top_k=10)
"""

from app.vectors.base import VectorStore, SearchResult, create_vector_store

__all__ = ["VectorStore", "SearchResult", "create_vector_store"]
