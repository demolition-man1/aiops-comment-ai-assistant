from types import SimpleNamespace
from unittest.mock import patch

import pytest

from app.rag.runtime import RagRuntime


def _settings(*, enabled: bool = True) -> SimpleNamespace:
    return SimpleNamespace(
        rag_enabled=enabled,
        rag_collection="aiops_knowledge_v1",
        rag_chroma_dir="./data/chroma",
        embedding_model="intfloat/multilingual-e5-small",
        embedding_device="cpu",
    )


def test_disabled_rag_never_initializes_factories() -> None:
    calls: list[str] = []

    runtime = RagRuntime(
        _settings(enabled=False),
        embeddings_factory=lambda: calls.append("embeddings"),
        chroma_client_factory=lambda: calls.append("client"),
    )

    assert not runtime.is_initialized
    assert not runtime.is_embedding_initialized
    with pytest.raises(RuntimeError, match="RAG is disabled"):
        runtime.get_vector_store()
    assert calls == []


def test_chroma_client_is_lazy_and_cached_without_initializing_embeddings() -> None:
    calls: list[str] = []
    client = object()

    runtime = RagRuntime(
        _settings(),
        embeddings_factory=lambda: calls.append("embeddings"),
        chroma_client_factory=lambda: calls.append("client") or client,
    )

    assert not runtime.is_initialized
    assert runtime.get_chroma_client() is client
    assert runtime.get_chroma_client() is client
    assert runtime.is_initialized
    assert not runtime.is_embedding_initialized
    assert calls == ["client"]


def test_embeddings_are_lazy_and_cached() -> None:
    calls: list[str] = []
    embeddings = object()

    runtime = RagRuntime(
        _settings(),
        embeddings_factory=lambda: calls.append("embeddings") or embeddings,
        chroma_client_factory=lambda: object(),
    )

    assert runtime.get_embeddings() is embeddings
    assert runtime.get_embeddings() is embeddings
    assert runtime.is_embedding_initialized
    assert calls == ["embeddings"]


def test_default_embeddings_use_e5_document_and_query_prompts() -> None:
    embeddings = object()
    runtime = RagRuntime(_settings())

    with patch("langchain_huggingface.HuggingFaceEmbeddings", return_value=embeddings) as constructor:
        assert runtime.get_embeddings() is embeddings

    constructor.assert_called_once_with(
        model_name="intfloat/multilingual-e5-small",
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True, "prompt": "passage: "},
        query_encode_kwargs={"normalize_embeddings": True, "prompt": "query: "},
    )


def test_vector_store_uses_injected_dependencies_and_configuration() -> None:
    client = object()
    embeddings = object()
    calls: list[tuple[object, object, str]] = []
    vector_store = object()

    def vector_store_factory(actual_client: object, actual_embeddings: object, collection_name: str) -> object:
        calls.append((actual_client, actual_embeddings, collection_name))
        return vector_store

    runtime = RagRuntime(
        _settings(),
        embeddings_factory=lambda: embeddings,
        chroma_client_factory=lambda: client,
        vector_store_factory=vector_store_factory,
    )

    assert runtime.get_vector_store() is vector_store
    assert runtime.get_vector_store() is vector_store
    assert calls == [(client, embeddings, "aiops_knowledge_v1")]
