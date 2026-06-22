"""灵感卡片 Pydantic Schema（对应 Android Dto.kt / V2.0 §7.2.1）"""

from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel


class CreateCardRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    content: str = Field(..., min_length=1)
    type: str = "TEXT"
    privacy_level: str = "LOCAL_ONLY"
    tags: list[str] = []


class InspirationDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    id: str
    content: str
    type: str
    privacy_level: str
    tags: list[str]
    created_at: int
