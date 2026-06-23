"""用户 API 测试（Task E）"""

from fastapi.testclient import TestClient


class TestUsersAPI:
    def test_get_user_me(self, client: TestClient):
        """获取用户信息"""
        response = client.get(
            "/api/v1/users/me",
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200
        assert response.json()["userId"] == "test-user-0000-0000-0000-000000000001"

    def test_get_user_me_missing_id(self, client: TestClient):
        """缺少 X-User-Id 返回 422"""
        response = client.get("/api/v1/users/me")
        assert response.status_code == 422

    def test_delete_user_me(self, client: TestClient):
        """删除用户返回 204"""
        response = client.delete(
            "/api/v1/users/me",
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 204
