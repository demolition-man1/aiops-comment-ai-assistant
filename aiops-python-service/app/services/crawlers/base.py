from abc import ABC, abstractmethod
from typing import Any


class BaseCrawler(ABC):
    @abstractmethod
    def crawl(self, request: dict[str, Any]) -> dict[str, Any]:
        raise NotImplementedError
