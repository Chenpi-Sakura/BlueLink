"""离线同步 Pydantic Schema（对应 Android Dto.kt / V2.0 §7.2.1）"""

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class SyncItemDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    operation: str
    local_ref_id: str
    payload_json: str


class SyncResultDto(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    local_ref_id: str
    server_ref_id: str | None = None
    success: bool
    error: str | None = None


class SyncBatchResponse(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    synced: int
    failed: int
    results: list[SyncResultDto] = []
