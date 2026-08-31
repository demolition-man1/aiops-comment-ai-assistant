from datetime import datetime

from app.rag.document_builder import build_knowledge_documents


def test_problem_solution_documents_have_stable_ids_scalar_metadata_and_unicode_content() -> None:
    documents = build_knowledge_documents(
        problem_solutions=[
            {
                "id": 14,
                "problem_type": "logistics",
                "category_name_en": "health_beauty",
                "solution_title": "核对配送时效",
                "solution_content": "联系物流商并同步预计处理时间。",
                "keywords": "delivery,entrega",
                "source_type": "merchant",
                "priority": 10,
                "update_time": datetime(2026, 8, 30, 10, 0, 0),
            }
        ],
        historical_replies=[],
    )

    assert len(documents) == 1
    document = documents[0]
    assert document.id == "problem_solution:14"
    assert "核对配送时效" in document.page_content
    assert "联系物流商" in document.page_content
    assert document.metadata == {
        "sourceType": "problem_solution",
        "sourceId": 14,
        "problemType": "logistics",
        "categoryNameEn": "health_beauty",
        "recordSourceType": "merchant",
        "title": "核对配送时效",
        "priority": 10,
        "updatedAt": "2026-08-30T10:00:00",
    }
    assert all(value is not None and isinstance(value, (str, int, float, bool)) for value in document.metadata.values())


def test_historical_reply_documents_fill_missing_optional_values_and_keep_deterministic_ids() -> None:
    documents = build_knowledge_documents(
        problem_solutions=[],
        historical_replies=[
            {
                "id": 25,
                "problem_type": None,
                "comment_content": "Produto chegou atrasado",
                "reply_content": "Pedimos desculpas e vamos acompanhar a entrega.",
                "effect_tag": None,
                "favorite_flag": None,
                "use_count": None,
                "update_time": None,
            }
        ],
    )

    assert len(documents) == 1
    document = documents[0]
    assert document.id == "historical_reply:25"
    assert "Produto chegou atrasado" in document.page_content
    assert "Pedimos desculpas" in document.page_content
    assert document.metadata == {
        "sourceType": "historical_reply",
        "sourceId": 25,
        "problemType": "",
        "title": "已确认有效的历史回复",
        "effectTag": "",
        "favoriteFlag": 0,
        "useCount": 0,
        "eligibilityReason": "unknown",
        "retrievalVersion": "historical-reply-v1",
        "updatedAt": "",
    }


def test_historical_reply_documents_expose_the_selected_eligibility_reason() -> None:
    documents = build_knowledge_documents(
        problem_solutions=[],
        historical_replies=[
            {
                "id": 26,
                "problem_type": "logistics",
                "comment_content": "Where is my order?",
                "reply_content": "We will confirm the shipping status today.",
                "effect_tag": "no_feedback",
                "favorite_flag": 0,
                "use_count": 3,
                "update_time": None,
            }
        ],
    )

    assert documents[0].metadata["useCount"] == 3
    assert documents[0].metadata["eligibilityReason"] == "repeat_use"
    assert documents[0].metadata["retrievalVersion"] == "historical-reply-v1"


def test_review_evidence_documents_prefer_clean_text_and_keep_target_metadata() -> None:
    documents = build_knowledge_documents(
        problem_solutions=[],
        historical_replies=[],
        review_evidence=[
            {
                "id": 36,
                "product_id": "product-1",
                "seller_id": "seller-1",
                "review_score": 1,
                "sentiment": "negative",
                "problem_type": "logistics",
                "review_time": datetime(2026, 8, 31, 9, 30, 0),
                "clean_content": "Delivery is still delayed.",
                "review_content": "The parcel was late.",
            }
        ],
    )

    assert len(documents) == 1
    document = documents[0]
    assert document.id == "review_evidence:36"
    assert "Delivery is still delayed." in document.page_content
    assert "The parcel was late." not in document.page_content
    assert document.metadata == {
        "sourceType": "review_evidence",
        "sourceId": 36,
        "productId": "product-1",
        "sellerId": "seller-1",
        "reviewScore": 1,
        "sentiment": "negative",
        "problemType": "logistics",
        "reviewTime": "2026-08-31T09:30:00",
        "title": "Review evidence #36",
        "updatedAt": "",
    }


def test_review_evidence_documents_skip_blank_and_placeholder_text() -> None:
    documents = build_knowledge_documents(
        problem_solutions=[],
        historical_replies=[],
        review_evidence=[
            {"id": 37, "clean_content": "  nan ", "review_content": "none"},
            {"id": 38, "clean_content": "", "review_content": "   "},
        ],
    )

    assert documents == []
