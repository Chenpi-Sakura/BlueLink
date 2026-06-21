"""费曼伴学 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel, Field


class FeynmanRequest(BaseModel):
    user_explanation: str = Field(..., min_length=1)
    target_concept: str = Field(..., min_length=1)
    context_segment_ids: list[str]


class DeviationDto(BaseModel):
    user_segment: str
    deviation_type: str
    explanation: str
    original_snippet: str
    anchor_segment_id: str


class GravityLineDto(BaseModel):
    from_idx: int = Field(..., alias="from")
    to_segment_id: str


class FeynmanResponse(BaseModel):
    summary: str
    deviations: list[DeviationDto]
    gravity_lines: list[GravityLineDto] = []
