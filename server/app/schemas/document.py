"""文档相关 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel


class DocumentDto(BaseModel):
    id: str
    title: str
    privacy_level: str = "LOCAL_FIRST"
    source: str | None = None
    created_at: int = 0
    concept_beacon: str | None = None


class DocumentListDto(BaseModel):
    items: list[DocumentDto]
    next_cursor: str | None = None


class SegmentDto(BaseModel):
    id: str
    index: int = 0
    summary: str = ""
    folded: bool = False


class SegmentListDto(BaseModel):
    segments: list[SegmentDto]


class FoldedRangeDto(BaseModel):
    segment_index_start: int
    segment_index_end: int
    reason: str = ""


class DeltaResponse(BaseModel):
    folded_ranges: list[FoldedRangeDto] = []
    new_content_ratio: float = 1.0
