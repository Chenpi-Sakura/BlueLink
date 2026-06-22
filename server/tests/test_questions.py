"""溯源提问 API 测试（Task B）

mock LLM 的 chat_json 和 embed_text，不真实调用 API Key。
"""

import pytest
from fastapi.testclient import TestClient


class FakeLLM:
    """mock LLMProvider — 同时 mock chat_json 和 embed_text"""

    @staticmethod
    def chat_json(system_prompt, user_prompt, temperature=None, max_retries=3):
        return {
            "introduction": "请查看以下原文段落，思考其中的关键论述...",
            "anchors": [
                {
                    "anchor_id": "a-001",
                    "doc_title": "深度工作",
                    "snippet": "深度工作是指在无干扰的状态下...",
                    "segment_id": "seg-001",
                    "score": 0.95,
                }
            ],
        }

    @staticmethod
    def embed_text(text: str) -> list[float]:
        return [0.1] * 1024  # 固定 mock 向量


def _mock_llm(monkeypatch):
    import app.services.anchor_engine as engine
    monkeypatch.setattr(engine, "get_llm", lambda: FakeLLM())


class TestAskQuestion:
    """溯源提问 API"""

    def test_ask_returns_200(self, client: TestClient, monkeypatch):
        """正常请求返回 200，字段 camelCase 映射正确"""
        _mock_llm(monkeypatch)

        response = client.post(
            "/api/v1/questions/ask",
            json={
                "query": "什么是深度工作？",
                "granularity": "SENTENCE",
                "scopeDocIds": ["doc-001"],
                "localSegmentIds": ["seg-local-001"],
            },
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )

        assert response.status_code == 200
        data = response.json()

        # 顶层字段 camelCase
        assert "introduction" in data
        assert "anchors" in data

        # 嵌套字段
        anchor = data["anchors"][0]
        assert anchor["anchorId"] == "a-001"
        assert anchor["docTitle"] == "深度工作"
        assert anchor["snippet"] == "深度工作是指在无干扰的状态下..."
        assert anchor["segmentId"] == "seg-001"
        assert anchor["score"] == 0.95

    def test_ask_minimal_request(self, client: TestClient, monkeypatch):
        """只有必填字段也能正常返回"""
        _mock_llm(monkeypatch)

        response = client.post(
            "/api/v1/questions/ask",
            json={"query": "测试问题"},
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200

    def test_ask_missing_query(self, client: TestClient):
        """空 query 返回 422"""
        response = client.post(
            "/api/v1/questions/ask",
            json={"query": ""},
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 422

    def test_ask_missing_user_id(self, client: TestClient, monkeypatch):
        """缺少 X-User-Id 返回 422"""
        _mock_llm(monkeypatch)

        response = client.post(
            "/api/v1/questions/ask",
            json={"query": "测试"},
        )
        assert response.status_code == 422
