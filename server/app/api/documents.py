"""文档 API（V2.0 §7.1）"""

from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/documents", tags=["文档"])
