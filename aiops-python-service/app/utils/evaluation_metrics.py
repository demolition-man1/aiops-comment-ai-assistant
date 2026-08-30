from __future__ import annotations

from collections.abc import Iterable
from typing import Any


_QUALITY_KEYS = ("sentimentAccuracy", "problemMicroF1", "problemMacroF1")


def evaluate_comment_predictions(rows: list[dict[str, Any]]) -> dict[str, Any]:
    annotated_rows = [row for row in rows if _text_value(row.get("manualSentiment"))]
    attempted_rows = [row for row in rows if _text_value(row.get("callStatus")) != "pending"]
    successful_rows = [row for row in attempted_rows if _text_value(row.get("callStatus")) == "success"]

    counts = {
        "sampleCount": len(rows),
        "annotatedCount": len(annotated_rows),
        "attemptedCallCount": len(attempted_rows),
        "successfulCallCount": len(successful_rows),
        "failedCallCount": len([row for row in attempted_rows if _text_value(row.get("callStatus")) == "failed"]),
    }
    validity = {
        "annotationCoverage": _ratio(len(annotated_rows), len(rows)),
        "jsonValidRate": _ratio(_truthy_count(attempted_rows, "jsonValid"), len(attempted_rows)),
        "evidenceValidRate": _ratio(_truthy_count(successful_rows, "evidenceValid"), len(successful_rows)),
        "callSuccessRate": _ratio(len(successful_rows), len(attempted_rows)),
    }
    usage = {
        "totalTokens": sum(_int_value(row.get("tokenUsage")) for row in attempted_rows),
        "estimatedTokenRowCount": _truthy_count(attempted_rows, "tokenUsageEstimated"),
        "averageLatencyMs": _average([_int_value(row.get("latencyMs")) for row in attempted_rows]),
    }
    if not annotated_rows:
        empty_metrics = dict.fromkeys(_QUALITY_KEYS)
        return {
            "qualityReady": False,
            "counts": counts,
            "validity": validity,
            "usage": usage,
            "rule": empty_metrics,
            "ai": empty_metrics.copy(),
            "delta": empty_metrics.copy(),
        }

    rule = _quality_metrics(annotated_rows, "rule")
    ai = _quality_metrics(annotated_rows, "ai")
    return {
        "qualityReady": True,
        "counts": counts,
        "validity": validity,
        "usage": usage,
        "rule": rule,
        "ai": ai,
        "delta": {key: ai[key] - rule[key] for key in _QUALITY_KEYS},
    }


def _quality_metrics(rows: list[dict[str, Any]], source: str) -> dict[str, float]:
    sentiment_matches = sum(
        1
        for row in rows
        if _text_value(row.get("manualSentiment")) == _sentiment_for_source(row, source)
    )
    tp = fp = fn = 0
    labels: set[str] = set()
    label_counts: dict[str, list[int]] = {}
    for row in rows:
        expected = _labels(row.get("manualProblemTypes"))
        actual = _problem_labels_for_source(row, source)
        labels.update(expected, actual)
        tp += len(expected & actual)
        fp += len(actual - expected)
        fn += len(expected - actual)
        for label in expected | actual:
            counts = label_counts.setdefault(label, [0, 0, 0])
            if label in expected and label in actual:
                counts[0] += 1
            elif label in actual:
                counts[1] += 1
            else:
                counts[2] += 1

    macro_scores = [_f1(*label_counts[label]) for label in labels]
    return {
        "sentimentAccuracy": _ratio(sentiment_matches, len(rows)),
        "problemMicroF1": _f1(tp, fp, fn),
        "problemMacroF1": _average(macro_scores),
    }


def _sentiment_for_source(row: dict[str, Any], source: str) -> str | None:
    if source == "ai" and _text_value(row.get("callStatus")) != "success":
        return None
    key = "ruleSentiment" if source == "rule" else "aiSentiment"
    return _text_value(row.get(key))


def _problem_labels_for_source(row: dict[str, Any], source: str) -> set[str]:
    if source == "rule":
        return _labels([row.get("ruleProblemType")])
    if _text_value(row.get("callStatus")) != "success":
        return set()
    return _labels(row.get("aiProblems"))


def _labels(value: Any) -> set[str]:
    values: Iterable[Any]
    if isinstance(value, (str, dict)) or value is None:
        values = [value]
    elif isinstance(value, Iterable):
        values = value
    else:
        values = [value]
    labels: set[str] = set()
    for item in values:
        if isinstance(item, dict):
            item = item.get("type")
        normalized = _text_value(item)
        if normalized:
            labels.add(normalized)
    return labels


def _f1(tp: int, fp: int, fn: int) -> float:
    denominator = 2 * tp + fp + fn
    return 0.0 if denominator == 0 else (2 * tp) / denominator


def _ratio(numerator: int, denominator: int) -> float:
    return 0.0 if denominator == 0 else numerator / denominator


def _average(values: list[int | float]) -> float:
    return 0.0 if not values else sum(values) / len(values)


def _truthy_count(rows: list[dict[str, Any]], key: str) -> int:
    return sum(1 for row in rows if bool(row.get(key)))


def _text_value(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _int_value(value: Any) -> int:
    try:
        return int(value or 0)
    except (TypeError, ValueError):
        return 0
