"""溯源提问 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    query: str = Field(..., min_length=1)
    granularity: str = "SENTENCE"
    scope_doc_ids: list[str] | None = None
    local_segment_ids: list[str] | None = None


class AnchorDto(BaseModel):
    anchor_id: str
    doc_title: str
    snippet: str
    segment_id: str
    score: float
    is_local: bool = False


class AskResponse(BaseModel):
    introduction: str
    anchors: list[AnchorDto]
