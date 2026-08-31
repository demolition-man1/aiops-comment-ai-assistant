from fastapi import APIRouter, BackgroundTasks, HTTPException

from app.rag.index_service import RagDisabledError, rag_index_service


router = APIRouter(prefix="/internal/ai/rag", tags=["internal-rag"])


@router.get("/status")
def get_rag_status() -> dict[str, object]:
    return {"success": True, "data": rag_index_service.status().to_payload()}


@router.post("/reindex", status_code=202)
def reindex_rag(background_tasks: BackgroundTasks) -> dict[str, object]:
    try:
        accepted = rag_index_service.start_reindex()
    except RagDisabledError as exc:
        raise HTTPException(status_code=409, detail="RAG is disabled.") from exc
    if not accepted:
        raise HTTPException(status_code=409, detail="Knowledge index rebuild is already running.")
    background_tasks.add_task(rag_index_service.run_reserved_reindex)
    return {"success": True, "data": rag_index_service.status().to_payload()}
