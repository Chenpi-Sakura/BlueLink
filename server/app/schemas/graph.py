"""知识图谱 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel


class GraphNodeDto(BaseModel):
    id: str
    label: str
    type: str
    ref_id: str | None = None


class GraphEdgeDto(BaseModel):
    source: str
    target: str
    relation: str
    confidence: float
    is_manual: bool = False


class GraphDto(BaseModel):
    nodes: list[GraphNodeDto]
    edges: list[GraphEdgeDto]
    next_cursor: str | None = None
