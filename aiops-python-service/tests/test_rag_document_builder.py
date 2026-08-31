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
        "updatedAt": "",
    }
