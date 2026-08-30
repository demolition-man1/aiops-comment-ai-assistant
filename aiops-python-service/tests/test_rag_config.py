import os
import unittest
from unittest.mock import patch

from app.config import Settings


class RagConfigTests(unittest.TestCase):
    def test_rag_defaults_keep_the_feature_disabled(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            settings = Settings()

        self.assertFalse(settings.rag_enabled)
        self.assertEqual(settings.rag_collection, "aiops_knowledge_v1")
        self.assertEqual(settings.rag_top_k, 4)
        self.assertEqual(settings.rag_min_relevance_score, 0.35)
        self.assertEqual(settings.rag_max_context_chars, 6000)
        self.assertEqual(settings.rag_chroma_dir, "./data/chroma")
        self.assertEqual(settings.embedding_model, "intfloat/multilingual-e5-small")
        self.assertEqual(settings.embedding_device, "cpu")

    def test_rag_accepts_supported_runtime_configuration(self) -> None:
        with patch.dict(
            os.environ,
            {
                "RAG_ENABLED": "true",
                "RAG_COLLECTION": "merchant_knowledge_2026",
                "RAG_TOP_K": "8",
                "RAG_MIN_RELEVANCE_SCORE": "0.7",
                "RAG_MAX_CONTEXT_CHARS": "9000",
                "RAG_CHROMA_DIR": "./runtime/chroma",
                "EMBEDDING_MODEL": "intfloat/multilingual-e5-small",
                "EMBEDDING_DEVICE": "cuda",
            },
            clear=True,
        ):
            settings = Settings()

        self.assertTrue(settings.rag_enabled)
        self.assertEqual(settings.rag_collection, "merchant_knowledge_2026")
        self.assertEqual(settings.rag_top_k, 8)
        self.assertEqual(settings.rag_min_relevance_score, 0.7)
        self.assertEqual(settings.rag_max_context_chars, 9000)
        self.assertEqual(settings.rag_chroma_dir, "./runtime/chroma")
        self.assertEqual(settings.embedding_device, "cuda")

    def test_rag_rejects_invalid_collection_name(self) -> None:
        with patch.dict(os.environ, {"RAG_COLLECTION": "Invalid Collection"}, clear=False):
            with self.assertRaisesRegex(ValueError, "RAG_COLLECTION"):
                Settings()

    def test_rag_rejects_out_of_range_values_and_unknown_device(self) -> None:
        cases = (
            ("RAG_TOP_K", "0"),
            ("RAG_MIN_RELEVANCE_SCORE", "1.1"),
            ("RAG_MAX_CONTEXT_CHARS", "100"),
            ("EMBEDDING_DEVICE", "accelerator"),
        )

        for name, value in cases:
            with self.subTest(name=name):
                with patch.dict(os.environ, {name: value}, clear=False):
                    with self.assertRaisesRegex(ValueError, name):
                        Settings()


if __name__ == "__main__":
    unittest.main()
