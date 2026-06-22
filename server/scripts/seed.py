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

# 示例图谱节点
SAMPLE_NODES = [
    ("n1", "深度工作", "CONCEPT"),
    ("n2", "费曼学习法", "CONCEPT"),
    ("n3", "信息增量", "CONCEPT"),
    ("n4", "Deep Work", "DOCUMENT"),
    ("n5", "灵感卡片 #1", "INSPIRATION"),
]

# 示例图谱边
SAMPLE_EDGES = [
    ("n1", "n4", "CITE", 0.95),
    ("n1", "n2", "SUPPLEMENT", 0.7),
    ("n2", "n5", "SUPPORT", 0.85),
    ("n3", "n1", "CHALLENGE", 0.6),
    ("n3", "n5", "SUPPLEMENT", 0.75),
    ("n4", "n1", "CITE", 0.9),
    ("n2", "n1", "SUPPORT", 0.8),
    ("n5", "n3", "SUPPORT", 0.7),
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

        # 1. 用 PDF 创建示例文档（如果存在）
        pdf_path = Path(__file__).resolve().parent.parent / "resource" / "ai_agents_handbook.pdf"
        if pdf_path.exists():
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
                logger.info("  ✅ 导入文档: %s (%d 切片)", doc.title, len(chunks))
            except Exception as e:
                logger.warning("  ⚠️ 文档导入失败（跳过）: %s", e)
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

        # 3. 创建图谱节点
        for nid, label, ntype in SAMPLE_NODES:
            node = GraphNode(
                id=nid,
                user_id=SEED_USER_ID,
                label=label,
                type=ntype,
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
