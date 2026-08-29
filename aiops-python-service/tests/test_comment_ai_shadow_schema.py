from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
SQL_DIRECTORY = REPOSITORY_ROOT / "aiops-backend" / "aiops-server" / "src" / "main" / "resources" / "sql"


def test_shadow_tables_have_matching_migration_and_baseline_schema() -> None:
    migration = (SQL_DIRECTORY / "upgrade-2026-08-29-comment-ai-shadow.sql").read_text(encoding="utf-8")
    baseline = (SQL_DIRECTORY / "schema.sql").read_text(encoding="utf-8")

    for sql in (migration, baseline):
        assert "create table if not exists biz_comment_ai_shadow_run" in sql
        assert "unique key uk_comment_ai_shadow_task (task_id)" in sql
        assert "create table if not exists biz_comment_ai_shadow_result" in sql
        assert "unique key uk_comment_ai_shadow_sample (run_id, comment_id)" in sql
        assert "index idx_comment_ai_shadow_result_run (run_id, sample_order)" in sql


def test_default_shadow_prompts_cover_all_supported_languages() -> None:
    data = (SQL_DIRECTORY / "data.sql").read_text(encoding="utf-8")

    for language in ("zh-CN", "en-US", "pt-BR"):
        assert f"'comment_analysis_shadow', '{language}'" in data
