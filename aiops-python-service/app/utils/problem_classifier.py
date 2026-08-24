from collections import Counter

PROBLEM_RULES: dict[str, tuple[str, ...]] = {
    "logistics": (
        "entrega",
        "atraso",
        "prazo",
        "demora",
        "correio",
        "transportadora",
        "shipping",
        "delay",
        "物流",
        "发货",
        "快递",
        "延迟",
    ),
    "quality": (
        "quebrado",
        "defeito",
        "ruim",
        "qualidade",
        "danificado",
        "broken",
        "defect",
        "质量",
        "坏了",
        "破损",
    ),
    "size": (
        "tamanho",
        "pequeno",
        "grande",
        "medida",
        "size",
        "尺寸",
        "偏大",
        "偏小",
    ),
    "service": (
        "atendimento",
        "resposta",
        "vendedor",
        "suporte",
        "客服",
        "服务",
        "回复",
    ),
    "price": (
        "caro",
        "preço",
        "preco",
        "valor",
        "expensive",
        "价格",
        "贵",
    ),
}


def classify_problem(text: str, is_negative: int) -> str | None:
    if not is_negative:
        return None
    normalized = text.lower()
    for problem, keywords in PROBLEM_RULES.items():
        if any(keyword in normalized for keyword in keywords):
            return problem
    return "other"


def problem_distribution(problem_types: list[str | None]) -> list[dict[str, int | str]]:
    counter = Counter(item for item in problem_types if item)
    return [{"name": key, "count": value} for key, value in counter.most_common()]
