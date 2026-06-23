"""灵感卡片 API 测试（Task E + 关键词提取）"""

import pytest
from fastapi.testclient import TestClient


class TestCardsAPI:
    def test_create_card(self, client: TestClient):
        """创建卡片返回正确结构"""
        response = client.post(
            "/api/v1/cards",
            json={
                "content": "测试灵感卡片",
                "type": "TEXT",
                "privacyLevel": "LOCAL_ONLY",
                "tags": ["测试", "灵感"],
            },
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["id"] is not None
        assert data["content"] == "测试灵感卡片"
        assert data["type"] == "TEXT"
        assert data["privacyLevel"] == "LOCAL_ONLY"
        assert "tags" in data
        assert data["createdAt"] > 0

    def test_create_card_minimal(self, client: TestClient):
        """只有必填字段也能创建"""
        response = client.post(
            "/api/v1/cards",
            json={"content": "简约卡片"},
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200

    def test_create_card_empty_content(self, client: TestClient):
        """空内容返回 422"""
        response = client.post(
            "/api/v1/cards",
            json={"content": ""},
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 422

    def test_create_card_missing_user_id(self, client: TestClient):
        """缺少 X-User-Id 返回 422"""
        response = client.post(
            "/api/v1/cards",
            json={"content": "测试"},
        )
        assert response.status_code == 422


class TestCardKeywordExtraction:
    """关键词提取测试"""

    HEADERS = {"X-User-Id": "test-user-0000-0000-0000-000000000000"}

    def test_extract_keywords_when_tags_empty(self, client: TestClient, monkeypatch):
        """空 tags 时调 LLM 提取关键词"""
        import app.api.cards as cards
        class FakeLLM:
            @staticmethod
            def chat_json(system_prompt, user_prompt, temperature=None, max_retries=3):
                return {"keywords": ["费曼", "学习法", "认知"]}
        monkeypatch.setattr(cards, "get_llm", lambda: FakeLLM())

        resp = client.post(
            "/api/v1/cards",
            json={"content": "费曼学习法的核心是用输出倒逼输入"},
            headers=self.HEADERS,
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "费曼" in data["tags"]
        assert "学习法" in data["tags"]

    def test_skip_extraction_when_tags_provided(self, client: TestClient, monkeypatch):
        """已传 tags 时不调 LLM"""
        called = False
        import app.api.cards as cards
        original = cards.get_llm
        def fake_get_llm():
            nonlocal called
            called = True
            return original()
        monkeypatch.setattr(cards, "get_llm", fake_get_llm)

        resp = client.post(
            "/api/v1/cards",
            json={"content": "测试内容", "tags": ["手动"]},
            headers=self.HEADERS,
        )
        assert resp.status_code == 200
        assert not called, "已传 tags 时不应调用 LLM"

    def test_list_cards(self, client: TestClient, monkeypatch):
        """列出卡片"""
        import app.api.cards as cards
        class FakeLLM:
            @staticmethod
            def chat_json(*a, **kw): return {"keywords": ["标签"]}
        monkeypatch.setattr(cards, "get_llm", lambda: FakeLLM())

        # 先创建一张卡片
        client.post("/api/v1/cards", json={"content": "列表测试"}, headers=self.HEADERS)

        resp = client.get("/api/v1/cards", headers=self.HEADERS)
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) >= 1
        assert "content" in data[0]

    def test_delete_card(self, client: TestClient, monkeypatch):
        """删除卡片返回 204"""
        import app.api.cards as cards
        class FakeLLM:
            @staticmethod
            def chat_json(*a, **kw): return {"keywords": ["标签"]}
        monkeypatch.setattr(cards, "get_llm", lambda: FakeLLM())

        create = client.post("/api/v1/cards", json={"content": "待删除"}, headers=self.HEADERS)
        card_id = create.json()["id"]

        resp = client.delete(f"/api/v1/cards/{card_id}", headers=self.HEADERS)
        assert resp.status_code == 204

    def test_skip_extraction_when_content_short(self, client: TestClient, monkeypatch):
        """短内容（≤5字）不调 LLM"""
        called = False
        import app.api.cards as cards
        original = cards.get_llm
        def fake_get_llm():
            nonlocal called
            called = True
            return original()
        monkeypatch.setattr(cards, "get_llm", fake_get_llm)

        resp = client.post(
            "/api/v1/cards",
            json={"content": "测试"},
            headers=self.HEADERS,
        )
        assert resp.status_code == 200
        assert not called, "短内容不应调用 LLM"
