"""同步 API 测试（Task E）"""

from fastapi.testclient import TestClient


class TestSyncAPI:
    def test_batch_sync(self, client: TestClient):
        """批量同步返回正确统计"""
        response = client.post(
            "/api/v1/sync/batch",
            json=[
                {"operation": "CREATE_CARD", "localRefId": "local-001", "payloadJson": "{}"},
                {"operation": "CREATE_DOC", "localRefId": "local-002", "payloadJson": "{}"},
            ],
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["synced"] == 2
        assert data["failed"] == 0
        assert len(data["results"]) == 2

    def test_batch_sync_empty(self, client: TestClient):
        """空数组返回 0 同步"""
        response = client.post(
            "/api/v1/sync/batch",
            json=[],
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200
        assert response.json()["synced"] == 0

    def test_batch_sync_missing_user_id(self, client: TestClient):
        """缺少 X-User-Id 返回 422"""
        response = client.post(
            "/api/v1/sync/batch",
            json=[],
        )
        assert response.status_code == 422
