"""PgVectorStore — PostgreSQL pgvector 实现（Task 0 / 生产环境）

使用 pgvector 扩展的 <-> 算子做余弦相似度检索。
依赖 `segment_vectors` 表（见 server/scripts/init-db.sql）。
"""

from __future__ import annotations

import numpy as np
from pgvector.sqlalchemy import Vector
from sqlalchemy import Column, String, ForeignKey
from sklearn.metrics.pairwise import cosine_similarity

from app.models.database import Base, SessionLocal
from app.vectors.base import VectorStore, SearchResult
from app.core.config import settings

VECTOR_DIM = settings.VECTOR_DIMENSION


class SegmentVector(Base):
    """切片向量 ORM 模型 — pgvector 存储"""
    __tablename__ = "segment_vectors"

    segment_id = Column(String, primary_key=True)
    doc_id = Column(
        String,
        ForeignKey("documents.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    embedding = Column(Vector(VECTOR_DIM))  # type: ignore


class PgVectorStore(VectorStore):
    """PostgreSQL pgvector 实现"""

    def save_segment_vectors(
        self,
        doc_id: str,
        segment_vectors: list[tuple[str, list[float]]],
    ) -> None:
        """批量写入切片向量（upsert）"""
        with SessionLocal() as session:
            for seg_id, vec in segment_vectors:
                session.merge(
                    SegmentVector(
                        segment_id=seg_id,
                        doc_id=doc_id,
                        embedding=vec,
                    )
                )
            session.commit()

    def search(
        self,
        query_vector: list[float],
        top_k: int = 20,
        doc_ids: list[str] | None = None,
    ) -> list[SearchResult]:
        """pgvector <-> 算子余弦相似度检索"""
        q_vec = np.array(query_vector, dtype=np.float32).reshape(1, -1)

        with SessionLocal() as session:
            query = session.query(SegmentVector)

            # 限定文档范围
            if doc_ids:
                query = query.filter(SegmentVector.doc_id.in_(doc_ids))

            rows = (
                query
                .order_by(SegmentVector.embedding.cosine_distance(query_vector))
                .limit(top_k)
                .all()
            )
            results = []
            for r in rows:
                vec = np.array(r.embedding, dtype=np.float32).reshape(1, -1)
                sim = float(cosine_similarity(q_vec, vec)[0][0])
                results.append(SearchResult(
                    doc_id=r.doc_id,
                    segment_id=r.segment_id,
                    score=sim,
                ))
            return results

    def delete_doc_vectors(self, doc_id: str) -> None:
        """删除文档所有向量"""
        with SessionLocal() as session:
            session.query(SegmentVector).filter(
                SegmentVector.doc_id == doc_id
            ).delete()
            session.commit()
