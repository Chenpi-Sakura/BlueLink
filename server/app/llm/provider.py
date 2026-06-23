"""LLM Provider — 封装 OpenAI 兼容协议，厂商无关（V2.0 §9.1.5 / V2.1 §5.1）

纯配置驱动，不绑定任何特定 AI 厂商。新增厂商只需改 .env 配置，不改代码。

用法：
    llm = get_llm()
    result = llm.chat_json(system_prompt, user_prompt)
    vector = llm.embed_text("hello")
"""

import json
import logging
from dataclasses import dataclass, field

import openai
from openai import OpenAI

from app.core.config import settings

logger = logging.getLogger("bluelink.llm")


@dataclass
class LLMConfig:
    """OpenAI 兼容协议配置 — 纯数据，不绑定任何厂商

    所有字段由 Settings 提供，用户通过 .env 自由切换厂商。
    """
    api_key: str
    base_url: str
    chat_model: str
    embed_model: str
    embed_base_url: str | None = None       # 独立 embedding 地址（可选，默认复用 chat 的）
    embed_api_key: str | None = None        # 独立 embedding 密钥（可选，默认复用 chat 的）
    temperature: float = 0.3
    timeout_sec: int = 30


class LLMProvider:
    """纯 OpenAI SDK 封装，内部无任何厂商分支"""

    def __init__(self, config: LLMConfig):
        self.config = config
        self._chat_client = OpenAI(
            api_key=config.api_key,
            base_url=config.base_url,
            timeout=config.timeout_sec,
        )
        self._embed_client = OpenAI(
            api_key=config.embed_api_key or config.api_key,
            base_url=config.embed_base_url or config.base_url,
            timeout=config.timeout_sec,
        )

    # ─── Chat ────────────────────────────────────────────────

    def chat_json(
        self,
        system_prompt: str,
        user_prompt: str,
        temperature: float | None = None,
        max_retries: int = 3,
    ) -> dict:
        """调 LLM 并返回结构化 JSON

        Args:
            system_prompt: 系统指令
            user_prompt: 用户输入
            temperature: 温度（默认取 config 值）
            max_retries: 超时重试次数

        Returns:
            解析后的 JSON dict

        Raises:
            LLMError: LLM 返回内容为空或非 JSON
            LLMTimeoutError: 重试耗尽后仍超时
        """
        temp = temperature if temperature is not None else self.config.temperature
        last_error: Exception | None = None

        for attempt in range(max_retries):
            try:
                response = self._chat_client.chat.completions.create(
                    model=self.config.chat_model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=temp,
                    response_format={"type": "json_object"},
                )
                content = response.choices[0].message.content
                if not content:
                    raise LLMError("LLM 返回空内容")
                return json.loads(content)

            except openai.APITimeoutError as e:
                last_error = e
                logger.warning("LLM 超时 (attempt %d/%d)", attempt + 1, max_retries)
                continue

            except openai.APIError as e:
                last_error = e
                logger.warning("LLM API 错误 (attempt %d/%d): %s", attempt + 1, max_retries, e)
                if attempt == max_retries - 1:
                    raise LLMError(f"LLM API 错误: {e}") from e
                continue

            except json.JSONDecodeError as e:
                raise LLMError(f"LLM 返回非 JSON: {content}") from e

        raise LLMTimeoutError(f"LLM 请求超时，已重试 {max_retries} 次")

    # ─── Embedding ───────────────────────────────────────────

    def embed_text(self, text: str) -> list[float]:
        """单段文本向量化"""
        try:
            resp = self._embed_client.embeddings.create(
                model=self.config.embed_model, input=text
            )
            return resp.data[0].embedding
        except openai.APIError as e:
            raise LLMError(f"Embedding API 错误: {e}") from e

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """批量文本向量化"""
        try:
            resp = self._embed_client.embeddings.create(
                model=self.config.embed_model, input=texts
            )
            sorted_data = sorted(resp.data, key=lambda x: x.index)
            return [d.embedding for d in sorted_data]
        except openai.APIError as e:
            raise LLMError(f"Embedding API 错误: {e}") from e


# ─── 异常 ──────────────────────────────────────────────────

class LLMError(Exception):
    pass


class LLMTimeoutError(LLMError):
    pass


# ─── 全局单例工厂 ─────────────────────────────────────────

_provider: LLMProvider | None = None


def get_llm() -> LLMProvider:
    """获取全局 LLMProvider 单例，配置从 Settings 读取"""
    global _provider
    if _provider is None:
        _provider = LLMProvider(
            LLMConfig(
                api_key=settings.LLM_API_KEY,
                base_url=settings.LLM_BASE_URL,
                chat_model=settings.LLM_CHAT_MODEL,
                embed_model=settings.LLM_EMBED_MODEL,
                embed_base_url=settings.LLM_EMBED_BASE_URL,
                embed_api_key=settings.LLM_EMBED_API_KEY,
                temperature=settings.LLM_TEMPERATURE,
                timeout_sec=settings.LLM_TIMEOUT_SEC,
            )
        )
    return _provider
