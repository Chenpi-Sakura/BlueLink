"""LLM Provider — 封装 OpenAI 兼容协议（V2.0 §9.1.5 / V2.1 §5.1）"""

import json
import logging
from typing import Any

import openai
from openai import OpenAI

from app.core.config import settings

logger = logging.getLogger("bluelink.llm")


class LLMProvider:
    def __init__(self):
        self._client: OpenAI | None = None
        self._embed_client: OpenAI | None = None

    def _get_client(self) -> OpenAI:
        if self._client is None:
            if settings.LLM_PROVIDER == "deepseek":
                self._client = OpenAI(api_key=settings.DEEPSEEK_API_KEY, base_url=settings.DEEPSEEK_BASE_URL, timeout=settings.LLM_TIMEOUT_SEC)
            else:
                self._client = OpenAI(api_key=settings.MOONSHOT_API_KEY, base_url=settings.MOONSHOT_BASE_URL, timeout=settings.LLM_TIMEOUT_SEC)
        return self._client

    def _get_embed_client(self) -> OpenAI:
        if self._embed_client is None:
            self._embed_client = OpenAI(api_key=settings.MOONSHOT_API_KEY, base_url=settings.MOONSHOT_BASE_URL, timeout=settings.LLM_TIMEOUT_SEC)
        return self._embed_client

    @property
    def model(self) -> str:
        return settings.DEEPSEEK_MODEL if settings.LLM_PROVIDER == "deepseek" else settings.MOONSHOT_MODEL

    @property
    def embed_model(self) -> str:
        return settings.MOONSHOT_EMBED_MODEL

    def chat_json(self, system_prompt: str, user_prompt: str, temperature: float | None = None, max_retries: int = 3) -> dict[str, Any]:
        temp = temperature if temperature is not None else settings.LLM_TEMPERATURE
        last_error: Exception | None = None

        for attempt in range(max_retries):
            try:
                response = self._get_client().chat.completions.create(
                    model=self.model,
                    messages=[{"role": "system", "content": system_prompt}, {"role": "user", "content": user_prompt}],
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

    def embed_text(self, text: str) -> list[float]:
        try:
            resp = self._get_embed_client().embeddings.create(model=self.embed_model, input=text)
            return resp.data[0].embedding
        except openai.APIError as e:
            raise LLMError(f"Embedding API 错误: {e}") from e

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        try:
            resp = self._get_embed_client().embeddings.create(model=self.embed_model, input=texts)
            sorted_data = sorted(resp.data, key=lambda x: x.index)
            return [d.embedding for d in sorted_data]
        except openai.APIError as e:
            raise LLMError(f"Embedding API 错误: {e}") from e


class LLMError(Exception):
    pass


class LLMTimeoutError(LLMError):
    pass


_provider: LLMProvider | None = None


def get_llm() -> LLMProvider:
    global _provider
    if _provider is None:
        _provider = LLMProvider()
    return _provider
