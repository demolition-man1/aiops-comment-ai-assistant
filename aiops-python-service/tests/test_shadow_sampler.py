from app.utils.shadow_sampler import select_shadow_sample


def comment(comment_id: int, sentiment: str, text: str = "review text") -> dict[str, object]:
    return {"id": comment_id, "sentiment": sentiment, "clean_content": text}


def test_select_shadow_sample_is_deterministic_and_stratified() -> None:
    comments = [
        comment(14, "negative"),
        comment(13, "positive"),
        comment(11, "negative"),
        comment(12, "neutral"),
    ]

    sample = select_shadow_sample(comments, sample_size=3, sample_seed=20260829)
    reordered_sample = select_shadow_sample(list(reversed(comments)), sample_size=3, sample_seed=20260829)

    assert [row["sentiment"] for row in sample] == ["negative", "neutral", "positive"]
    assert [row["id"] for row in sample] == [row["id"] for row in reordered_sample]


def test_select_shadow_sample_uses_sha256_order_when_one_group_has_extra_rows() -> None:
    comments = [comment(3, "negative"), comment(2, "negative"), comment(1, "negative")]

    sample = select_shadow_sample(comments, sample_size=2, sample_seed=20260829)

    assert [row["id"] for row in sample] == [1, 3]


def test_select_shadow_sample_excludes_comments_without_text() -> None:
    comments = [
        comment(1, "negative", ""),
        {"id": 2, "sentiment": "neutral", "clean_content": "  ", "review_content": "\t"},
        {"id": 3, "sentiment": "positive", "clean_content": "", "review_content": "Helpful seller"},
    ]

    sample = select_shadow_sample(comments, sample_size=10, sample_seed=20260829)

    assert [row["id"] for row in sample] == [3]


def test_select_shadow_sample_caps_at_available_rows_when_groups_are_short() -> None:
    comments = [comment(1, "negative"), comment(2, "neutral")]

    sample = select_shadow_sample(comments, sample_size=10, sample_seed=20260829)

    assert [row["id"] for row in sample] == [1, 2]
