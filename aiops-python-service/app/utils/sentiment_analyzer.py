from decimal import Decimal


def sentiment_from_score(score: int | float | None) -> tuple[str, Decimal, int]:
    if score is None:
        return "neutral", Decimal("0.5000"), 0
    value = int(score)
    if value <= 2:
        return "negative", Decimal("0.2000"), 1
    if value == 3:
        return "neutral", Decimal("0.5000"), 0
    return "positive", Decimal("0.8500"), 0
