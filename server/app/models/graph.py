"""知识图谱 ORM 模型（V2.0 §6.3）"""

from sqlalchemy import Column, String, Float, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from app.models.database import Base


class GraphNode(Base):
    __tablename__ = "graph_nodes"
    id = Column(String, primary_key=True)
    user_id = Column(String, nullable=False, index=True)
    label = Column(String, nullable=False)
    type = Column(String, nullable=False)
    ref_id = Column(String, nullable=True)
    outgoing_edges = relationship("GraphEdge", foreign_keys="GraphEdge.source_id", back_populates="source_node", cascade="all, delete-orphan")
    incoming_edges = relationship("GraphEdge", foreign_keys="GraphEdge.target_id", back_populates="target_node", cascade="all, delete-orphan")


class GraphEdge(Base):
    __tablename__ = "graph_edges"
    id = Column(String, primary_key=True)
    source_id = Column(String, ForeignKey("graph_nodes.id", ondelete="CASCADE"), nullable=False)
    target_id = Column(String, ForeignKey("graph_nodes.id", ondelete="CASCADE"), nullable=False)
    relation = Column(String, nullable=False)
    confidence = Column(Float, default=0.0)
    is_manual = Column(Boolean, default=False)
    source_node = relationship("GraphNode", foreign_keys=[source_id], back_populates="outgoing_edges")
    target_node = relationship("GraphNode", foreign_keys=[target_id], back_populates="incoming_edges")
