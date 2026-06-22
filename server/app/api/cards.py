"""灵感卡片 API（Task E）

纯 CRUD，创建灵感卡片，入库后返回完整记录。
"""

import logging
import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.llm.provider import get_llm
from app.models.database import get_db
from app.models.document import InspirationCard
from app.models.graph import GraphNode
from app.schemas.card import CreateCardRequest, UpdateCardRequest, InspirationDto
from app.services.graph_builder import GraphBuilder

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

    # 如果没有提供 tags，自动从内容提取关键词
    if not request.tags and len(card.content) > 5:
        try:
            result = get_llm().chat_json(
                system_prompt="你是一个关键词提取助手。从内容提取 3-5 个关键词（词或短语），"
                              "输出 JSON 格式：{\"keywords\": [\"词1\", \"词2\"]}",
                user_prompt=card.content,
                temperature=0.1,
            )
            kw = result.get("keywords", [])
            if kw:
                card.tags = ",".join(kw[:5])
                db.commit()
                logger.info("自动提取关键词 card=%s keywords=%s", card.id, card.tags)
        except Exception as e:
            logger.warning("关键词提取跳过: %s", e)

    # 创建图谱节点 + 排期关系发现
    try:
        label = card.content[:30]
        node = GraphBuilder.ensure_node(
            db, user_id, card.id, label, "INSPIRATION",
        )
        db.commit()
        GraphBuilder.schedule_discovery(node.id, user_id)
    except Exception as e:
        logger.warning("图谱节点创建失败（不影响创建卡片）: %s", e)

    logger.info("创建灵感卡片 user=%s card=%s", user_id, card.id)
    return InspirationDto(
        id=card.id,
        content=card.content,
        type=card.type,
        privacy_level=card.privacy_level,
        tags=card.tags.split(",") if card.tags else [],
        created_at=card.created_at,
    )


@router.get("", response_model=list[InspirationDto], response_model_by_alias=True)
def list_cards(
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> list[InspirationDto]:
    """获取用户所有灵感卡片"""
    cards = (
        db.query(InspirationCard)
        .filter(InspirationCard.user_id == user_id)
        .order_by(InspirationCard.created_at.desc())
        .all()
    )
    return [
        InspirationDto(
            id=c.id, content=c.content, type=c.type,
            privacy_level=c.privacy_level,
            tags=c.tags.split(",") if c.tags else [],
            created_at=c.created_at,
        )
        for c in cards
    ]


@router.put("/{card_id}", response_model=InspirationDto, response_model_by_alias=True)
def update_card(
    card_id: str,
    request: UpdateCardRequest,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> InspirationDto:
    """更新灵感卡片内容或标签"""
    card = db.query(InspirationCard).filter(
        InspirationCard.id == card_id,
        InspirationCard.user_id == user_id,
    ).first()
    if not card:
        from fastapi import HTTPException
        raise HTTPException(404, detail="卡片不存在")

    if request.content is not None:
        card.content = request.content
    if request.type is not None:
        card.type = request.type
    if request.privacy_level is not None:
        card.privacy_level = request.privacy_level
    if request.tags is not None:
        card.tags = ",".join(request.tags)
    db.commit()
    db.refresh(card)

    return InspirationDto(
        id=card.id, content=card.content, type=card.type,
        privacy_level=card.privacy_level,
        tags=card.tags.split(",") if card.tags else [],
        created_at=card.created_at,
    )


@router.delete("/{card_id}", status_code=204)
def delete_card(
    card_id: str,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
):
    """删除灵感卡片及其图谱节点"""
    card = db.query(InspirationCard).filter(
        InspirationCard.id == card_id,
        InspirationCard.user_id == user_id,
    ).first()
    if not card:
        return

    # 删除关联的图谱节点
    db.query(GraphNode).filter(
        GraphNode.ref_id == card_id, GraphNode.user_id == user_id,
    ).delete()
    db.delete(card)
    db.commit()
    logger.info("卡片已删除 user=%s card=%s", user_id, card_id)
