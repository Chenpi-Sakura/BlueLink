"""知识图谱构建服务（Task D / V2.0 §9.1.4）

当前功能：
  - 从 DB 查询全量节点和边

后续扩展：
  - `build_graph()`: 收集所有节点，调 LLM 发现关系，持久化新边
"""

import logging

from sqlalchemy.orm import Session

from app.models.graph import GraphNode, GraphEdge

logger = logging.getLogger("bluelink.graph")


class GraphBuilder:

    @staticmethod
    def get_graph(
        db: Session,
        user_id: str,
        cursor: str | None = None,
        limit: int = 500,
    ) -> dict:
        """获取用户的全量图谱数据

        Args:
            db: 数据库会话
            user_id: 用户 ID
            cursor: 分页游标（暂未实现，保留参数）
            limit: 最大返回数量

        Returns:
            {"nodes": [...], "edges": [...], "next_cursor": None}
        """
        nodes = (
            db.query(GraphNode)
            .filter(GraphNode.user_id == user_id)
            .all()
        )
        edges = (
            db.query(GraphEdge)
            .join(GraphNode, GraphEdge.source_id == GraphNode.id)
            .filter(GraphNode.user_id == user_id)
            .all()
        )

        logger.info(
            "获取图谱 user=%s nodes=%d edges=%d",
            user_id, len(nodes), len(edges),
        )

        return {
            "nodes": [
                {
                    "id": n.id,
                    "label": n.label,
                    "type": n.type,
                    "ref_id": n.ref_id,
                }
                for n in nodes
            ],
            "edges": [
                {
                    "source": e.source_id,
                    "target": e.target_id,
                    "relation": e.relation,
                    "confidence": e.confidence,
                    "is_manual": e.is_manual,
                }
                for e in edges
            ],
            "next_cursor": None,
        }
