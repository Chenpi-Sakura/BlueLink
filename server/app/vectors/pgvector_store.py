"""PgVectorStore — PostgreSQL pgvector 实现（Task 0 / 生产环境）

使用 pgvector 扩展的 <-> 算子做余弦相似度检索。
依赖 `segment_vectors` 表（见 server/scripts/init-db.sql）。
"""

from __future__ import annotations

from sqlalchemy import Column, String, ForeignKey, create_engine
from sqlalchemy.orm import Session
from pgvector.sqlalchemy import Vector

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
    ) -> list[SearchResult]:
        """pgvector <-> 算子余弦相似度检索"""
        with SessionLocal() as session:
            rows = (
                session.query(SegmentVector)
                .order_by(SegmentVector.embedding.cosine_distance(query_vector))
                .limit(top_k)
                .all()
            )
            return [
                SearchResult(
                    doc_id=r.doc_id,
                    segment_id=r.segment_id,
                    score=1.0 - r.embedding.cosine_distance(query_vector),
                )
                for r in rows
            ]

    def delete_doc_vectors(self, doc_id: str) -> None:
        """删除文档所有向量"""
        with SessionLocal() as session:
            session.query(SegmentVector).filter(
                SegmentVector.doc_id == doc_id
            ).delete()
            session.commit()
