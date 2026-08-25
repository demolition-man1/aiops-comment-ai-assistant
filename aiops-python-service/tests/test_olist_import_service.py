import unittest

import pandas as pd

from app.services.olist_import_service import OlistImportService


class OlistImportServiceTests(unittest.TestCase):
    def test_apply_column_mapping_normalizes_merchant_review_columns(self) -> None:
        service = OlistImportService()
        frame = pd.DataFrame(
            [
                {"商品ID": "sku-1", "评分": 5, "评论内容": "good delivery"},
                {"商品ID": "sku-2", "评分": 1, "评论内容": "bad packaging"},
            ]
        )

        normalized = service._apply_column_mapping(
            frame,
            {
                "product_id": "商品ID",
                "review_score": "评分",
                "review_content": "评论内容",
            },
        )

        self.assertIn("product_id", normalized.columns)
        self.assertIn("review_score", normalized.columns)
        self.assertIn("review_content", normalized.columns)
        self.assertEqual(normalized.iloc[0]["product_id"], "sku-1")
        self.assertEqual(normalized.iloc[1]["review_score"], 1)


if __name__ == "__main__":
    unittest.main()
