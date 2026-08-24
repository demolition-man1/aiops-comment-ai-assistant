from typing import Any

from app.services.crawlers.base import BaseCrawler


class CrawleeCrawler(BaseCrawler):
    def crawl(self, request: dict[str, Any]) -> dict[str, Any]:
        return {
            "success": False,
            "message": (
                "Crawlee crawler adapter is reserved but not active. "
                "Install optional crawler dependencies and implement a compliant Crawlee workflow before use."
            ),
            "successCount": 0,
            "failCount": 1,
        }
