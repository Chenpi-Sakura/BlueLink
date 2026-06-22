"""知识图谱 API（V2.0 §9.1.4 / Task D）

返回全量图谱节点和边，供 Android 端 ECharts 渲染。
"""

import logging

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.models.database import get_db
from app.schemas.graph import GraphDto
from app.services.graph_builder import GraphBuilder

logger = logging.getLogger("bluelink.api.graph")
router = APIRouter(prefix="/api/v1/graph", tags=["知识图谱"])


@router.get("", response_model=GraphDto, response_model_by_alias=True)
def fetch_graph(
    cursor: str | None = Query(None),
    limit: int = Query(500, ge=1, le=1000),
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> GraphDto:
    """获取全量图谱数据

    返回用户的所有图谱节点和边，供 ECharts 渲染。
    cursor 分页暂未实现，保留参数接口。
    """
    logger.info("获取图谱 user=%s limit=%d", user_id, limit)

    result = GraphBuilder.get_graph(
        db=db,
        user_id=user_id,
        cursor=cursor,
        limit=limit,
    )

    return GraphDto(**result)
