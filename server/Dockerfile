# BlueLink 后端 — Python FastAPI 容器化
# 使用方式：docker compose up --build
# 中国大陆构建默认使用阿里云 pip 镜像（可换其他镜像）：
#   docker build --build-arg PIP_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple .

FROM python:3.12-slim

WORKDIR /app

# 系统依赖 — 使用清华镜像源加速国内构建
RUN sed -i 's|deb.debian.org|mirrors.tuna.tsinghua.edu.cn|g' /etc/apt/sources.list.d/debian.sources 2>/dev/null || true && \
    sed -i 's|deb.debian.org|mirrors.tuna.tsinghua.edu.cn|g' /etc/apt/sources.list 2>/dev/null || true && \
    apt-get update && apt-get install -y --no-install-recommends \
        libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖（利用 Docker 层缓存，requirements.txt 不变时不重装）
COPY requirements.txt .
ARG PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
RUN pip install --no-cache-dir --default-timeout=120 -r requirements.txt

# 复制源码
COPY . .

# 非 root 用户运行
RUN useradd -m -u 1000 bluelink && mkdir -p /app/uploads && chown -R bluelink:bluelink /app
USER bluelink

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
