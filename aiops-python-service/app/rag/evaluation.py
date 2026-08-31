from __future__ import annotations

from typing import Any


def evaluate_report_retrieval(cases: list[dict[str, Any]]) -> dict[str, Any]:
    """Evaluate report retrieval using only locally supplied, human-reviewed labels."""

    expected_sets = [_reference_keys(case.get("expectedReferences")) for case in cases]
    retrieved_sets = [_reference_keys(case.get("retrievedReferences")) for case in cases]
    expected_count = sum(len(items) for items in expected_sets)
    matched_count = sum(
        len(expected & retrieved)
        for expected, retrieved in zip(expected_sets, retrieved_sets, strict=True)
    )
    grounded = _labelled_values(cases, "grounded")
    relevant = _labelled_values(cases, "replyRelevant")
    unsupported_policy = _labelled_values(cases, "unsupportedPolicy")

    return {
        "counts": {
            "caseCount": len(cases),
            "expectedReferenceCount": expected_count,
            "retrievedReferenceCount": sum(len(items) for items in retrieved_sets),
            "groundedLabelCount": len(grounded),
            "replyRelevanceLabelCount": len(relevant),
            "unsupportedPolicyLabelCount": len(unsupported_policy),
        },
        "retrieval": {"recallAt5": _ratio(matched_count, expected_count)},
        "validity": {"jsonValidRate": _ratio(_truthy_count(cases, "jsonValid"), len(cases))},
        "quality": {
            "groundednessRate": _boolean_rate(grounded),
            "replyRelevanceRate": _boolean_rate(relevant),
            "unsupportedPolicyRate": _boolean_rate(unsupported_policy),
        },
    }


def _reference_keys(value: Any) -> set[tuple[str, int]]:
    if not isinstance(value, list):
        return set()
    keys: set[tuple[str, int]] = set()
    for item in value:
        if not isinstance(item, dict):
            continue
        source_type = str(item.get("sourceType") or "").strip()
        try:
            source_id = int(item.get("sourceId"))
        except (TypeError, ValueError):
            continue
        if source_type and source_id > 0:
            keys.add((source_type, source_id))
    return keys


def _labelled_values(cases: list[dict[str, Any]], key: str) -> list[bool]:
    return [case[key] for case in cases if isinstance(case.get(key), bool)]


def _truthy_count(cases: list[dict[str, Any]], key: str) -> int:
    return sum(1 for case in cases if case.get(key) is True)


def _boolean_rate(values: list[bool]) -> float | None:
    if not values:
        return None
    return sum(values) / len(values)


def _ratio(numerator: int, denominator: int) -> float:
    return 0.0 if denominator == 0 else numerator / denominator
