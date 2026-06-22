-- BlueLink 数据库初始化 — PostgreSQL 17 + pgvector
-- 首次启动时由 docker-entrypoint-initdb.d 自动执行

-- 1. 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 建表（与 SQLAlchemy ORM 模型一致，见 app/models/）

-- 文档元数据
CREATE TABLE IF NOT EXISTS documents (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    privacy_level TEXT NOT NULL DEFAULT 'LOCAL_FIRST',
    source TEXT,
    created_at BIGINT NOT NULL,
    concept_beacon TEXT
);
CREATE INDEX IF NOT EXISTS idx_documents_user ON documents(user_id);

-- 文档切片（明文存储，不含加密）
CREATE TABLE IF NOT EXISTS segments (
    id TEXT PRIMARY KEY,
    doc_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    index_num INTEGER NOT NULL,
    text TEXT NOT NULL DEFAULT '',
    summary TEXT DEFAULT '',
    is_folded BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_segments_doc ON segments(doc_id);

-- 切片向量（pgvector）
CREATE TABLE IF NOT EXISTS segment_vectors (
    segment_id TEXT PRIMARY KEY REFERENCES segments(id) ON DELETE CASCADE,
    doc_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    embedding vector(1024)
);
CREATE INDEX IF NOT EXISTS idx_segment_vectors_doc ON segment_vectors(doc_id);
CREATE INDEX IF NOT EXISTS idx_segment_vectors_embedding
    ON segment_vectors USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 锚点缓存
CREATE TABLE IF NOT EXISTS anchors (
    id TEXT PRIMARY KEY,
    query_hash TEXT NOT NULL,
    segment_id TEXT NOT NULL REFERENCES segments(id),
    doc_title TEXT DEFAULT '',
    snippet TEXT DEFAULT '',
    score REAL DEFAULT 0.0,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_anchors_query ON anchors(query_hash);

-- 图谱节点
CREATE TABLE IF NOT EXISTS graph_nodes (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    label TEXT NOT NULL,
    type TEXT NOT NULL,
    ref_id TEXT
);
CREATE INDEX IF NOT EXISTS idx_graph_nodes_user ON graph_nodes(user_id);

-- 图谱边
CREATE TABLE IF NOT EXISTS graph_edges (
    id TEXT PRIMARY KEY,
    source_id TEXT NOT NULL REFERENCES graph_nodes(id) ON DELETE CASCADE,
    target_id TEXT NOT NULL REFERENCES graph_nodes(id) ON DELETE CASCADE,
    relation TEXT NOT NULL,
    confidence REAL DEFAULT 0.0,
    is_manual BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_graph_edges_source ON graph_edges(source_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_target ON graph_edges(target_id);

-- 灵感卡片
CREATE TABLE IF NOT EXISTS inspiration_cards (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT 'TEXT',
    privacy_level TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
    tags TEXT DEFAULT '',
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cards_user ON inspiration_cards(user_id);

-- 离线同步队列
CREATE TABLE IF NOT EXISTS pending_sync (
    id TEXT PRIMARY KEY,
    operation TEXT NOT NULL,
    local_ref_id TEXT NOT NULL,
    server_ref_id TEXT,
    payload_json TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT,
    status TEXT DEFAULT 'PENDING'
);
CREATE INDEX IF NOT EXISTS idx_pending_sync_status ON pending_sync(status);
