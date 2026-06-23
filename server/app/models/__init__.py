"""ORM 模型 — 导入所有模型以确保 Base.metadata 注册"""
from app.models.document import Document, Segment, Anchor, InspirationCard
from app.models.graph import GraphNode, GraphEdge

__all__ = [
    "Document", "Segment", "Anchor", "InspirationCard",
    "GraphNode", "GraphEdge",
]

# pgvector 模型（SegmentVector）由 init-db.sql 建表，
# 不在 ORM 层自动注册（Vector 类型不兼容 SQLite 测试环境）。
# 使用 PgVectorStore 时直接引用 pgvector_store.SegmentVector。
