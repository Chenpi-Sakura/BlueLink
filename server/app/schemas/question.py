"""溯源提问 Pydantic Schema（对应 Android Dto.kt / V2.0 §7.2.1）

Android 端用 camelCase，后端用 snake_case。
通过 alias_generator=to_camel 自动映射。
"""

from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel


class AskRequest(BaseModel):
    """溯源提问请求"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    query: str = Field(..., min_length=1)
    granularity: str = "SENTENCE"
    scope_doc_ids: list[str] | None = None
    local_segment_ids: list[str] | None = None


class AnchorDto(BaseModel):
    """锚点卡片"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    anchor_id: str = ""
    doc_title: str
    snippet: str
    segment_id: str
    score: float
    is_local: bool = False


class AskResponse(BaseModel):
    """溯源提问响应"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    introduction: str
    anchors: list[AnchorDto]
