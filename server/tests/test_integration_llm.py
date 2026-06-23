"""真实 LLM API 集成测试（手动触发，不默认运行）

依赖服务器已配置 LLM_API_KEY，会真实消耗 API 额度。

运行方式：
    pytest tests/ -v --run-integration          # 跑所有测试 + 集成测试
    pytest tests/test_integration_llm.py -v     # 只跑集成测试（需加参数）
"""

import pytest
from fastapi.testclient import TestClient


@pytest.mark.integration
class TestRealLLM:

    def test_chat_json_returns_valid_json(self):
        """真实调 LLM chat_json 返回合法 JSON"""
        from app.llm.provider import get_llm
        result = get_llm().chat_json(
            system_prompt="你是一个测试助手。只回复 JSON。",
            user_prompt="回复：{\"status\": \"ok\"}",
            temperature=0.1,
        )
        assert isinstance(result, dict)
        assert "status" in result
        assert result["status"] == "ok"

    def test_embed_text_returns_1024d(self):
        """真实调 LLM embed_text 返回 1024 维向量"""
        from app.llm.provider import get_llm
        vec = get_llm().embed_text("测试文本")
        assert isinstance(vec, list)
        assert len(vec) == 1024
        assert all(isinstance(v, float) for v in vec)

    def test_feynman_endpoint(self, client: TestClient):
        """费曼端点真实 LLM 调用返回正确结构"""
        response = client.post(
            "/api/v1/feynman/evaluate",
            json={
                "userExplanation": "深度工作就是长时间专注工作",
                "targetConcept": "深度工作",
                "contextSegmentIds": [],
            },
            headers={"X-User-Id": "integration-0000-0000-0000-00000001"},
        )
        assert response.status_code == 200
        data = response.json()
        assert "summary" in data
        assert "deviations" in data
        assert len(data["deviations"]) > 0
        assert "deviation_type" in data["deviations"][0] or "deviationType" in data["deviations"][0]

    def test_ask_endpoint(self, client: TestClient):
        """溯源提问端点真实 LLM 调用（空库时也应返回结构）"""
        response = client.post(
            "/api/v1/questions/ask",
            json={"query": "什么是深度工作？"},
            headers={"X-User-Id": "integration-0000-0000-0000-00000001"},
        )
        assert response.status_code == 200
        data = response.json()
        assert "introduction" in data
        assert "anchors" in data
