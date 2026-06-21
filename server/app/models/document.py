"""文档 & 切片 & 锚点 ORM 模型（V2.0 §6.3）"""

from sqlalchemy import Column, String, Integer, Float, ForeignKey, LargeBinary, Boolean, BigInteger
from sqlalchemy.orm import relationship
from app.models.database import Base


class Document(Base):
    __tablename__ = "documents"
    id = Column(String, primary_key=True, comment="UUID")
    user_id = Column(String, nullable=False, index=True)
    title = Column(String, nullable=False)
    privacy_level = Column(String, nullable=False, default="LOCAL_FIRST")
    source = Column(String, nullable=True)
    created_at = Column(BigInteger, nullable=False)
    concept_beacon = Column(String, nullable=True)
    segments = relationship("Segment", back_populates="document", cascade="all, delete-orphan")


class Segment(Base):
    __tablename__ = "segments"
    id = Column(String, primary_key=True)
    doc_id = Column(String, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False, index=True)
    index_num = Column(Integer, nullable=False)
    text_encrypted = Column(LargeBinary, nullable=True)
    summary = Column(String, default="")
    is_folded = Column(Boolean, default=False)
    document = relationship("Document", back_populates="segments")


class Anchor(Base):
    __tablename__ = "anchors"
    id = Column(String, primary_key=True)
    query_hash = Column(String, nullable=False, index=True)
    segment_id = Column(String, nullable=False)
    doc_title = Column(String, default="")
    snippet = Column(String, default="")
    score = Column(Float, default=0.0)
    created_at = Column(BigInteger, nullable=False)
