"""文档 API 测试（Task A）

mock LLM embed_texts，不真实调用 API Key。
"""

import io
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.models.document import Document, Segment

SAMPLE_TXT = """这是第一段内容。测试文档的解析和切片功能。

这是第二段。包含一些更详细的信息用于验证。

第三段在这里，确保分段逻辑正确。
"""


class FakeLLM:
    @staticmethod
    def chat_json(system_prompt, user_prompt, temperature=None, max_retries=3):
        return {}

    @staticmethod
    def embed_text(text: str) -> list[float]:
        return [0.1] * 1024

    @staticmethod
    def embed_texts(texts: list[str]) -> list[list[float]]:
        return [[0.1] * 1024 for _ in texts]


def _mock_llm(monkeypatch):
    import app.services.document_service as ds
    import app.services.dedup_service as dedup
    monkeypatch.setattr(ds, "get_llm", lambda: FakeLLM())
    monkeypatch.setattr(dedup, "get_llm", lambda: FakeLLM())


class TestDocumentService:
    """文档解析单元测试"""

    def test_parse_txt(self, tmp_path: Path):
        from app.services.document_service import DocumentService
        txt_file = tmp_path / "test.txt"
        txt_file.write_text(SAMPLE_TXT, encoding="utf-8")

        chunks = DocumentService.parse_and_chunk(str(txt_file))
        assert len(chunks) >= 3
        assert chunks[0]["text"].startswith("这是第一段")
        assert "index" in chunks[0]

    def test_parse_empty(self, tmp_path: Path):
        from app.services.document_service import DocumentService
        txt_file = tmp_path / "empty.txt"
        txt_file.write_text("")
        chunks = DocumentService.parse_and_chunk(str(txt_file))
        assert chunks == []


class TestDocumentsAPI:
    """文档 API 集成测试"""

    UPLOAD_HEADERS = {"X-User-Id": "test-user-0000-0000-0000-000000000001"}

    def _upload(self, client, filename="test.txt", content=SAMPLE_TXT):
        return client.post(
            "/api/v1/documents/upload",
            files={"file": (filename, content.encode("utf-8"))},
            data={"privacy_level": "CLOUD_OK"},
            headers=self.UPLOAD_HEADERS,
        )

    def test_upload_txt(self, client: TestClient, monkeypatch):
        """上传 TXT 返回文档信息"""
        _mock_llm(monkeypatch)
        resp = self._upload(client)
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] is not None
        assert data["title"] == "test.txt"
        assert "privacyLevel" in data
        assert "createdAt" in data

    def test_list_documents(self, client: TestClient, monkeypatch):
        """上传后列表包含新文档"""
        _mock_llm(monkeypatch)
        self._upload(client)

        resp = client.get("/api/v1/documents", headers=self.UPLOAD_HEADERS)
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["items"]) >= 1
        assert "nextCursor" in data

    def test_get_segments(self, client: TestClient, monkeypatch):
        """获取切片列表"""
        _mock_llm(monkeypatch)
        upload_resp = self._upload(client)
        doc_id = upload_resp.json()["id"]

        resp = client.get(
            f"/api/v1/documents/{doc_id}/segments",
            headers=self.UPLOAD_HEADERS,
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["segments"]) >= 3
        assert "id" in data["segments"][0]
        assert "index" in data["segments"][0]

    def test_compute_delta(self, client: TestClient, monkeypatch):
        """去重计算返回结构正确"""
        _mock_llm(monkeypatch)
        upload_resp = self._upload(client)
        doc_id = upload_resp.json()["id"]

        resp = client.post(
            f"/api/v1/documents/{doc_id}/compute_delta",
            headers=self.UPLOAD_HEADERS,
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "foldedRanges" in data
        assert "newContentRatio" in data
        assert 0.0 <= data["newContentRatio"] <= 1.0

    def test_upload_missing_user_id(self, client: TestClient):
        """缺少 X-User-Id 返回 422"""
        resp = client.post(
            "/api/v1/documents/upload",
            files={"file": ("test.txt", b"hello")},
            data={"privacy_level": "CLOUD_OK"},
        )
        assert resp.status_code == 422
