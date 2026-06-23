"""溯源提问 API（V2.0 §9.1.1 / Task B）

接收用户问题，返回引导引言和相关锚点卡片。
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.schemas.question import AskRequest, AskResponse
from app.services.anchor_engine import AnchorEngine

logger = logging.getLogger("bluelink.api.questions")
router = APIRouter(prefix="/api/v1/questions", tags=["溯源提问"])


@router.post("/ask", response_model=AskResponse, response_model_by_alias=True)
def ask_question(
    request: AskRequest,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> AskResponse:
    """溯源提问

    将用户问题向量化，检索知识库中最相关的文档切片，
    调用大模型生成引导引言和锚点卡片。
    """
    logger.info("溯源提问 user=%s query=%s", user_id, request.query)

    result = AnchorEngine.generate_anchors(
        db=db,
        query=request.query,
        granularity=request.granularity,
        scope_doc_ids=request.scope_doc_ids,
        local_segment_ids=request.local_segment_ids,
    )

    try:
        return AskResponse(**result)
    except Exception as e:
        logger.warning("LLM 返回格式异常: %s", e)
        return AskResponse(
            introduction="抱歉，AI 暂时无法处理您的请求，请稍后重试。",
            anchors=[],
        )
