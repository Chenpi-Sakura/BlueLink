"""用户 API（Task E）

当前展示用户 ID，后续可扩展用户设置等功能。
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.schemas.user import UserInfoResponse

logger = logging.getLogger("bluelink.api.users")
router = APIRouter(prefix="/api/v1/users", tags=["用户"])


@router.get("/me", response_model=UserInfoResponse, response_model_by_alias=True)
def get_user_me(
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> UserInfoResponse:
    """获取当前用户信息"""
    return UserInfoResponse(user_id=user_id)


@router.delete("/me", status_code=204)
def delete_user_me(
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
):
    """删除当前用户所有数据"""
    from app.models.document import Document, Segment, Anchor, InspirationCard
    from app.models.graph import GraphNode, GraphEdge

    logger.warning("删除用户数据 user=%s", user_id)

    # 按依赖顺序删除
    db.query(GraphEdge).filter(
        GraphEdge.source_id.in_(
            db.query(GraphNode.id).filter(GraphNode.user_id == user_id)
        )
    ).delete(synchronize_session=False)
    db.query(GraphNode).filter(GraphNode.user_id == user_id).delete()
    db.query(Segment).filter(
        Segment.doc_id.in_(
            db.query(Document.id).filter(Document.user_id == user_id)
        )
    ).delete(synchronize_session=False)
    db.query(Anchor).filter(Anchor.query_hash.isnot(None)).delete()
    db.query(Document).filter(Document.user_id == user_id).delete()
    db.query(InspirationCard).filter(InspirationCard.user_id == user_id).delete()
    db.commit()
