"""溯源提问服务 — Anchor Engine（Task B / V2.0 §9.1.1）

流程：
    1. 将用户问题向量化
    2. 通过 VectorStore 检索最相关切片
    3. 如果有本地私密切片 ID，按 ID 直接查 DB
    4. 构造 Prompt → 调 LLM → 返回引言 + 锚点
"""

import logging

from sqlalchemy.orm import Session

from app.llm.provider import get_llm
from app.llm.prompts import ANCHOR_SYSTEM_PROMPT
from app.models.document import Segment
from app.vectors.base import create_vector_store

logger = logging.getLogger("bluelink.anchor")


class AnchorEngine:

    @staticmethod
    def generate_anchors(
        db: Session,
        query: str,
        granularity: str = "SENTENCE",
        scope_doc_ids: list[str] | None = None,
        local_segment_ids: list[str] | None = None,
    ) -> dict:
        """溯源提问 — 根据用户问题生成引言和锚点

        Args:
            db: 数据库会话
            query: 用户问题
            granularity: 检索粒度 SENTENCE / PARAGRAPH
            scope_doc_ids: 限定检索的文档 ID 列表（可选）
            local_segment_ids: 客户端私密文档命中的切片 ID 列表（可选）

        Returns:
            LLM 返回的原始 dict，包含 introduction 和 anchors
        """
        # 1. 查询向量化
        logger.info("溯源提问: query=%s, granularity=%s", query, granularity)
        query_vector = get_llm().embed_text(query)

        # 2. 向量检索公开文档切片
        store = create_vector_store()
        search_results = store.search(query_vector, top_k=20)
        logger.info("向量检索命中 %d 个切片", len(search_results))

        # 收集所有需要查询的 segment_id
        seg_ids = [r.segment_id for r in search_results]

        # 3. 如果有本地私密切片 ID，合并进来（仅 ID，不含原文）
        if local_segment_ids:
            seg_ids.extend(local_segment_ids)

        # 4. 从 DB 查询切片原文
        segments = []
        if seg_ids:
            segments = (
                db.query(Segment)
                .filter(Segment.id.in_(seg_ids))
                .all()
            )

        # 5. 按 doc_id 分组获取文档标题
        doc_ids = list({s.doc_id for s in segments})
        from app.models.document import Document
        docs = {d.id: d.title for d in db.query(Document).filter(Document.id.in_(doc_ids)).all()} if doc_ids else {}

        # 6. 构造 Prompt
        seg_block = "\n\n".join(
            f"--- 文档: {docs.get(s.doc_id, '未知')} | 片段 ID: {s.id} ---\n{s.text}"
            for s in segments
        ) if segments else "（知识库中暂未找到相关文档片段）"

        user_prompt = (
            f"【用户问题】\n{query}\n\n"
            f"【检索粒度】\n{granularity}\n\n"
            f"【文档片段】（共 {len(segments)} 条）\n{seg_block}\n\n"
            f"请严格按系统指令的 JSON 格式输出。"
        )

        # 7. 调 LLM
        result = get_llm().chat_json(
            system_prompt=ANCHOR_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            temperature=0.3,
        )

        return result
