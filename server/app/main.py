"""BlueLink 后端入口 — FastAPI 应用（V2.0 §4.2 / V2.1 §5）"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.models.database import init_db
from app.api import documents, questions, feynman, graph, cards, sync, users

logger = logging.getLogger("bluelink")


def _init_db():
    init_db()
    logger.info("数据库表已就绪")


def _seed_data():
    """启动时注入演示数据"""
    try:
        from scripts.seed import seed
        seed()
    except Exception as e:
        logger.warning("seed 跳过: %s", e)


def _register_routers(app: FastAPI):
    app.include_router(documents.router)
    app.include_router(questions.router)
    app.include_router(feynman.router)
    app.include_router(graph.router)
    app.include_router(cards.router)
    app.include_router(sync.router)
    app.include_router(users.router)


def _register_exception_handlers(app: FastAPI):
    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        logger.exception("未捕获异常: %s", exc)
        return JSONResponse(
            status_code=500,
            content={"error_code": "INTERNAL", "message": "服务器内部错误"},
        )


@asynccontextmanager
async def lifespan(app: FastAPI):
    logging.basicConfig(level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO))
    logger.info("BlueLink 后端启动 — environment=%s", settings.ENV)
    _init_db()
    _seed_data()
    yield
    logger.info("BlueLink 后端关闭")


app = FastAPI(title="BlueLink API", description="蓝链 — AI 阅读与知识管理后端", version="2.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_register_routers(app)
_register_exception_handlers(app)


@app.get("/health")
async def health_check():
    return {"status": "ok", "version": "2.1.0"}
