from fastapi import FastAPI

from app.routers import ai_router, analysis_router, import_router

app = FastAPI(title="AI Ops Python Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, object]:
    return {"success": True, "status": "ok"}


app.include_router(import_router.router)
app.include_router(analysis_router.router)
app.include_router(ai_router.router)
