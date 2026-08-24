from collections import Counter
from datetime import datetime
from decimal import Decimal
import json
from typing import Any

from app.db import get_conn
from app.repositories import analysis_repository, comment_repository, task_repository
from app.utils.keyword_extractor import keyword_rank
from app.utils.problem_classifier import problem_distribution
from app.utils.topic_clusterer import topic_distribution


def _rounded_rate(part: int, total: int) -> Decimal:
    if total == 0:
        return Decimal("0.0000")
    return (Decimal(part) / Decimal(total)).quantize(Decimal("0.0001"))


def aggregate_custom_tags(comments: list[dict[str, Any]]) -> list[dict[str, int | str]]:
    counter: Counter[str] = Counter()
    for comment in comments:
        for tag in _parse_tags(comment.get("custom_tags")):
            counter[tag] += 1
    return [{"name": tag, "count": count} for tag, count in counter.most_common()]


def build_trend_distribution(comments: list[dict[str, Any]], granularity: str = "month") -> list[dict[str, float | int | str]]:
    if granularity not in {"day", "week", "month"}:
        granularity = "month"
    buckets: dict[str, dict[str, int]] = {}
    for comment in comments:
        review_time = _parse_datetime(comment.get("review_time"))
        if review_time is None:
            continue
        bucket = _time_bucket(review_time, granularity)
        item = buckets.setdefault(bucket, {"commentCount": 0, "negativeCount": 0, "scoreSum": 0})
        item["commentCount"] += 1
        item["negativeCount"] += 1 if int(comment.get("is_negative") or 0) == 1 else 0
        item["scoreSum"] += int(comment.get("review_score") or 0)

    trend: list[dict[str, float | int | str]] = []
    for bucket in sorted(buckets):
        item = buckets[bucket]
        comment_count = item["commentCount"]
        negative_count = item["negativeCount"]
        trend.append(
            {
                "timeBucket": bucket,
                "commentCount": comment_count,
                "negativeCount": negative_count,
                "negativeRate": round(negative_count / comment_count, 4) if comment_count else 0.0,
                "avgScore": round(item["scoreSum"] / comment_count, 2) if comment_count else 0.0,
            }
        )
    return trend


def build_problem_types(comments: list[dict[str, Any]]) -> list[str]:
    problem_types: list[str] = []
    for comment in comments:
        if not comment.get("is_negative"):
            continue
        problem_type = comment.get("manual_problem_type") or comment.get("problem_type")
        if problem_type:
            problem_types.append(str(problem_type))
    return problem_types


def merge_problem_and_topics(
    problems: list[dict[str, int | str]],
    topics: list[dict[str, int | str]],
) -> list[dict[str, int | str]]:
    counter: Counter[str] = Counter()
    for item in problems:
        name = str(item.get("name") or "").strip()
        if name:
            counter[name] += int(item.get("count") or 0)
    for item in topics:
        name = str(item.get("name") or "").strip()
        if name:
            counter[name] += int(item.get("count") or 0)
    return [{"name": name, "count": count} for name, count in counter.most_common()]


def _parse_tags(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    text = str(value).strip()
    if not text:
        return []
    try:
        parsed = json.loads(text)
        if isinstance(parsed, list):
            return [str(item).strip() for item in parsed if str(item).strip()]
    except json.JSONDecodeError:
        pass
    return [item.strip() for item in text.split(",") if item.strip()]


def _parse_datetime(value: Any) -> datetime | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    text = str(value).strip()
    if not text:
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(text[:19] if fmt.endswith("%S") else text[:10], fmt)
        except ValueError:
            continue
    try:
        return datetime.fromisoformat(text)
    except ValueError:
        return None


def _time_bucket(value: datetime, granularity: str) -> str:
    if granularity == "day":
        return value.strftime("%Y-%m-%d")
    if granularity == "week":
        year, week, _ = value.isocalendar()
        return f"{year}-W{week:02d}"
    return value.strftime("%Y-%m")


class CommentAnalysisService:
    def analyze_comments(self, request: dict[str, Any]) -> dict[str, Any]:
        task_id = int(request.get("taskId") or 0)
        target_type = request.get("targetType")
        target_id = request.get("targetId")
        if target_type not in {"product", "seller"}:
            raise ValueError("targetType must be product or seller")
        if not target_id:
            raise ValueError("targetId is required")
        trend_granularity = request.get("trendGranularity") or "month"

        with get_conn() as conn:
            task_repository.update_analysis_task(conn, task_id, "processing", 20)
            comments = comment_repository.fetch_comments(conn, target_type, str(target_id))

        total = len(comments)
        sentiment_counter = Counter(comment.get("sentiment") or "neutral" for comment in comments)
        problem_types = build_problem_types(comments)
        score_distribution = [
            {"name": str(score), "count": count}
            for score, count in sorted(Counter(comment.get("review_score") for comment in comments).items())
        ]
        all_texts = [comment.get("clean_content") or comment.get("review_content") or "" for comment in comments]
        negative_texts = [
            comment.get("clean_content") or comment.get("review_content") or ""
            for comment in comments
            if comment.get("is_negative")
        ]
        top_keywords = keyword_rank(all_texts, limit=20)
        negative_keywords = keyword_rank(negative_texts, limit=20)
        problems = problem_distribution(problem_types)
        topics = topic_distribution(negative_texts, limit=10)
        problems = merge_problem_and_topics(problems, topics)
        custom_tags = aggregate_custom_tags(comments)
        trend = build_trend_distribution(comments, str(trend_granularity))
        summary = self._summary(total, sentiment_counter, top_keywords, problems, topics)

        result = {
            "task_id": task_id,
            "target_type": target_type,
            "target_id": str(target_id),
            "total_count": total,
            "positive_count": int(sentiment_counter.get("positive", 0)),
            "neutral_count": int(sentiment_counter.get("neutral", 0)),
            "negative_count": int(sentiment_counter.get("negative", 0)),
            "positive_rate": _rounded_rate(int(sentiment_counter.get("positive", 0)), total),
            "negative_rate": _rounded_rate(int(sentiment_counter.get("negative", 0)), total),
            "top_keywords": json.dumps(top_keywords, ensure_ascii=False),
            "negative_keywords": json.dumps(negative_keywords, ensure_ascii=False),
            "problem_distribution": json.dumps(problems, ensure_ascii=False),
            "score_distribution": json.dumps(score_distribution, ensure_ascii=False),
            "custom_tag_distribution": json.dumps(custom_tags, ensure_ascii=False),
            "trend_distribution": json.dumps(trend, ensure_ascii=False),
            "summary": summary,
        }

        with get_conn() as conn:
            result_id = analysis_repository.insert_analysis_result(conn, result)
            task_repository.update_analysis_task(conn, task_id, "success", 100)

        return {
            "success": True,
            "analysisResultId": result_id,
            "totalCount": total,
            "message": "Comment analysis completed",
        }

    def _summary(
        self,
        total: int,
        sentiment_counter: Counter[str],
        top_keywords: list[dict[str, int | str]],
        problems: list[dict[str, int | str]],
        topics: list[dict[str, int | str]],
    ) -> str:
        keyword_text = ", ".join(str(item["keyword"]) for item in top_keywords[:5]) or "none"
        problem_text = ", ".join(str(item["name"]) for item in problems[:3]) or "none"
        topic_text = ", ".join(str(item["name"]) for item in topics[:3]) or "none"
        return (
            f"Analyzed {total} reviews. Positive: {sentiment_counter.get('positive', 0)}, "
            f"neutral: {sentiment_counter.get('neutral', 0)}, negative: {sentiment_counter.get('negative', 0)}. "
            f"Top keywords: {keyword_text}. Main negative problems: {problem_text}. "
            f"Topic clusters: {topic_text}."
        )
