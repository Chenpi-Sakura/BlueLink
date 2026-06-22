"""文档 API（V2.0 §7.1 / Task A）

文档上传、列表、切片查询、去重计算。
"""

import logging
import os
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends, File, Form, UploadFile, Query
from sqlalchemy.orm import Session

from app.core.auth import get_user_id
from app.core.config import settings
from app.models.database import get_db
from app.models.document import Document, Segment
from app.models.graph import GraphNode
from app.schemas.document import (
    DocumentDto,
    DocumentListDto,
    SegmentDto,
    SegmentListDto,
    DeltaResponse,
    FoldedRangeDto,
    UpdateDocumentRequest,
)
from app.services.document_service import DocumentService
from app.services.dedup_service import DedupService
from app.services.graph_builder import GraphBuilder

logger = logging.getLogger("bluelink.api.documents")
router = APIRouter(prefix="/api/v1/documents", tags=["文档"])

# 确保上传目录存在
UPLOAD_DIR = Path(settings.UPLOADS_DIR)
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)


@router.post("/upload", response_model=DocumentDto, response_model_by_alias=True)
def upload_document(
    file: UploadFile = File(...),
    privacy_level: str = Form("CLOUD_OK"),
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> DocumentDto:
    """上传文档

    接收 PDF / Word / Markdown / TXT 文件，解析切片后入库。
    """
    # 保存上传文件到临时目录
    ext = Path(file.filename or "file.txt").suffix
    tmp_path = UPLOAD_DIR / f"{uuid.uuid4()}{ext}"
    content = file.file.read()
    tmp_path.write_bytes(content)

    try:
        # 解析切片
        chunks = DocumentService.parse_and_chunk(str(tmp_path))

        # 入库 + 向量化
        doc = DocumentService.save_document(
            db=db,
            user_id=user_id,
            title=file.filename or "未命名文档",
            source=str(tmp_path),
            chunks=chunks,
            privacy_level=privacy_level,
        )

        # 创建图谱节点 + 排期关系发现
        try:
            node = GraphBuilder.ensure_node(
                db, user_id, doc.id, doc.title, "DOCUMENT",
            )
            db.commit()
            GraphBuilder.schedule_discovery(node.id, user_id)
        except Exception as e:
            logger.warning("图谱节点创建失败（不影响上传）: %s", e)

        logger.info(
            "文档上传成功 user=%s doc=%s chunks=%d",
            user_id, doc.id, len(chunks),
        )
        return DocumentDto(
            id=doc.id,
            title=doc.title,
            privacy_level=doc.privacy_level,
            source=doc.source,
            created_at=doc.created_at,
            concept_beacon=doc.concept_beacon,
        )
    finally:
        # 清理临时文件
        if tmp_path.exists():
            tmp_path.unlink(missing_ok=True)


@router.get("", response_model=DocumentListDto, response_model_by_alias=True)
def list_documents(
    cursor: str | None = Query(None),
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> DocumentListDto:
    """分页获取文档列表"""
    query = (
        db.query(Document)
        .filter(Document.user_id == user_id)
        .order_by(Document.created_at.desc())
        .limit(limit + 1)
        .all()
    )

    has_more = len(query) > limit
    items = query[:limit]

    return DocumentListDto(
        items=[
            DocumentDto(
                id=d.id,
                title=d.title,
                privacy_level=d.privacy_level,
                source=d.source,
                created_at=d.created_at,
                concept_beacon=d.concept_beacon,
            )
            for d in items
        ],
        next_cursor=items[-1].id if has_more else None,
    )


@router.get(
    "/{doc_id}/segments",
    response_model=SegmentListDto,
    response_model_by_alias=True,
)
def get_document_segments(
    doc_id: str,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> SegmentListDto:
    """获取文档所有切片（摘要信息，不含原文全文）"""
    segments = (
        db.query(Segment)
        .join(Document)
        .filter(
            Segment.doc_id == doc_id,
            Document.user_id == user_id,
        )
        .order_by(Segment.index_num)
        .all()
    )

    return SegmentListDto(
        segments=[
            SegmentDto(
                id=s.id,
                index=s.index_num,
                summary=s.summary,
                folded=s.is_folded,
            )
            for s in segments
        ]
    )


@router.post(
    "/{doc_id}/compute_delta",
    response_model=DeltaResponse,
    response_model_by_alias=True,
)
def compute_delta(
    doc_id: str,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> DeltaResponse:
    """计算文档去重结果"""
    result = DedupService.compute_delta(db=db, doc_id=doc_id)

    return DeltaResponse(
        folded_ranges=[
            FoldedRangeDto(**r) for r in result["folded_ranges"]
        ],
        new_content_ratio=result["new_content_ratio"],
    )


@router.put("/{doc_id}", response_model=DocumentDto, response_model_by_alias=True)
def update_document(
    doc_id: str,
    request: UpdateDocumentRequest,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
) -> DocumentDto:
    """更新文档信息（标题、隐私等级）"""
    doc = db.query(Document).filter(
        Document.id == doc_id, Document.user_id == user_id,
    ).first()
    if not doc:
        from fastapi import HTTPException
        raise HTTPException(404, detail="文档不存在")

    if request.title is not None:
        doc.title = request.title
    if request.privacy_level is not None:
        doc.privacy_level = request.privacy_level
    db.commit()
    db.refresh(doc)

    return DocumentDto(
        id=doc.id, title=doc.title, privacy_level=doc.privacy_level,
        source=doc.source, created_at=doc.created_at,
        concept_beacon=doc.concept_beacon,
    )


@router.delete("/{doc_id}", status_code=204)
def delete_document(
    doc_id: str,
    db: Session = Depends(get_db),
    user_id: str = Depends(get_user_id),
):
    """删除文档及其切片、向量和图谱节点"""
    doc = db.query(Document).filter(
        Document.id == doc_id, Document.user_id == user_id,
    ).first()
    if not doc:
        return

    # 删除图谱节点（级联删除边）
    db.query(GraphNode).filter(
        GraphNode.ref_id == doc_id, GraphNode.user_id == user_id,
    ).delete()

    # 删除向量
    try:
        from app.vectors.base import create_vector_store
        create_vector_store().delete_doc_vectors(doc_id)
    except Exception as e:
        logger.warning("向量删除失败: %s", e)

    # 删除文档（级联删除切片）
    db.delete(doc)
    db.commit()
    logger.info("文档已删除 user=%s doc=%s", user_id, doc_id)
