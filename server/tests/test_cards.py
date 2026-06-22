"""灵感卡片 API 测试（Task E）"""

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
