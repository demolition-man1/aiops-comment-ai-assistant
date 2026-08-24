import unittest

from app.services.crawler_service import CrawlerService


class CrawlerServiceTests(unittest.TestCase):
    def test_default_crawl_uses_safe_sample_adapter(self) -> None:
        result = CrawlerService().crawl({"platform": "demo", "targetUrl": "https://example.com/item/1"})

        self.assertFalse(result["success"])
        self.assertIn("reserved for low-frequency research collection", result["message"])

    def test_scrapy_adapter_returns_clear_setup_message(self) -> None:
        result = CrawlerService(crawler_enabled=True).crawl(
            {"platform": "scrapy", "targetUrl": "https://example.com/item/1"}
        )

        self.assertFalse(result["success"])
        self.assertIn("Scrapy", result["message"])
        self.assertIn("optional crawler dependencies", result["message"])


if __name__ == "__main__":
    unittest.main()
