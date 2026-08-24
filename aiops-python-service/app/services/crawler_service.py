from typing import Any

from app.config import settings
from app.db import get_conn
from app.repositories import task_repository
from app.services.crawlers import CrawleeCrawler, SampleCrawler, ScrapyCrawler


class CrawlerService:
    def __init__(self, crawler_enabled: bool | None = None) -> None:
        self.crawler_enabled = settings.crawler_enabled if crawler_enabled is None else crawler_enabled

    def crawl(self, request: dict[str, Any]) -> dict[str, Any]:
        platform = str(request.get("platform") or "demo").lower()
        if not self.crawler_enabled:
            return SampleCrawler().crawl(request)
        if platform == "scrapy":
            return ScrapyCrawler().crawl(request)
        if platform == "crawlee":
            return CrawleeCrawler().crawl(request)
        return {
            "success": False,
            "message": f"No crawler adapter is implemented for platform: {platform}",
            "successCount": 0,
            "failCount": 1,
        }

    def import_by_crawler(self, request: dict[str, Any]) -> dict[str, Any]:
        task_id = int(request.get("taskId") or 0)
        crawl_result = self.crawl(request)
        message = str(crawl_result.get("message") or "")
        status = "success" if crawl_result.get("success") else "failed"

        with get_conn() as conn:
            task_repository.update_crawl_task(
                conn,
                task_id=task_id,
                status=status,
                progress=100,
                success_count=int(crawl_result.get("successCount") or 0),
                fail_count=int(crawl_result.get("failCount") or 0),
                error_message=message,
            )
        return crawl_result
