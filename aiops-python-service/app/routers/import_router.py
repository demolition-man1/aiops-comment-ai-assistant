from typing import Any

from fastapi import APIRouter, HTTPException
from requests import RequestException

from app.services.crawler_service import CrawlerService
from app.services.olist_import_service import OlistImportService

router = APIRouter(prefix="/internal", tags=["internal-import"])


@router.post("/csv/import")
def import_csv(request: dict[str, Any]) -> dict[str, Any]:
    return _call_import(lambda: OlistImportService().import_csv(request))


@router.post("/crawler/import")
def import_by_crawler(request: dict[str, Any]) -> dict[str, Any]:
    return _call_import(lambda: CrawlerService().import_by_crawler(request))


def _call_import(action: Any) -> dict[str, Any]:
    try:
        return action()
    except (ValueError, FileNotFoundError) as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except RequestException as exc:
        raise HTTPException(status_code=502, detail=f"CSV file request failed: {exc}") from exc
