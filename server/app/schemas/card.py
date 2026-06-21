"""灵感卡片 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel, Field


class CreateCardRequest(BaseModel):
    content: str = Field(..., description="脱敏后的文本内容")
    type: str = "TEXT"
    privacy_level: str = "LOCAL_ONLY"
    tags: list[str] = []


class InspirationDto(BaseModel):
    id: str
    content: str
    type: str
    privacy_level: str
    tags: list[str]
    created_at: int
