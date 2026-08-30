from app.services.comment_analysis_service import build_problem_types, effective_problem_type


def test_hybrid_problem_type_keeps_manual_label_above_an_active_ai_decision() -> None:
    comment = {
        "manual_problem_type": "service",
        "problem_type": "other",
        "active_ai_problem_type": "logistics",
    }

    assert effective_problem_type(comment, "hybrid") == "service"


def test_rule_mode_ignores_an_active_ai_decision() -> None:
    comment = {
        "manual_problem_type": None,
        "problem_type": "other",
        "active_ai_problem_type": "logistics",
    }

    assert effective_problem_type(comment, "rule") == "other"


def test_hybrid_mode_uses_only_an_active_ai_decision_before_the_rule_label() -> None:
    comment = {
        "manual_problem_type": "",
        "problem_type": "other",
        "active_ai_problem_type": "logistics",
    }

    assert effective_problem_type(comment, "hybrid") == "logistics"


def test_problem_aggregation_uses_the_configured_mode_for_active_ai_decisions() -> None:
    comment = {
        "is_negative": 1,
        "manual_problem_type": None,
        "problem_type": "other",
        "active_ai_problem_type": "logistics",
    }

    assert build_problem_types([comment], "rule") == ["other"]
    assert build_problem_types([comment], "hybrid") == ["logistics"]
