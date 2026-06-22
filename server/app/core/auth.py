"""认证依赖 — 演示模式：所有设备共享同一用户 ID"""

DEMO_USER_ID = "seed-user-0000-0000-0000-000000000000"


async def get_user_id() -> str:
    """演示期间所有设备共享同一用户 ID，后续接入登录时改回 Header 校验"""
    return DEMO_USER_ID
