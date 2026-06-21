"""离线同步 API"""

from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/sync", tags=["同步"])
