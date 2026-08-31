from __future__ import annotations

from collections.abc import Callable
from threading import RLock
from typing import Any

from app.config import Settings, settings
from app.rag.models import RagRuntimeStatus


EmbeddingsFactory = Callable[[], Any]
ChromaClientFactory = Callable[[], Any]
VectorStoreFactory = Callable[[Any, Any, str], Any]


class RagRuntime:
    """Lazily creates optional local RAG dependencies only when they are requested."""

    def __init__(
        self,
        runtime_settings: Settings | Any = settings,
        *,
        embeddings_factory: EmbeddingsFactory | None = None,
        chroma_client_factory: ChromaClientFactory | None = None,
        vector_store_factory: VectorStoreFactory | None = None,
    ) -> None:
        self._settings = runtime_settings
        self._embeddings_factory = embeddings_factory or self._build_embeddings
        self._chroma_client_factory = chroma_client_factory or self._build_chroma_client
        self._vector_store_factory = vector_store_factory or self._build_vector_store
        self._embeddings: Any | None = None
        self._chroma_client: Any | None = None
        self._vector_store: Any | None = None
        self._lock = RLock()

    @property
    def is_initialized(self) -> bool:
        return self._chroma_client is not None or self._vector_store is not None

    @property
    def settings(self) -> Settings | Any:
        return self._settings

    @property
    def is_embedding_initialized(self) -> bool:
        return self._embeddings is not None

    def status(self) -> RagRuntimeStatus:
        return RagRuntimeStatus(
            enabled=self._settings.rag_enabled,
            initialized=self.is_initialized,
            embedding_initialized=self.is_embedding_initialized,
        )

    def get_embeddings(self) -> Any:
        self._require_enabled()
        if self._embeddings is None:
            with self._lock:
                if self._embeddings is None:
                    self._embeddings = self._embeddings_factory()
        return self._embeddings

    def get_chroma_client(self) -> Any:
        self._require_enabled()
        if self._chroma_client is None:
            with self._lock:
                if self._chroma_client is None:
                    self._chroma_client = self._chroma_client_factory()
        return self._chroma_client

    def get_vector_store(self) -> Any:
        self._require_enabled()
        if self._vector_store is None:
            with self._lock:
                if self._vector_store is None:
                    self._vector_store = self._vector_store_factory(
                        self.get_chroma_client(),
                        self.get_embeddings(),
                        self._settings.rag_collection,
                    )
        return self._vector_store

    def _require_enabled(self) -> None:
        if not self._settings.rag_enabled:
            raise RuntimeError("RAG is disabled. Set RAG_ENABLED=true before using the RAG runtime.")

    def _build_embeddings(self) -> Any:
        from langchain_huggingface import HuggingFaceEmbeddings

        return HuggingFaceEmbeddings(
            model_name=self._settings.embedding_model,
            model_kwargs={"device": self._settings.embedding_device},
            encode_kwargs={"normalize_embeddings": True, "prompt": "passage: "},
            query_encode_kwargs={"normalize_embeddings": True, "prompt": "query: "},
        )

    def _build_chroma_client(self) -> Any:
        import chromadb

        return chromadb.PersistentClient(path=self._settings.rag_chroma_dir)

    @staticmethod
    def _build_vector_store(chroma_client: Any, embeddings: Any, collection_name: str) -> Any:
        from langchain_chroma import Chroma

        return Chroma(
            client=chroma_client,
            collection_name=collection_name,
            embedding_function=embeddings,
        )


rag_runtime = RagRuntime()
