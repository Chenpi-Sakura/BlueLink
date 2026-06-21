# BlueLink 后端 — Python FastAPI 容器化
# 使用方式：docker compose up --build

FROM python:3.12-slim

WORKDIR /app

# 系统依赖（PyMuPDF、psycopg2 等需要）
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制源码
COPY . .

# 非 root 用户运行
RUN useradd -m -u 1000 bluelink && chown -R bluelink:bluelink /app
USER bluelink

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
