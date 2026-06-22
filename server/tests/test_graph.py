"""知识图谱 API 测试（Task D）

测试 GET /api/v1/graph 端点的功能和数据格式。
"""

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.models.database import engine, SessionLocal
from app.models.graph import GraphNode, GraphEdge


def _seed_graph_data():
    """注入测试图谱数据"""
    db = SessionLocal()
    try:
        # 清空旧数据
        db.query(GraphEdge).delete()
        db.query(GraphNode).delete()

        uid = "test-user-0000-0000-0000-000000000001"
        nodes = [
            GraphNode(id="n1", user_id=uid, label="深度工作", type="CONCEPT"),
            GraphNode(id="n2", user_id=uid, label="Deep Work", type="DOCUMENT"),
            GraphNode(id="n3", user_id="other-user", label="无关节点", type="CONCEPT"),
        ]
        db.add_all(nodes)
        db.flush()

        edges = [
            GraphEdge(id="e1", source_id="n1", target_id="n2",
                      relation="CITE", confidence=0.95, is_manual=False),
        ]
        db.add_all(edges)
        db.commit()
    finally:
        db.close()


class TestGraphAPI:
    """图谱 API 测试"""

    def test_fetch_graph_empty(self, client: TestClient):
        """空图谱返回空列表"""
        response = client.get(
            "/api/v1/graph",
            headers={"X-User-Id": "empty-user-0000-0000-0000-000000000000"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["nodes"] == []
        assert data["edges"] == []

    def test_fetch_graph_with_data(self, client: TestClient):
        """有数据的图谱返回节点和边"""
        _seed_graph_data()

        response = client.get(
            "/api/v1/graph",
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200
        data = response.json()

        # 返回 camelCase 字段
        assert len(data["nodes"]) == 2  # 只返回该用户的节点
        assert len(data["edges"]) == 1

        # 节点字段
        node = data["nodes"][0]
        assert "id" in node
        assert "label" in node
        assert "type" in node
        assert "refId" in node  # camelCase

        # 边字段
        edge = data["edges"][0]
        assert "source" in edge
        assert "target" in edge
        assert "relation" in edge
        assert "confidence" in edge
        assert "isManual" in edge  # camelCase

    def test_fetch_graph_cursor_and_limit(self, client: TestClient):
        """cursor 和 limit 参数正常接受"""
        response = client.get(
            "/api/v1/graph?cursor=abc&limit=100",
            headers={"X-User-Id": "test-user-0000-0000-0000-000000000001"},
        )
        assert response.status_code == 200

    def test_fetch_graph_missing_user_id(self, client: TestClient):
        """缺少 X-User-Id 返回 422"""
        response = client.get("/api/v1/graph")
        assert response.status_code == 422
