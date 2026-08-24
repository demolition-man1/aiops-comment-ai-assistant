import re
from collections import Counter
from os import getenv
from typing import Iterable

STOPWORDS = {
    "a",
    "o",
    "e",
    "de",
    "do",
    "da",
    "em",
    "um",
    "uma",
    "para",
    "com",
    "que",
    "ao",
    "aos",
    "as",
    "até",
    "antes",
    "bem",
    "como",
    "dos",
    "das",
    "ele",
    "ela",
    "eu",
    "foi",
    "me",
    "meu",
    "minha",
    "muito",
    "muita",
    "mas",
    "na",
    "não",
    "no",
    "nos",
    "ou",
    "por",
    "sem",
    "seu",
    "sua",
    "super",
    "um",
    "uma",
    "the",
    "and",
    "for",
    "this",
    "that",
    "是",
    "的",
    "了",
    "和",
    "很",
    "买",
    "nan",
    "none",
    "null",
    "undefined",
}

_KEYBERT_MODEL = None
_KEYBERT_ATTEMPTED = False


def extract_keywords(text: str, limit: int = 8) -> list[str]:
    semantic_keywords = _extract_keybert_keywords(text, limit)
    if semantic_keywords:
        return semantic_keywords
    words = re.findall(r"[\w\u4e00-\u9fff]{2,}", text.lower())
    filtered = [word for word in words if word not in STOPWORDS and not word.isdigit()]
    return [word for word, _ in Counter(filtered).most_common(limit)]


def keyword_rank(texts: list[str], limit: int = 20) -> list[dict[str, int | str]]:
    counter: Counter[str] = Counter()
    for text in texts:
        counter.update(extract_keywords(text, limit=20))
    return [{"keyword": key, "count": value} for key, value in counter.most_common(limit)]


def _extract_keybert_keywords(text: str, limit: int) -> list[str]:
    if getenv("AIOPS_KEYWORD_EXTRACTOR", "rule").lower() != "keybert":
        return []
    model = _load_keybert_model()
    if model is None:
        return []
    try:
        result = model.extract_keywords(
            text,
            keyphrase_ngram_range=(1, 2),
            stop_words=list(STOPWORDS),
            top_n=limit,
        )
    except Exception:
        return []
    return [keyword for keyword in _normalize_keybert_result(result) if keyword not in STOPWORDS][:limit]


def _load_keybert_model():
    global _KEYBERT_MODEL, _KEYBERT_ATTEMPTED
    if _KEYBERT_ATTEMPTED:
        return _KEYBERT_MODEL
    _KEYBERT_ATTEMPTED = True
    try:
        from keybert import KeyBERT

        _KEYBERT_MODEL = KeyBERT()
    except Exception:
        _KEYBERT_MODEL = None
    return _KEYBERT_MODEL


def _normalize_keybert_result(result: Iterable[object]) -> list[str]:
    keywords: list[str] = []
    for item in result:
        keyword = item[0] if isinstance(item, tuple) and item else item
        text = str(keyword).strip().lower()
        if text and text not in STOPWORDS:
            keywords.append(text)
    return keywords
