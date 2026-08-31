from app.rag.evaluation import evaluate_report_retrieval


def test_report_retrieval_evaluation_is_deterministic_and_uses_only_local_labels() -> None:
    result = evaluate_report_retrieval(
        [
            {
                "expectedReferences": [
                    {"sourceType": "review_evidence", "sourceId": 11},
                    {"sourceType": "problem_solution", "sourceId": 7},
                ],
                "retrievedReferences": [
                    {"sourceType": "review_evidence", "sourceId": 11},
                    {"sourceType": "problem_solution", "sourceId": 7},
                ],
                "jsonValid": True,
                "grounded": True,
                "replyRelevant": True,
                "unsupportedPolicy": False,
            },
            {
                "expectedReferences": [{"sourceType": "review_evidence", "sourceId": 12}],
                "retrievedReferences": [{"sourceType": "problem_solution", "sourceId": 8}],
                "jsonValid": True,
                "grounded": False,
                "replyRelevant": False,
                "unsupportedPolicy": True,
            },
        ]
    )

    assert result["counts"] == {
        "caseCount": 2,
        "expectedReferenceCount": 3,
        "retrievedReferenceCount": 3,
        "groundedLabelCount": 2,
        "replyRelevanceLabelCount": 2,
        "unsupportedPolicyLabelCount": 2,
    }
    assert result["retrieval"] == {"recallAt5": 2 / 3}
    assert result["validity"] == {"jsonValidRate": 1.0}
    assert result["quality"] == {
        "groundednessRate": 0.5,
        "replyRelevanceRate": 0.5,
        "unsupportedPolicyRate": 0.5,
    }


def test_report_retrieval_evaluation_ignores_missing_human_labels() -> None:
    result = evaluate_report_retrieval(
        [{"expectedReferences": [], "retrievedReferences": [], "jsonValid": False}]
    )

    assert result["counts"]["caseCount"] == 1
    assert result["retrieval"] == {"recallAt5": 0.0}
    assert result["validity"] == {"jsonValidRate": 0.0}
    assert result["quality"] == {
        "groundednessRate": None,
        "replyRelevanceRate": None,
        "unsupportedPolicyRate": None,
    }
