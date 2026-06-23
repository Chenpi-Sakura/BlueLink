"""费曼伴学 Pydantic Schema（对应 Android Dto.kt）

Android 端用 camelCase，后端用 snake_case。
通过 alias_generator=to_camel 自动映射，对双端透明。
"""

from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel


class FeynmanRequest(BaseModel):
    """费曼评估请求 — Android 传入 camelCase，自动映射"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    user_explanation: str = Field(..., min_length=1)
    target_concept: str = Field(..., min_length=1)
    context_segment_ids: list[str]


class DeviationDto(BaseModel):
    """偏差条目"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    user_segment: str
    deviation_type: str
    explanation: str
    original_snippet: str
    anchor_segment_id: str


class GravityLineDto(BaseModel):
    """引力线 — 用户表述位置 → 原文切片位置"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    from_idx: int = Field(..., alias="from")
    to_segment_id: str


class FeynmanResponse(BaseModel):
    """费曼评估响应 — 序列化时按 alias 输出 camelCase"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    summary: str
    deviations: list[DeviationDto]
    gravity_lines: list[GravityLineDto] = []
