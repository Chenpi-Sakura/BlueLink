"""知识图谱 Pydantic Schema（对应 Android Dto.kt / V2.0 §7.2.1）

Android 端用 camelCase，后端用 snake_case。
通过 alias_generator=to_camel 自动映射。
"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class GraphNodeDto(BaseModel):
    """图谱节点"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    id: str
    label: str
    type: str
    ref_id: str | None = None


class GraphEdgeDto(BaseModel):
    """图谱边"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    source: str
    target: str
    relation: str
    confidence: float
    is_manual: bool = False


class GraphDto(BaseModel):
    """全量图谱数据"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    nodes: list[GraphNodeDto]
    edges: list[GraphEdgeDto]
    next_cursor: str | None = None
