"""文档去重服务（Task A / V2.0 §9.2）

将新文档的切片与历史切片做向量相似度对比，
找出重复区间并计算信息增量比例。
"""

import logging

from sqlalchemy.orm import Session

from app.llm.provider import get_llm
from app.models.document import Segment
from app.vectors.base import create_vector_store

logger = logging.getLogger("bluelink.dedup")

SIMILARITY_THRESHOLD = 0.9  # 余弦相似度阈值


class DedupService:

    @staticmethod
    def compute_delta(db: Session, doc_id: str) -> dict:
        """计算新文档与历史文档的重复区间

        Args:
            db: 数据库会话
            doc_id: 新文档的 ID

        Returns:
            {
                "folded_ranges": [{"segment_index_start": int, "segment_index_end": int, "reason": str}],
                "new_content_ratio": float  # 0.0~1.0
            }
        """
        # 1. 获取新文档所有切片
        segments = (
            db.query(Segment)
            .filter(Segment.doc_id == doc_id)
            .order_by(Segment.index_num)
            .all()
        )
        if not segments:
            return {"folded_ranges": [], "new_content_ratio": 1.0}

        # 2. 向量化每个切片
        store = create_vector_store()
        folded_ranges = []
        folded_count = 0

        for seg in segments:
            try:
                query_vec = get_llm().embed_text(seg.text)
                results = store.search(query_vec, top_k=5)

                # 排除自身（同一 doc_id 的匹配不计）
                external_results = [r for r in results if r.doc_id != doc_id]

                if external_results and external_results[0].score >= SIMILARITY_THRESHOLD:
                    folded_ranges.append({
                        "segment_index_start": seg.index_num,
                        "segment_index_end": seg.index_num,
                        "reason": f"与已有切片相似度 {external_results[0].score:.2f}",
                    })
                    folded_count += 1
            except Exception as e:
                logger.warning("切片去重失败 seg=%s: %s", seg.id, e)

        # 3. 合并连续重复区间
        folded_ranges = DedupService._merge_ranges(folded_ranges)

        new_content_ratio = 1.0 - (folded_count / len(segments))

        return {
            "folded_ranges": folded_ranges,
            "new_content_ratio": round(new_content_ratio, 4),
        }

    @staticmethod
    def _merge_ranges(ranges: list[dict]) -> list[dict]:
        """合并连续的重复区间"""
        if not ranges:
            return []

        sorted_ranges = sorted(ranges, key=lambda r: r["segment_index_start"])
        merged = [dict(sorted_ranges[0])]

        for r in sorted_ranges[1:]:
            last = merged[-1]
            if r["segment_index_start"] <= last["segment_index_end"] + 1:
                # 连续或重叠 → 合并
                last["segment_index_end"] = max(
                    last["segment_index_end"], r["segment_index_end"]
                )
            else:
                merged.append(dict(r))

        return merged
