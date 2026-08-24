import unittest
from decimal import Decimal

import pandas as pd

from app.utils.keyword_extractor import extract_keywords, keyword_rank
from app.utils.problem_classifier import classify_problem
from app.utils.sentiment_analyzer import sentiment_from_score
from app.utils.text_cleaner import clean_text
from app.utils.topic_clusterer import topic_distribution
from app.services.comment_analysis_service import aggregate_custom_tags, build_problem_types, build_trend_distribution
from app.services.ai_service import AiService
from app.services.olist_import_service import OlistImportService


class TextToolTests(unittest.TestCase):
    def test_clean_text_removes_html_and_urls(self) -> None:
        value = clean_text("<p>good product</p> https://example.com")
        self.assertEqual(value, "good product")

    def test_sentiment_from_score(self) -> None:
        self.assertEqual(sentiment_from_score(1), ("negative", Decimal("0.2000"), 1))
        self.assertEqual(sentiment_from_score(3), ("neutral", Decimal("0.5000"), 0))
        self.assertEqual(sentiment_from_score(5), ("positive", Decimal("0.8500"), 0))

    def test_problem_classifier(self) -> None:
        self.assertEqual(classify_problem("entrega com atraso", 1), "logistics")
        self.assertEqual(classify_problem("produto quebrado", 1), "quality")
        self.assertIsNone(classify_problem("produto bom", 0))

    def test_keyword_tools(self) -> None:
        self.assertIn("produto", extract_keywords("produto produto bom", limit=2))
        ranked = keyword_rank(["produto bom", "produto excelente"], limit=1)
        self.assertEqual(ranked[0]["keyword"], "produto")
        self.assertEqual(ranked[0]["count"], 2)

    def test_keyword_tools_filter_placeholder_tokens(self) -> None:
        ranked = keyword_rank(["nan nan produto ótimo", "null produto excelente"], limit=3)
        keywords = [item["keyword"] for item in ranked]
        self.assertIn("produto", keywords)
        self.assertNotIn("nan", keywords)
        self.assertNotIn("null", keywords)

    def test_topic_distribution_falls_back_to_rule_based_clusters(self) -> None:
        topics = topic_distribution(
            [
                "entrega atrasada e prazo muito ruim",
                "produto chegou quebrado com baixa qualidade",
                "demorou entrega, nao recebi no prazo",
            ],
            limit=3,
        )

        self.assertEqual(topics[0]["name"], "logistics")
        self.assertEqual(topics[0]["count"], 2)
        self.assertIn({"name": "quality", "count": 1}, topics)

    def test_aggregate_custom_tags_counts_json_array_tags(self) -> None:
        comments = [
            {"custom_tags": '["delivery_delay", "vip"]'},
            {"custom_tags": '["delivery_delay"]'},
            {"custom_tags": None},
        ]

        self.assertEqual(
            aggregate_custom_tags(comments),
            [
                {"name": "delivery_delay", "count": 2},
                {"name": "vip", "count": 1},
            ],
        )

    def test_build_problem_types_prefers_manual_problem_type(self) -> None:
        comments = [
            {"is_negative": 1, "problem_type": "quality", "manual_problem_type": "package"},
            {"is_negative": 1, "problem_type": "logistics", "manual_problem_type": None},
            {"is_negative": 0, "problem_type": "quality", "manual_problem_type": "service"},
        ]

        self.assertEqual(build_problem_types(comments), ["package", "logistics"])

    def test_build_trend_distribution_groups_by_month(self) -> None:
        comments = [
            {"review_time": "2018-05-01 00:00:00", "review_score": 5, "is_negative": 0},
            {"review_time": "2018-05-20 00:00:00", "review_score": 1, "is_negative": 1},
            {"review_time": "2018-06-02 00:00:00", "review_score": 3, "is_negative": 0},
        ]

        self.assertEqual(
            build_trend_distribution(comments, "month"),
            [
                {
                    "timeBucket": "2018-05",
                    "commentCount": 2,
                    "negativeCount": 1,
                    "negativeRate": 0.5,
                    "avgScore": 3.0,
                },
                {
                    "timeBucket": "2018-06",
                    "commentCount": 1,
                    "negativeCount": 0,
                    "negativeRate": 0.0,
                    "avgScore": 3.0,
                },
            ],
        )

    def test_generate_product_compare_parses_ai_json(self) -> None:
        service = AiService()
        service._chat = lambda prompt, temperature: (
            '{"compareSummary":"A negative rate is lower.",'
            '"advantageAnalysis":"A has better logistics feedback.",'
            '"riskAnalysis":"B has more quality complaints.",'
            '"operationSuggestions":"Use A as benchmark."}'
        )

        result = service.generate_product_compare(
            {
                "leftProductId": "product-a",
                "rightProductId": "product-b",
                "leftAnalysis": {"negativeRate": 0.1},
                "rightAnalysis": {"negativeRate": 0.3},
                "language": "zh-CN",
            }
        )

        self.assertTrue(result["success"])
        self.assertEqual(result["data"]["compareSummary"], "A negative rate is lower.")
        self.assertEqual(result["data"]["operationSuggestions"], "Use A as benchmark.")

    def test_generate_product_compare_flattens_nested_ai_sections(self) -> None:
        service = AiService()
        service._chat = lambda prompt, temperature: (
            '{"compareSummary":"A is more stable.",'
            '"advantageAnalysis":{"left":"评论量更大","right":"好评率更高"},'
            '"riskAnalysis":{"left":"质量投诉较多","right":"物流仍需优化"},'
            '"operationSuggestions":{"left":"重点改进质量","right":"继续保持安装体验"}}'
        )

        result = service.generate_product_compare(
            {
                "leftProductId": "product-a",
                "rightProductId": "product-b",
                "leftAnalysis": {"negativeRate": 0.2},
                "rightAnalysis": {"negativeRate": 0.1},
                "language": "zh-CN",
            }
        )

        advantage = result["data"]["advantageAnalysis"]
        self.assertIsInstance(advantage, str)
        self.assertIn("左侧：评论量更大", advantage)
        self.assertIn("右侧：好评率更高", advantage)
        self.assertNotIn("left=", advantage)

    def test_generate_negative_reply_uses_comment_context_and_higher_variation(self) -> None:
        service = AiService()
        captured = {}

        def fake_chat(prompt: str, temperature: float) -> str:
            captured["prompt"] = prompt
            captured["temperature"] = temperature
            return "尊敬的顾客，很抱歉包装破损影响了体验。"

        service._chat = fake_chat

        result = service.generate_negative_reply(
            {
                "commentId": 22,
                "reviewId": "review-22",
                "productId": "product-a",
                "reviewScore": 2,
                "commentTitle": "Entrega atrasada",
                "commentContent": "produto chegou quebrado",
                "problemType": "packaging",
                "toneType": "professional",
                "language": "zh-CN",
            }
        )

        self.assertTrue(result["success"])
        self.assertIn("produto chegou quebrado", captured["prompt"])
        self.assertIn("review-22", captured["prompt"])
        self.assertIn("product-a", captured["prompt"])
        self.assertIn("2", captured["prompt"])
        self.assertIn("每条回复都必须针对这条评论单独生成", captured["prompt"])
        self.assertGreaterEqual(captured["temperature"], 0.7)

    def test_translate_comment_uses_target_language_and_parses_json(self) -> None:
        service = AiService()
        captured = {}

        def fake_chat(prompt: str, temperature: float) -> str:
            captured["prompt"] = prompt
            captured["temperature"] = temperature
            return (
                '{"translatedContent":"The product arrived broken.",'
                '"sourceLanguage":"pt-BR"}'
            )

        service._chat = fake_chat

        result = service.translate_comment(
            {
                "commentId": 22,
                "reviewId": "review-22",
                "commentContent": "Produto chegou quebrado",
                "targetLanguage": "en-US",
            }
        )

        self.assertTrue(result["success"])
        self.assertEqual(result["data"]["translatedContent"], "The product arrived broken.")
        self.assertEqual(result["data"]["sourceLanguage"], "pt-BR")
        self.assertIn("target language: en-US", captured["prompt"])
        self.assertIn("Produto chegou quebrado", captured["prompt"])
        self.assertLessEqual(captured["temperature"], 0.3)

    def test_single_csv_comment_rows_require_product_id_and_review_score(self) -> None:
        service = OlistImportService()
        df = pd.DataFrame(
            [
                {
                    "review_id": "review-a",
                    "product_id": "product-a",
                    "seller_id": "seller-a",
                    "review_score": 1,
                    "review_content": "produto chegou quebrado",
                }
            ]
        )

        rows = service._build_comment_rows(df)

        self.assertEqual(rows[0]["review_id"], "review-a")
        self.assertEqual(rows[0]["product_id"], "product-a")
        self.assertEqual(rows[0]["seller_id"], "seller-a")
        self.assertEqual(rows[0]["sentiment"], "negative")
        self.assertEqual(rows[0]["problem_type"], "quality")

        with self.assertRaisesRegex(ValueError, "product_id.*review_score"):
            service._build_comment_rows(pd.DataFrame([{"review_content": "sem identificador"}]))


if __name__ == "__main__":
    unittest.main()
