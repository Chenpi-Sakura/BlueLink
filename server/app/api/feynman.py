"""费曼伴学 API（V2.0 §9.1.2 / Task C）

客户端发送用户对自己概念的复述，后端通过 LLM 比对原文，
返回偏差（遗漏/矛盾/过度延伸）与引力线。
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.schemas.feynman import FeynmanRequest, FeynmanResponse
from app.services.feynman_evaluator import FeynmanEvaluator

logger = logging.getLogger("bluelink.api.feynman")
router = APIRouter(prefix="/api/v1/feynman", tags=["费曼伴学"])


@router.post("/evaluate", response_model=FeynmanResponse, response_model_by_alias=True)
def evaluate_feynman(
    request: FeynmanRequest,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> FeynmanResponse:
    """费曼偏差评估

    接收用户的解释文本和目标概念，与原文切片对比，返回偏差列表。
    """
    logger.info("费曼评估 user=%s concept=%s", user_id, request.target_concept)

    result = FeynmanEvaluator.evaluate(
        db=db,
        user_explanation=request.user_explanation,
        target_concept=request.target_concept,
        context_segment_ids=request.context_segment_ids,
    )

    try:
        return FeynmanResponse(**result)
    except Exception as e:
        logger.warning("LLM 返回格式异常: %s", e)
        return FeynmanResponse(
            summary="AI 暂时无法完成评估，请稍后重试。",
            deviations=[],
        )
