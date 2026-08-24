from typing import Any

from app.services.crawlers.base import BaseCrawler


class SampleCrawler(BaseCrawler):
    def crawl(self, request: dict[str, Any]) -> dict[str, Any]:
        return {
            "success": False,
            "message": (
                "Crawler adapter is reserved for low-frequency research collection. "
                "Set CRAWLER_ENABLED=true and choose a configured platform adapter before running real crawling."
            ),
            "successCount": 0,
            "failCount": 1,
        }
