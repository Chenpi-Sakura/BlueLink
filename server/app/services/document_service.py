"""文档解析服务（Task A / V2.0 §9.7）

支持 PDF / Word / Markdown / TXT 格式解析和切片。
"""

import logging
import os
import uuid
from pathlib import Path

from sqlalchemy.orm import Session

from app.llm.provider import get_llm
from app.models.document import Document, Segment
from app.vectors.base import create_vector_store

logger = logging.getLogger("bluelink.document")

# 最大切片字符数
MAX_CHUNK_SIZE = 500


class DocumentService:

    @staticmethod
    def parse_and_chunk(file_path: str) -> list[dict]:
        """解析文档文件，按段落切片

        Args:
            file_path: 文件路径

        Returns:
            [{"index": int, "text": str}, ...]
        """
        ext = Path(file_path).suffix.lower()

        if ext == ".pdf":
            text = DocumentService._extract_pdf(file_path)
        elif ext in (".docx", ".doc"):
            text = DocumentService._extract_docx(file_path)
        else:
            # .md, .txt 等纯文本
            with open(file_path, encoding="utf-8", errors="replace") as f:
                text = f.read()

        return DocumentService._chunk_by_paragraph(text)

    @staticmethod
    def _extract_pdf(file_path: str) -> str:
        """PyMuPDF 提取 PDF 文本"""
        import fitz  # PyMuPDF
        doc = fitz.open(file_path)
        pages = []
        for page in doc:
            text = page.get_text().strip()
            if text:
                pages.append(text)
        doc.close()
        return "\n\n".join(pages)

    @staticmethod
    def _extract_docx(file_path: str) -> str:
        """python-docx 提取 Word 文本"""
        from docx import Document as DocxDocument
        doc = DocxDocument(file_path)
        paragraphs = [p.text.strip() for p in doc.paragraphs if p.text.strip()]
        return "\n\n".join(paragraphs)

    @staticmethod
    def _chunk_by_paragraph(text: str, max_len: int = MAX_CHUNK_SIZE) -> list[dict]:
        """按空行分段，每段不超过 max_len 字"""
        paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
        chunks: list[dict] = []
        buf, idx = "", 0

        for p in paragraphs:
            if len(buf) + len(p) > max_len and buf:
                chunks.append({"index": idx, "text": buf.strip()})
                idx += 1
                buf = ""
            buf += p + "\n"

        if buf.strip():
            chunks.append({"index": idx, "text": buf.strip()})

        return chunks

    @staticmethod
    def save_document(
        db: Session,
        user_id: str,
        title: str,
        source: str | None,
        chunks: list[dict],
        privacy_level: str = "CLOUD_OK",
    ) -> Document:
        """保存文档及切片到数据库，并向量化

        Args:
            db: 数据库会话
            user_id: 用户 ID
            title: 文档标题
            source: 来源路径
            chunks: parse_and_chunk 的输出
            privacy_level: 隐私等级

        Returns:
            创建的 Document ORM 对象
        """
        import time
        now = int(time.time() * 1000)
        doc_id = str(uuid.uuid4())

        # 1. 创建文档记录
        doc = Document(
            id=doc_id,
            user_id=user_id,
            title=title,
            privacy_level=privacy_level,
            source=source,
            created_at=now,
        )
        db.add(doc)
        db.flush()

        # 2. 创建切片记录
        segments: list[Segment] = []
        for chunk in chunks:
            seg = Segment(
                id=str(uuid.uuid4()),
                doc_id=doc_id,
                index_num=chunk["index"],
                text=chunk["text"],
                summary=chunk["text"][:80],
            )
            segments.append(seg)
            db.add(seg)
        db.flush()

        # 3. 先提交 DB 事务，确保 segments 已持久化（否则 VectorStore 外键约束会失败）
        db.commit()

        # 4. 向量化并存入 VectorStore
        try:
            texts = [s.text for s in segments]
            vectors = get_llm().embed_texts(texts)
            segment_vectors = [
                (s.id, vec) for s, vec in zip(segments, vectors)
            ]
            store = create_vector_store()
            store.save_segment_vectors(doc_id, segment_vectors)
            logger.info("向量化成功: %d 个切片", len(vectors))
        except Exception as e:
            logger.warning("向量化失败（跳过）: %s", e)
        logger.info(
            "文档保存成功 user=%s doc=%s chunks=%d",
            user_id, doc_id, len(segments),
        )
        return doc
