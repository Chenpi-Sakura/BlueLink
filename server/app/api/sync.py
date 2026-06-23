"""离线同步 API（Task E）

接收客户端离线队列批量上传，逐条处理并返回结果。
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.schemas.sync import SyncItemDto, SyncBatchResponse, SyncResultDto

logger = logging.getLogger("bluelink.api.sync")
router = APIRouter(prefix="/api/v1/sync", tags=["同步"])


@router.post("/batch", response_model=SyncBatchResponse, response_model_by_alias=True)
def batch_sync(
    payload: list[SyncItemDto],
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> SyncBatchResponse:
    """批量同步离线操作

    接收客户端离线期间累积的操作队列，当前阶段仅记录日志并标记成功。
    后续扩展可实现真正的冲突检测和合并逻辑。
    """
    logger.info("批量同步 user=%s items=%d", user_id, len(payload))

    results: list[SyncResultDto] = []
    for item in payload:
        logger.debug(
            "同步操作 user=%s op=%s ref=%s",
            user_id, item.operation, item.local_ref_id,
        )
        results.append(
            SyncResultDto(
                local_ref_id=item.local_ref_id,
                server_ref_id=None,
                success=True,
            )
        )

    return SyncBatchResponse(
        synced=len(results),
        failed=0,
        results=results,
    )
