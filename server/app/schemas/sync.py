"""离线同步 Pydantic Schema（对应 Android Dto.kt）"""

from pydantic import BaseModel


class SyncItemDto(BaseModel):
    operation: str
    local_ref_id: str
    payload_json: str


class SyncResultDto(BaseModel):
    local_ref_id: str
    server_ref_id: str | None = None
    success: bool
    error: str | None = None


class SyncBatchResponse(BaseModel):
    synced: int
    failed: int
    results: list[SyncResultDto] = []
