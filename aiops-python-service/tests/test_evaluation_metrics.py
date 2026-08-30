import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.utils.evaluation_metrics import evaluate_comment_predictions


def test_evaluation_uses_only_annotations_as_ground_truth_and_reports_hand_calculated_metrics() -> None:
    result = evaluate_comment_predictions(
        [
            {
                "manualSentiment": "negative",
                "manualProblemTypes": ["delivery"],
                "ruleSentiment": "negative",
                "ruleProblemType": "delivery",
                "aiSentiment": "negative",
                "aiProblems": ["delivery"],
                "callStatus": "success",
                "jsonValid": True,
                "evidenceValid": True,
                "tokenUsage": 100,
                "tokenUsageEstimated": False,
                "latencyMs": 40,
            },
            {
                "manualSentiment": "negative",
                "manualProblemTypes": ["quality"],
                "ruleSentiment": "neutral",
                "ruleProblemType": None,
                "aiSentiment": "negative",
                "aiProblems": ["delivery"],
                "callStatus": "success",
                "jsonValid": True,
                "evidenceValid": False,
                "tokenUsage": 120,
                "tokenUsageEstimated": True,
                "latencyMs": 60,
            },
            {
                "manualSentiment": None,
                "manualProblemTypes": [],
                "ruleSentiment": "positive",
                "ruleProblemType": None,
                "aiSentiment": "positive",
                "aiProblems": [],
                "callStatus": "pending",
                "jsonValid": False,
                "evidenceValid": False,
                "tokenUsage": 0,
                "tokenUsageEstimated": False,
                "latencyMs": 0,
            },
        ]
    )

    assert result["qualityReady"] is True
    assert result["counts"] == {
        "sampleCount": 3,
        "annotatedCount": 2,
        "attemptedCallCount": 2,
        "successfulCallCount": 2,
        "failedCallCount": 0,
    }
    assert result["validity"] == {
        "annotationCoverage": 2 / 3,
        "jsonValidRate": 1.0,
        "evidenceValidRate": 0.5,
        "callSuccessRate": 1.0,
    }
    assert result["usage"] == {
        "totalTokens": 220,
        "estimatedTokenRowCount": 1,
        "averageLatencyMs": 50.0,
    }
    assert result["rule"] == {
        "sentimentAccuracy": 0.5,
        "problemMicroF1": 2 / 3,
        "problemMacroF1": 0.5,
    }
    assert result["ai"] == {
        "sentimentAccuracy": 1.0,
        "problemMicroF1": 0.5,
        "problemMacroF1": 1 / 3,
    }
    assert result["delta"] == pytest.approx(
        {
            "sentimentAccuracy": 0.5,
            "problemMicroF1": -1 / 6,
            "problemMacroF1": -1 / 6,
        }
    )


def test_evaluation_counts_failed_ai_output_as_incorrect_after_annotation() -> None:
    result = evaluate_comment_predictions(
        [
            {
                "manualSentiment": "negative",
                "manualProblemTypes": ["delivery"],
                "ruleSentiment": "negative",
                "ruleProblemType": "delivery",
                "aiSentiment": None,
                "aiProblems": [],
                "callStatus": "failed",
                "jsonValid": False,
                "evidenceValid": False,
                "tokenUsage": 0,
                "tokenUsageEstimated": False,
                "latencyMs": 25,
            }
        ]
    )

    assert result["counts"]["failedCallCount"] == 1
    assert result["ai"] == {
        "sentimentAccuracy": 0.0,
        "problemMicroF1": 0.0,
        "problemMacroF1": 0.0,
    }


def test_evaluation_returns_counts_but_no_quality_metrics_without_annotations() -> None:
    result = evaluate_comment_predictions(
        [
            {
                "manualSentiment": None,
                "manualProblemTypes": [],
                "ruleSentiment": "negative",
                "ruleProblemType": "delivery",
                "aiSentiment": "negative",
                "aiProblems": ["delivery"],
                "callStatus": "success",
                "jsonValid": True,
                "evidenceValid": True,
                "tokenUsage": 80,
                "tokenUsageEstimated": False,
                "latencyMs": 20,
            }
        ]
    )

    assert result["qualityReady"] is False
    assert result["counts"]["annotatedCount"] == 0
    assert result["rule"] == {
        "sentimentAccuracy": None,
        "problemMicroF1": None,
        "problemMacroF1": None,
    }
    assert result["ai"] == result["rule"]
    assert result["delta"] == result["rule"]


def test_internal_evaluation_endpoint_validates_rows_before_calculating_metrics() -> None:
    client = TestClient(app)

    response = client.post(
        "/internal/analysis/comments/evaluation",
        json={
            "rows": [
                {
                    "manualSentiment": "negative",
                    "manualProblemTypes": ["delivery"],
                    "ruleSentiment": "negative",
                    "ruleProblemType": "delivery",
                    "aiSentiment": "negative",
                    "aiProblems": ["delivery"],
                    "callStatus": "success",
                }
            ]
        },
    )
    invalid_response = client.post(
        "/internal/analysis/comments/evaluation",
        json={"rows": [{"manualSentiment": "mixed", "manualProblemTypes": []}]},
    )

    assert response.status_code == 200
    assert response.json()["success"] is True
    assert response.json()["evaluation"]["qualityReady"] is True
    assert invalid_response.status_code == 422
