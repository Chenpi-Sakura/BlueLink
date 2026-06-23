"""知识图谱构建服务（V2.0 §9.1.4）

功能：
  - get_graph: 查询全量节点和边
  - ensure_node: 创建或更新图谱节点
  - schedule_discovery: 60 秒防抖排期 LLM 关系发现
  - discover_relations: 增量关系发现（新节点 VS 已有节点）
"""

import logging
import uuid
from threading import Lock, Timer

from sqlalchemy.orm import Session

from app.llm.provider import get_llm
from app.llm.prompts import GRAPH_SYSTEM_PROMPT
from app.models.document import Document, Segment, InspirationCard
from app.models.graph import GraphNode, GraphEdge

logger = logging.getLogger("bluelink.graph")

# 防抖计时器: {node_id: Timer}
_discovery_timers: dict[str, Timer] = {}
_discovery_lock = Lock()

# 每批最多处理的已有节点数
BATCH_SIZE = 30


class GraphBuilder:

    @staticmethod
    def get_graph(
        db: Session,
        user_id: str,
        cursor: str | None = None,
        limit: int = 500,
    ) -> dict:
        """获取用户的全量图谱数据"""
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

        return {
            "nodes": [
                {"id": n.id, "label": n.label, "type": n.type, "ref_id": n.ref_id}
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

    @staticmethod
    def ensure_node(
        db: Session,
        user_id: str,
        ref_id: str,
        label: str,
        node_type: str,
    ) -> GraphNode:
        """如果节点不存在则创建，存在则更新 label

        Args:
            ref_id: 指向源文档或卡片的 UUID
            label: 节点显示名称
            node_type: DOCUMENT / INSPIRATION / CONCEPT

        Returns:
            已有的或新创建的 GraphNode
        """
        node = (
            db.query(GraphNode)
            .filter(
                GraphNode.user_id == user_id,
                GraphNode.ref_id == ref_id,
            )
            .first()
        )
        if node:
            # 更新 label
            node.label = label
        else:
            node = GraphNode(
                id=str(uuid.uuid4()),
                user_id=user_id,
                label=label,
                type=node_type,
                ref_id=ref_id,
            )
            db.add(node)
            db.flush()
            logger.info("创建图谱节点 user=%s node=%s type=%s", user_id, node.id, node_type)

        return node

    @staticmethod
    def schedule_discovery(node_id: str, user_id: str):
        """60 秒防抖排期关系发现"""
        with _discovery_lock:
            old = _discovery_timers.get(node_id)
            if old:
                old.cancel()
                logger.debug("取消 node=%s 的旧发现任务", node_id)

            t = Timer(60.0, GraphBuilder._run_discovery, args=[node_id, user_id])
            t.daemon = True
            _discovery_timers[node_id] = t
            t.start()
        logger.info("排期关系发现 node=%s （60 秒后执行）", node_id)

    @staticmethod
    def _run_discovery(node_id: str, user_id: str):
        """定时器回调 — 执行增量关系发现"""
        with _discovery_lock:
            _discovery_timers.pop(node_id, None)
        logger.info("开始关系发现 node=%s", node_id)

        from app.models.database import SessionLocal
        db = SessionLocal()
        try:
            GraphBuilder.discover_relations(db=db, node_id=node_id, user_id=user_id)
            db.commit()
            logger.info("关系发现完成 node=%s", node_id)
        except Exception as e:
            db.rollback()
            logger.error("关系发现失败 node=%s: %s", node_id, e)
        finally:
            db.close()

    @staticmethod
    def discover_relations(db: Session, node_id: str, user_id: str):
        """增量 LLM 关系发现

        为新节点与所有已有节点发现关系。
        如果节点已被编辑过，先删除其所有旧边再做全量发现。

        Args:
            db: 数据库会话（调用方负责 commit/rollback）
            node_id: 新节点的 ID
            user_id: 用户 ID
        """
        # 1. 获取新节点
        new_node = db.query(GraphNode).filter(GraphNode.id == node_id).first()
        if not new_node:
            logger.warning("节点 %s 不存在，跳过", node_id)
            return

        # 2. 获取所有其他节点
        other_nodes = (
            db.query(GraphNode)
            .filter(
                GraphNode.user_id == user_id,
                GraphNode.id != node_id,
            )
            .all()
        )

        if not other_nodes:
            logger.info("没有其他节点，跳过关系发现")
            return

        # 3. 获取新节点的内容摘要
        new_context = GraphBuilder._get_node_context(db, new_node)
        if not new_context:
            logger.warning("节点 %s 无内容，跳过", node_id)
            return

        # 4. 分批处理已有节点
        for i in range(0, len(other_nodes), BATCH_SIZE):
            batch = other_nodes[i:i + BATCH_SIZE]

            # 获取每个已有节点的内容
            other_contexts = []
            for n in batch:
                ctx = GraphBuilder._get_node_context(db, n)
                if ctx:
                    other_contexts.append(ctx)

            if not other_contexts:
                continue

            # 5. 构建 Prompt
            batch_prompt = (
                f"【新节点】\n"
                f"ID: {new_node.id} | 标题: {new_context['label']} | 类型: {new_context['type']}\n"
                f"内容摘要: {new_context['summary']}\n\n"
                f"【已有节点列表】\n"
            )
            for ctx in other_contexts:
                batch_prompt += (
                    f"- ID: {ctx['id']} | {ctx['label']} ({ctx['type']})\n"
                    f"  内容: {ctx['summary'][:200]}\n"
                )
            batch_prompt += (
                f"\n请判断新节点(source_id={new_node.id})与哪些已有节点存在关系。"
                f"只输出确实存在的关系，不要臆测。"
                f"输出 JSON 格式：\n"
                f'{{"edges": [{{"source_id": "{new_node.id}", "target_id": "已有节点ID", '
                f'"relation": "SUPPORT|CHALLENGE|SUPPLEMENT|CITE", '
                f'"confidence": 0.0-1.0, "reason": "简短理由"}}]}}'
            )

            # 6. 调 LLM
            try:
                result = get_llm().chat_json(
                    system_prompt=GRAPH_SYSTEM_PROMPT,
                    user_prompt=batch_prompt,
                    temperature=0.2,
                )
            except Exception as e:
                logger.warning("LLM 调用失败（跳过该批）: %s", e)
                continue

            # 7. 去重 & 插入新边（逐条验证外键，单条失败不影响其他）
            for edge_data in result.get("edges", []):
                try:
                    source_id = edge_data.get("source_id", node_id)
                    target_id = edge_data.get("target_id")
                    relation = edge_data.get("relation")
                    confidence = edge_data.get("confidence", 0.5)

                    if not source_id or not target_id or not relation:
                        continue

                    # 验证两个节点都存在
                    src = db.query(GraphNode).filter(GraphNode.id == source_id).first()
                    tgt = db.query(GraphNode).filter(GraphNode.id == target_id).first()
                    if not src or not tgt:
                        logger.warning("跳过不存在的节点: %s → %s", source_id, target_id)
                        continue

                    # 检查是否已存在相同边
                    existing = (
                        db.query(GraphEdge)
                        .filter(
                            GraphEdge.source_id == source_id,
                            GraphEdge.target_id == target_id,
                            GraphEdge.relation == relation,
                        )
                        .first()
                    )
                    if existing:
                        continue

                    db.add(GraphEdge(
                        id=str(uuid.uuid4()),
                        source_id=source_id,
                        target_id=target_id,
                        relation=relation,
                        confidence=confidence,
                        is_manual=False,
                    ))
                    logger.info("发现关系: %s →[%s]→ %s", source_id, relation, target_id)
                except Exception as e:
                    logger.warning("单条边插入失败（跳过）: %s", e)

    @staticmethod
    def _get_node_context(db: Session, node: GraphNode) -> dict | None:
        """获取节点的内容摘要（用于 LLM 上下文）

        Returns:
            {"id": str, "label": str, "type": str, "summary": str}
            或 None（无法获取内容）
        """
        if node.type == "DOCUMENT" and node.ref_id:
            doc = db.query(Document).filter(Document.id == node.ref_id).first()
            if doc:
                # 取前 3 个切片的摘要
                segs = (
                    db.query(Segment)
                    .filter(Segment.doc_id == doc.id)
                    .order_by(Segment.index_num)
                    .limit(3)
                    .all()
                )
                seg_summaries = " | ".join(s.summary[:100] for s in segs) if segs else doc.title
                return {
                    "id": node.id,
                    "label": node.label,
                    "type": "DOCUMENT",
                    "summary": f"文档《{doc.title}》\n{seg_summaries}",
                }

        elif node.type == "INSPIRATION" and node.ref_id:
            card = db.query(InspirationCard).filter(InspirationCard.id == node.ref_id).first()
            if card:
                return {
                    "id": node.id,
                    "label": node.label,
                    "type": "INSPIRATION",
                    "summary": card.content[:500],
                }

        # CONCEPT 类型或其他没有源表的节点
        return {
            "id": node.id,
            "label": node.label,
            "type": node.type,
            "summary": node.label,
        }
