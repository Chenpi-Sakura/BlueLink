"""费曼伴学偏差检测服务（Task C / V2.0 §9.1.2）

流程：
    1. 从 DB 取原文切片
    2. 构造 Prompt（概念 + 用户解释 + 原文）
    3. 调 LLM 生成偏差分析
    4. 返回结构化结果
"""

import logging

from sqlalchemy.orm import Session

from app.llm.provider import get_llm
from app.llm.prompts import FEYNMAN_SYSTEM_PROMPT
from app.models.document import Segment

logger = logging.getLogger("bluelink.feynman")


class FeynmanEvaluator:

    @staticmethod
    def evaluate(
        db: Session,
        user_explanation: str,
        target_concept: str,
        context_segment_ids: list[str],
    ) -> dict:
        """评估用户解释与原文的偏差

        Args:
            db: 数据库会话
            user_explanation: 用户用自己的话做出的解释
            target_concept: 要评估的概念名称
            context_segment_ids: 关联的原文切片 ID 列表

        Returns:
            {
                "summary": str,
                "deviations": list[{"user_segment", "deviation_type", "explanation", "original_snippet", "anchor_segment_id"}],
                "gravity_lines": list[{"from": int, "to_segment_id": str}]
            }
        """
        # 1. 从 DB 查询原文切片
        segments = (
            db.query(Segment)
            .filter(Segment.id.in_(context_segment_ids))
            .all()
        )

        # 2. 构建用户 Prompt
        context_block = "\n\n".join(
            f"--- 片段 {s.id} ---\n{s.text}"
            for s in segments
        ) if segments else "（未提供原文片段）"

        user_prompt = (
            f"【概念】\n{target_concept}\n\n"
            f"【用户解释】\n{user_explanation}\n\n"
            f"【原文片段】\n{context_block}\n\n"
            f"请按系统指令的 JSON 格式输出。"
        )

        # 3. 调用 LLM
        logger.info(
            "费曼评估: concept=%s, segments=%d",
            target_concept, len(segments),
        )
        result = get_llm().chat_json(
            system_prompt=FEYNMAN_SYSTEM_PROMPT,
            user_prompt=user_prompt,
            temperature=0.2,
        )

        return result
