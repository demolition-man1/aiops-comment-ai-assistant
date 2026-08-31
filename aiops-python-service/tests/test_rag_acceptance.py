from contextlib import contextmanager
from types import SimpleNamespace

from app.ai.results import AiInvocationResult
from app.ai.schemas import NegativeReplyOutput
from app.rag.index_service import RagIndexService
from app.rag.knowledge_retriever import KnowledgeRetriever
from app.rag.reply_service import RagReplyService
from app.rag.runtime import RagRuntime


class SemanticFixtureEmbeddings:
    """A deterministic multilingual fixture for exercising local Chroma persistence."""

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)

    @staticmethod
    def _embed(text: str) -> list[float]:
        normalized = text.lower()
        logistics = any(term in normalized for term in ("物流", "配送", "delivery", "shipping", "entrega", "atraso"))
        quality = any(term in normalized for term in ("质量", "缺陷", "quality", "defect", "broken"))
        service = any(term in normalized for term in ("客服", "服务", "service", "atendimento", "suporte"))
        return [0.8 if logistics else 0.2, 0.8 if quality else 0.2, 0.8 if service else 0.2]


@contextmanager
def _connection():
    yield object()


def _runtime(tmp_path):
    settings = SimpleNamespace(
        rag_enabled=True,
        rag_collection="aiops_knowledge_v1",
        rag_chroma_dir=str(tmp_path / "chroma"),
        embedding_model="fixture-embeddings",
        embedding_device="cpu",
        rag_top_k=3,
        rag_min_relevance_score=0.9,
        rag_max_context_chars=6000,
    )
    return RagRuntime(settings, embeddings_factory=SemanticFixtureEmbeddings)


def _knowledge_rows() -> list[dict[str, object]]:
    return [
        {
            "id": 14,
            "problem_type": "logistics",
            "category_name_en": "health_beauty",
            "solution_title": "物流配送进度核查",
            "solution_content": "核对物流配送进度，并主动说明预计送达时间。",
            "keywords": "物流,配送",
            "source_type": "merchant",
            "priority": 10,
            "update_time": "2026-08-30T10:00:00",
        },
        {
            "id": 15,
            "problem_type": "quality",
            "category_name_en": "electronics",
            "solution_title": "Quality defect resolution",
            "solution_content": "Verify the quality defect and offer an appropriate replacement path.",
            "keywords": "quality,defect",
            "source_type": "merchant",
            "priority": 9,
            "update_time": "2026-08-30T10:00:00",
        },
        {
            "id": 16,
            "problem_type": "service",
            "category_name_en": "home",
            "solution_title": "Acompanhamento de atendimento",
            "solution_content": "Retome o atendimento e informe ao cliente o proximo passo do suporte.",
            "keywords": "atendimento,suporte",
            "source_type": "merchant",
            "priority": 8,
            "update_time": "2026-08-30T10:00:00",
        },
    ]


def _index_service(runtime: RagRuntime) -> RagIndexService:
    return RagIndexService(
        runtime=runtime,
        connection_factory=_connection,
        problem_solution_loader=lambda _connection: _knowledge_rows(),
        historical_reply_loader=lambda _connection: [],
    )


def test_recreated_runtime_keeps_multilingual_knowledge_retrieval(tmp_path) -> None:
    first_runtime = _runtime(tmp_path)
    first_status = _index_service(first_runtime).reindex()

    recreated_runtime = _runtime(tmp_path)
    recreated_index = _index_service(recreated_runtime)
    retriever = KnowledgeRetriever(runtime=recreated_runtime, index_service=recreated_index)

    chinese = retriever.retrieve(
        review_text="物流配送太慢了。",
        review_score=1,
        problem_type=None,
        language="zh-CN",
    )
    english = retriever.retrieve(
        review_text="The product has a quality defect.",
        review_score=1,
        problem_type=None,
        language="en-US",
    )
    portuguese = retriever.retrieve(
        review_text="O atendimento do suporte demorou muito.",
        review_score=1,
        problem_type=None,
        language="pt-BR",
    )

    assert first_status.document_count == 3
    assert recreated_index.status().document_count == 3
    assert [reference.source_id for reference in chinese.references] == [14]
    assert [reference.source_id for reference in english.references] == [15]
    assert [reference.source_id for reference in portuguese.references] == [16]


def test_unavailable_retrieval_returns_a_structured_non_rag_reply() -> None:
    class UnavailableRetriever:
        def retrieve(self, **_kwargs):
            raise RuntimeError("fixture Chroma is unavailable")

    class StructuredReplyChain:
        def __init__(self) -> None:
            self.reference_context = "not-called"

        def generate(self, _prompt: str, *, reference_context: str | None = None) -> AiInvocationResult[NegativeReplyOutput]:
            self.reference_context = reference_context
            return AiInvocationResult(
                value=NegativeReplyOutput.model_validate({"replyContent": "We will check the delivery status."}),
                model_name="fixture-model",
                input_tokens=4,
                output_tokens=7,
                total_tokens=11,
                token_usage_estimated=False,
            )

    chain = StructuredReplyChain()
    result = RagReplyService(retriever=UnavailableRetriever(), reply_chain=chain).generate(
        request={
            "commentContent": "The package was late.",
            "reviewScore": 1,
            "problemType": "logistics",
            "language": "en-US",
        },
        rendered_prompt="Write a helpful reply.",
    )

    assert result.invocation.value.reply_content == "We will check the delivery status."
    assert result.rag_used is False
    assert result.references == []
    assert chain.reference_context is None
