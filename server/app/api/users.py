"""用户 API"""

from fastapi import APIRouter

router = APIRouter(prefix="/api/v1/users", tags=["用户"])
