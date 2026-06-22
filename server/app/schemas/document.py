"""文档相关 Pydantic Schema（对应 Android Dto.kt / V2.0 §7.2.1）"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class DocumentDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    id: str
    title: str
    privacy_level: str = "LOCAL_FIRST"
    source: str | None = None
    created_at: int = 0
    concept_beacon: str | None = None


class DocumentListDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    items: list[DocumentDto]
    next_cursor: str | None = None


class SegmentDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    id: str
    index: int = 0
    summary: str = ""
    folded: bool = False


class SegmentListDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    segments: list[SegmentDto]


class FoldedRangeDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    segment_index_start: int
    segment_index_end: int
    reason: str = ""


class DeltaResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    folded_ranges: list[FoldedRangeDto] = []
    new_content_ratio: float = 1.0


class UpdateDocumentRequest(BaseModel):
    """更新文档请求"""
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    title: str | None = None
    privacy_level: str | None = None
