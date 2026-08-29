from hashlib import sha256
from typing import Any


_SENTIMENT_ORDER = ("negative", "neutral", "positive")


def select_shadow_sample(
    comments: list[dict[str, Any]],
    sample_size: int,
    sample_seed: int,
) -> list[dict[str, Any]]:
    if sample_size < 0:
        raise ValueError("sample_size must be greater than or equal to 0")
    if sample_size == 0:
        return []

    groups: dict[str, list[dict[str, Any]]] = {sentiment: [] for sentiment in _SENTIMENT_ORDER}
    for comment in comments:
        if not _has_review_text(comment) or comment.get("id") is None:
            continue
        sentiment = str(comment.get("sentiment") or "neutral").strip().lower()
        groups[sentiment if sentiment in groups else "neutral"].append(comment)

    for sentiment in _SENTIMENT_ORDER:
        groups[sentiment].sort(key=lambda comment: _sample_key(comment, sample_seed))

    selected: list[dict[str, Any]] = []
    positions = {sentiment: 0 for sentiment in _SENTIMENT_ORDER}
    while len(selected) < sample_size:
        selected_in_round = False
        for sentiment in _SENTIMENT_ORDER:
            position = positions[sentiment]
            if position >= len(groups[sentiment]):
                continue
            selected.append(groups[sentiment][position])
            positions[sentiment] = position + 1
            selected_in_round = True
            if len(selected) == sample_size:
                break
        if not selected_in_round:
            break
    return selected


def _has_review_text(comment: dict[str, Any]) -> bool:
    for key in ("clean_content", "review_content"):
        value = comment.get(key)
        if isinstance(value, str) and value.strip():
            return True
    return False


def _sample_key(comment: dict[str, Any], sample_seed: int) -> tuple[str, str]:
    comment_id = str(comment["id"])
    digest = sha256(f"{sample_seed}:{comment_id}".encode("utf-8")).hexdigest()
    return digest, comment_id
