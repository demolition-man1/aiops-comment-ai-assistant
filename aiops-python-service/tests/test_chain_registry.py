from app.ai.registry import AiChainRegistry


def test_chain_registry_creates_each_supported_merchant_chain() -> None:
    registry = AiChainRegistry()

    assert registry.create("operation_report").__class__.__name__ == "ReportChain"
    assert registry.create("negative_reply").__class__.__name__ == "NegativeReplyChain"
    assert registry.create("product_compare").__class__.__name__ == "ProductCompareChain"
    assert registry.create("content_generation").__class__.__name__ == "ContentGenerationChain"
    assert registry.create("comment_translation").__class__.__name__ == "CommentTranslationChain"
    assert registry.create("comment_analysis").__class__.__name__ == "CommentAnalysisChain"
