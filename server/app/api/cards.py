"""灵感卡片 API（Task E）

纯 CRUD，创建灵感卡片，入库后返回完整记录。
"""

import logging
import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.models.document import InspirationCard
from app.schemas.card import CreateCardRequest, InspirationDto

logger = logging.getLogger("bluelink.api.cards")
router = APIRouter(prefix="/api/v1/cards", tags=["灵感卡片"])


@router.post("", response_model=InspirationDto, response_model_by_alias=True)
def create_inspiration(
    request: CreateCardRequest,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> InspirationDto:
    """创建灵感卡片"""
    now = int(__import__("time").time() * 1000)
    card = InspirationCard(
        id=str(uuid.uuid4()),
        user_id=user_id,
        content=request.content,
        type=request.type,
        privacy_level=request.privacy_level,
        tags=",".join(request.tags),
        created_at=now,
    )
    db.add(card)
    db.commit()
    db.refresh(card)

    logger.info("创建灵感卡片 user=%s card=%s", user_id, card.id)
    return InspirationDto(
        id=card.id,
        content=card.content,
        type=card.type,
        privacy_level=card.privacy_level,
        tags=card.tags.split(",") if card.tags else [],
        created_at=card.created_at,
    )
