"""费曼伴学 API 测试（Task C）

使用 monkeypatch（内置）mock LLM，不真实调用 API Key。
"""

import pytest
from fastapi.testclient import TestClient


def _mock_feynman_response(monkeypatch):
    """mock LLMProvider.chat_json 返回固定费曼结果"""
    import app.services.feynman_evaluator as ev

    class FakeLLM:
        @staticmethod
        def chat_json(system_prompt, user_prompt, temperature=None, max_retries=3):
            return {
                "summary": "基本准确，但遗漏了一个关键点",
                "deviations": [
                    {
                        "user_segment": "用户表述中的一部分",
                        "deviation_type": "OMISSION",
                        "explanation": "原文还提到了'无干扰'的前提条件",
                        "original_snippet": "深度工作是指在无干扰的状态下...",
                        "anchor_segment_id": "seg-001",
                    }
                ],
                "gravity_lines": [
                    {"from": 5, "to_segment_id": "seg-001"},
                ],
            }

    monkeypatch.setattr(ev, "get_llm", lambda: FakeLLM())


class TestFeynmanAPI:
    """费曼 API 接口测试"""

    def test_evaluate_returns_200_with_mocked_llm(self, client: TestClient, monkeypatch):
        """正常请求返回 200，字段映射正确"""
        _mock_feynman_response(monkeypatch)

        response = client.post(
            "/api/v1/feynman/evaluate",
            json={
                "userExplanation": "深度工作就是长时间专注工作",
                "targetConcept": "深度工作",
                "contextSegmentIds": ["seg-001", "seg-002"],
            },
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )

        assert response.status_code == 200
        data = response.json()

        # 响应字段应为 camelCase（与 Android 端一致）
        assert "summary" in data
        assert "deviations" in data
        assert "gravityLines" in data  # camelCase

        # deviations 子字段
        dev = data["deviations"][0]
        assert dev["userSegment"] == "用户表述中的一部分"
        assert dev["deviationType"] == "OMISSION"
        assert dev["explanation"] == "原文还提到了'无干扰'的前提条件"
        assert dev["originalSnippet"] == "深度工作是指在无干扰的状态下..."
        assert dev["anchorSegmentId"] == "seg-001"

        # gravityLines 子字段
        gl = data["gravityLines"][0]
        assert gl["from"] == 5            # 特殊 alias
        assert gl["toSegmentId"] == "seg-001"

    def test_evaluate_empty_segments(self, client: TestClient, monkeypatch):
        """空 contextSegmentIds 也应正常返回"""
        _mock_feynman_response(monkeypatch)

        response = client.post(
            "/api/v1/feynman/evaluate",
            json={
                "userExplanation": "测试解释",
                "targetConcept": "测试概念",
                "contextSegmentIds": [],
            },
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200

    def test_evaluate_missing_user_id(self, client: TestClient, monkeypatch):
        """缺少 X-User-Id 返回 400"""
        _mock_feynman_response(monkeypatch)

        response = client.post(
            "/api/v1/feynman/evaluate",
            json={
                "userExplanation": "测试",
                "targetConcept": "测试",
                "contextSegmentIds": [],
            },
        )
        assert response.status_code == 400

    def test_evaluate_blank_explanation(self, client: TestClient):
        """空的 userExplanation 返回 422"""
        response = client.post(
            "/api/v1/feynman/evaluate",
            json={
                "userExplanation": "",
                "targetConcept": "测试",
                "contextSegmentIds": [],
            },
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 422
