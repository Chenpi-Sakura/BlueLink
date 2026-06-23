"""Seed 脚本 — 启动时自动注入演示数据

检测数据库是否为空，是则注入示例文档、灵感卡片和图谱数据。
"""

import logging
import uuid
from pathlib import Path

from app.core.config import settings
from app.models.database import SessionLocal
from app.models.document import Document, Segment, InspirationCard
from app.models.graph import GraphNode, GraphEdge
from app.services.document_service import DocumentService
from app.services.graph_builder import GraphBuilder

logger = logging.getLogger("bluelink.seed")

SEED_USER_ID = "seed-user-0000-0000-0000-000000000000"

# 示例灵感卡片
SAMPLE_CARDS = [
    ("费曼学习法的核心是用输出倒逼输入", ["费曼", "学习法"]),
    ("信息增量的本质是减少认知熵", ["认知", "信息增量"]),
    ("AI 不应给答案，应给路径——蓝链的理念", ["AI", "产品哲学"]),
    ("Room FTS 在 MVP 阶段完全够用", ["Room", "MVP"]),
    ("知识图谱将碎片信息连接成网络", ["图谱", "知识管理"]),
]

# 示例图谱节点（固定 ID 便于关联）
SEED_NODE_IDS = {
    "deep_work_concept": str(uuid.uuid4()),
    "feynman": str(uuid.uuid4()),
    "info_delta": str(uuid.uuid4()),
    "deep_work_doc": str(uuid.uuid4()),
    "card_1": str(uuid.uuid4()),
}

SAMPLE_NODES = [
    (SEED_NODE_IDS["deep_work_concept"], "深度工作", "CONCEPT"),
    (SEED_NODE_IDS["feynman"], "费曼学习法", "CONCEPT"),
    (SEED_NODE_IDS["info_delta"], "信息增量", "CONCEPT"),
    (SEED_NODE_IDS["deep_work_doc"], "Deep Work", "DOCUMENT"),
    (SEED_NODE_IDS["card_1"], "灵感卡片 #1", "INSPIRATION"),
]

# 示例图谱边
SAMPLE_EDGES = [
    (SEED_NODE_IDS["deep_work_concept"], SEED_NODE_IDS["deep_work_doc"], "CITE", 0.95),
    (SEED_NODE_IDS["deep_work_concept"], SEED_NODE_IDS["feynman"], "SUPPLEMENT", 0.7),
    (SEED_NODE_IDS["feynman"], SEED_NODE_IDS["card_1"], "SUPPORT", 0.85),
    (SEED_NODE_IDS["info_delta"], SEED_NODE_IDS["deep_work_concept"], "CHALLENGE", 0.6),
    (SEED_NODE_IDS["info_delta"], SEED_NODE_IDS["card_1"], "SUPPLEMENT", 0.75),
    (SEED_NODE_IDS["deep_work_doc"], SEED_NODE_IDS["deep_work_concept"], "CITE", 0.9),
    (SEED_NODE_IDS["feynman"], SEED_NODE_IDS["deep_work_concept"], "SUPPORT", 0.8),
    (SEED_NODE_IDS["card_1"], SEED_NODE_IDS["info_delta"], "SUPPORT", 0.7),
]


def seed():
    """注入演示数据（幂等：已有数据则跳过）"""
    db = SessionLocal()
    try:
        # 检测是否已有 seed 数据
        existing = db.query(Document).filter(
            Document.user_id == SEED_USER_ID
        ).count()
        if existing > 0:
            logger.info("数据库已有 seed 数据，跳过")
            return

        now = int(__import__("time").time() * 1000)
        logger.info("开始注入演示数据...")

        # 1. 导入 resource/ 目录下所有 PDF
        resource_dir = Path(__file__).resolve().parent.parent / "resource"
        pdf_files = sorted(resource_dir.glob("*.pdf"))
        if pdf_files:
            for pdf_path in pdf_files:
                try:
                    chunks = DocumentService.parse_and_chunk(str(pdf_path))
                    doc = DocumentService.save_document(
                        db=db,
                        user_id=SEED_USER_ID,
                        title=pdf_path.stem,
                        source=str(pdf_path),
                        chunks=chunks,
                        privacy_level="CLOUD_OK",
                    )
                    GraphBuilder.ensure_node(
                        db, SEED_USER_ID, doc.id, doc.title, "DOCUMENT",
                    )
                    logger.info("  ✅ 导入文档: %s (%d 切片)", doc.title, len(chunks))
                except Exception as e:
                    logger.warning("  ⚠️ 文档导入失败（跳过 %s）: %s", pdf_path.name, e)
        else:
            logger.info("  ⏭️ 未找到 PDF 文件，跳过文档导入")

        # 2. 创建灵感卡片
        for content, tags in SAMPLE_CARDS:
            card_id = str(uuid.uuid4())
            card = InspirationCard(
                id=card_id,
                user_id=SEED_USER_ID,
                content=content,
                type="TEXT",
                privacy_level="LOCAL_ONLY",
                tags=",".join(tags),
                created_at=now,
            )
            db.add(card)
        logger.info("  ✅ 导入 %d 条灵感卡片", len(SAMPLE_CARDS))

        # 3. 创建图谱节点（DOCUMENT/INSPIRATION 类型设 ref_id 以便溯源内容）
        for nid, label, ntype in SAMPLE_NODES:
            ref_id = SEED_NODE_IDS.get("deep_work_doc") if nid == SEED_NODE_IDS.get("deep_work_doc") and ntype == "DOCUMENT" else None
            node = GraphNode(
                id=nid,
                user_id=SEED_USER_ID,
                label=label,
                type=ntype,
                ref_id=ref_id,
            )
            db.add(node)
        logger.info("  ✅ 导入 %d 个图谱节点", len(SAMPLE_NODES))

        # 4. 创建图谱边
        for src, tgt, rel, conf in SAMPLE_EDGES:
            edge = GraphEdge(
                id=f"e-{src}-{tgt}",
                source_id=src,
                target_id=tgt,
                relation=rel,
                confidence=conf,
                is_manual=False,
            )
            db.add(edge)
        logger.info("  ✅ 导入 %d 条图谱边", len(SAMPLE_EDGES))

        db.commit()
        logger.info("演示数据注入完成")
    except Exception as e:
        db.rollback()
        logger.error("seed 失败: %s", e)
        raise
    finally:
        db.close()
