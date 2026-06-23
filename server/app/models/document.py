"""文档 & 切片 & 锚点 ORM 模型（V2.0 §6.3）"""

from sqlalchemy import Column, String, Integer, Float, ForeignKey, Text, Boolean, BigInteger
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
    text = Column(Text, nullable=False, default="")
    summary = Column(String, default="")
    is_folded = Column(Boolean, default=False)
    document = relationship("Document", back_populates="segments")


class InspirationCard(Base):
    """灵感卡片"""
    __tablename__ = "inspiration_cards"

    id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    content = Column(Text, nullable=False, default="")
    type = Column(String, nullable=False, default="TEXT")
    privacy_level = Column(String, nullable=False, default="LOCAL_ONLY")
    tags = Column(String, default="")
    created_at = Column(BigInteger, nullable=False)


class Anchor(Base):
    __tablename__ = "anchors"
    id = Column(String, primary_key=True)
    query_hash = Column(String, nullable=False, index=True)
    segment_id = Column(String, nullable=False)
    doc_title = Column(String, default="")
    snippet = Column(String, default="")
    score = Column(Float, default=0.0)
    created_at = Column(BigInteger, nullable=False)
