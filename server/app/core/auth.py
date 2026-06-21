"""认证依赖 — 从 X-User-Id Header 获取用户 ID（V2.1 §4.3）"""

import re
from fastapi import Header, HTTPException

_USER_ID_PATTERN = re.compile(r"^[a-zA-Z0-9-]{32,64}$")


async def get_user_id(
    x_user_id: str = Header(..., description="客户端匿名用户 UUID"),
) -> str:
    if not _USER_ID_PATTERN.match(x_user_id):
        raise HTTPException(
            status_code=400,
            detail={
                "error_code": "INVALID_USER_ID",
                "message": "X-User-Id 格式无效，应为 32-64 位字母数字和连字符",
            },
        )
    return x_user_id
