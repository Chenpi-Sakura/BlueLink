"""NumpyVectorStore — numpy 文件回退实现（Task 0 / 开发测试用）

每文档一个 .npy 文件 + 一个 .json 元数据文件。
适合本地开发、单用户场景，文档数 < 1000 时性能够用。

目录结构：
    {storage_dir}/
        doc_abc123.npy       ← numpy 二维数组，[N, 1024]
        doc_abc123.json      ← { "segment_ids": ["seg-1", "seg-2", ...] }
"""

from __future__ import annotations

import json
import os
from pathlib import Path

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.vectors.base import VectorStore, SearchResult


class NumpyVectorStore(VectorStore):
    """numpy npy 文件实现 — 本地开发 / 单用户回退"""

    def __init__(self, storage_dir: str = "./vectors"):
        self._storage_dir = Path(storage_dir)
        self._storage_dir.mkdir(parents=True, exist_ok=True)

    # ─── 内部路径工具 ───────────────────────────────────────

    def _npy_path(self, doc_id: str) -> Path:
        return self._storage_dir / f"{doc_id}.npy"

    def _meta_path(self, doc_id: str) -> Path:
        return self._storage_dir / f"{doc_id}.json"

    # ─── 向量读写 ───────────────────────────────────────────

    def save_segment_vectors(
        self,
        doc_id: str,
        segment_vectors: list[tuple[str, list[float]]],
    ) -> None:
        """保存文档向量到 .npy + .json"""
        if not segment_vectors:
            return

        seg_ids, vectors = zip(*segment_vectors)
        arr = np.array(vectors, dtype=np.float32)

        np.save(self._npy_path(doc_id), arr)
        with open(self._meta_path(doc_id), "w") as f:
            json.dump({"segment_ids": list(seg_ids)}, f)

    def _load_doc(self, doc_id: str) -> tuple[np.ndarray, list[str]] | None:
        """加载某文档的向量矩阵和 ID 列表，文件不存在返回 None"""
        npy_path = self._npy_path(doc_id)
        meta_path = self._meta_path(doc_id)
        if not npy_path.exists() or not meta_path.exists():
            return None

        arr = np.load(npy_path)
        with open(meta_path) as f:
            meta = json.load(f)
        return arr, meta["segment_ids"]

    # ─── 检索 ────────────────────────────────────────────────

    def search(
        self,
        query_vector: list[float],
        top_k: int = 20,
        doc_ids: list[str] | None = None,
    ) -> list[SearchResult]:
        """遍历所有文档的向量文件，余弦相似度 top-k"""
        q_vec = np.array(query_vector, dtype=np.float32).reshape(1, -1)
        results: list[tuple[float, str, str]] = []  # (score, doc_id, seg_id)

        for npy_file in self._storage_dir.glob("*.npy"):
            doc_id = npy_file.stem
            if doc_ids is not None and doc_id not in doc_ids:
                continue
            loaded = self._load_doc(doc_id)
            if loaded is None:
                continue

            arr, seg_ids = loaded
            sims = cosine_similarity(q_vec, arr)[0]  # [N]

            for idx, score in enumerate(sims):
                results.append((float(score), doc_id, seg_ids[idx]))

        # 按分数降序排列
        results.sort(key=lambda x: x[0], reverse=True)
        results = results[:top_k]

        return [
            SearchResult(doc_id=doc_id, segment_id=seg_id, score=score)
            for score, doc_id, seg_id in results
        ]

    # ─── 删除 ────────────────────────────────────────────────

    def delete_doc_vectors(self, doc_id: str) -> None:
        """删除文档的 .npy 和 .json 文件"""
        self._npy_path(doc_id).unlink(missing_ok=True)
        self._meta_path(doc_id).unlink(missing_ok=True)
