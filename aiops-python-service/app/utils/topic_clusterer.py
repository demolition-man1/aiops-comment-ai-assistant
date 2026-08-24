from collections import Counter
from os import getenv
from typing import Iterable

TOPIC_RULES: dict[str, set[str]] = {
    "logistics": {
        "entrega",
        "entregue",
        "entregou",
        "prazo",
        "atrasada",
        "atrasado",
        "atrasou",
        "demorou",
        "recebi",
        "recebido",
        "chegou",
        "frete",
        "transportadora",
    },
    "quality": {
        "qualidade",
        "quebrado",
        "quebrada",
        "defeito",
        "defeituoso",
        "fraco",
        "ruim",
        "material",
        "produto",
        "danificado",
    },
    "package": {
        "embalagem",
        "pacote",
        "caixa",
        "amassado",
        "rasgado",
        "vazou",
        "lacrado",
    },
    "price": {
        "preco",
        "preço",
        "caro",
        "barato",
        "valor",
        "custo",
        "promoção",
    },
    "service": {
        "atendimento",
        "suporte",
        "vendedor",
        "resposta",
        "troca",
        "devolução",
        "reembolso",
    },
    "size": {
        "tamanho",
        "medida",
        "pequeno",
        "grande",
        "curto",
        "comprido",
        "apertado",
    },
}

_BERTOPIC_MODEL = None
_BERTOPIC_ATTEMPTED = False


def topic_distribution(texts: list[str], limit: int = 8) -> list[dict[str, int | str]]:
    cleaned = [str(text).strip() for text in texts if str(text).strip()]
    if not cleaned:
        return []
    semantic_topics = _bertopic_distribution(cleaned, limit)
    if semantic_topics:
        return semantic_topics
    return _rule_based_distribution(cleaned, limit)


def _rule_based_distribution(texts: list[str], limit: int) -> list[dict[str, int | str]]:
    counter: Counter[str] = Counter()
    for text in texts:
        lowered = text.lower()
        topic_scores = {
            topic: sum(1 for keyword in keywords if keyword in lowered)
            for topic, keywords in TOPIC_RULES.items()
        }
        topic, score = max(topic_scores.items(), key=lambda item: item[1])
        counter[topic if score > 0 else "other"] += 1
    return [{"name": topic, "count": count} for topic, count in counter.most_common(limit)]


def _bertopic_distribution(texts: list[str], limit: int) -> list[dict[str, int | str]]:
    if getenv("AIOPS_TOPIC_CLUSTERER", "rule").lower() != "bertopic":
        return []
    model = _load_bertopic_model()
    if model is None:
        return []
    try:
        topics, _ = model.fit_transform(texts)
        topic_info = model.get_topic_info()
    except Exception:
        return []
    topic_names = _topic_names(topic_info)
    counter = Counter(topic for topic in topics if topic != -1)
    return [
        {"name": topic_names.get(topic, f"topic_{topic}"), "count": count}
        for topic, count in counter.most_common(limit)
    ]


def _load_bertopic_model():
    global _BERTOPIC_MODEL, _BERTOPIC_ATTEMPTED
    if _BERTOPIC_ATTEMPTED:
        return _BERTOPIC_MODEL
    _BERTOPIC_ATTEMPTED = True
    try:
        from bertopic import BERTopic

        _BERTOPIC_MODEL = BERTopic(verbose=False)
    except Exception:
        _BERTOPIC_MODEL = None
    return _BERTOPIC_MODEL


def _topic_names(topic_info: Iterable[object]) -> dict[int, str]:
    names: dict[int, str] = {}
    try:
        rows = topic_info.to_dict("records")
    except Exception:
        return names
    for row in rows:
        topic = row.get("Topic")
        name = row.get("Name")
        if isinstance(topic, int) and name:
            names[topic] = str(name)
    return names
