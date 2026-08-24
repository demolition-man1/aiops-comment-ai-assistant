# AI Ops Sync Task Report Design

## Goal

Upgrade the topbar shortcuts from page anchors into three real operation modules:

- `/sync`: scheduled sync configuration backed by Quartz.
- `/tasks`: unified task center for import, analysis, AI, comparison, and sync execution records.
- `/reports`: standalone report center backed by MySQL aggregation and Redis-friendly service boundaries.

## Scope

This phase implements a production-shaped MVP. It must use real data and real backend endpoints, but it avoids heavyweight features that can be added later, such as PDF export, cron visual editing, and RAG/LangChain knowledge retrieval.

## Backend Design

### Tables

Add three tables:

- `biz_sync_config`: one row per scheduled sync rule.
- `biz_sync_execution`: one row per scheduled or manual sync run.
- `biz_task_record`: one normalized task row for task center queries.

Existing tables remain valid. `biz_analysis_task` and `biz_crawl_task` continue to store legacy import and analysis state. New services write a companion `biz_task_record` so the UI can display all task types in one place.

### Services

- `SyncConfigService`: validates sync configs, creates/updates/deletes configs, triggers sync immediately, and records execution state.
- `QuartzScheduleService`: registers, updates, pauses, resumes, and deletes Quartz jobs for enabled sync configs.
- `TaskCenterService`: lists normalized task records, returns details, and retries supported failed tasks.
- `ReportService`: returns report-center data by aggregating comments, products, sellers, and analysis results.

### Quartz Flow

1. User creates or enables a sync config.
2. Java saves `biz_sync_config`.
3. `QuartzScheduleService` registers a job using config ID.
4. On schedule, `SyncImportJob` calls `SyncConfigService.executeSyncConfig(configId, "scheduled")`.
5. The sync execution creates `biz_sync_execution` and `biz_task_record`.
6. The sync reuses existing CSV import or crawler import logic.
7. If enabled, a follow-up analysis task can be created after import.

## Frontend Design

### Routes

- `/sync`: Sync Center.
- `/tasks`: Task Center.
- `/reports`: Report Center.

The topbar buttons navigate directly to these routes.

### Pages

- `SyncCenterView.vue`: create sync config, list configs, enable/disable, trigger now, view execution history.
- `TaskCenterView.vue`: table with filters for type, status, keyword, and time range; detail and retry actions.
- `ReportsView.vue`: global KPIs, trends, sentiment distribution, problem distribution, and product ranking cards.

### Internationalization

All visible labels use the existing `vue-i18n` dictionaries for Chinese, English, and Portuguese.

## API Design

### Sync

- `GET /api/sync/configs`
- `POST /api/sync/configs`
- `PUT /api/sync/configs/{id}`
- `POST /api/sync/configs/{id}/enable`
- `POST /api/sync/configs/{id}/disable`
- `POST /api/sync/configs/{id}/trigger`
- `GET /api/sync/executions`

### Task Center

- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `POST /api/tasks/{id}/retry`

### Reports

- `GET /api/reports/overview`
- `GET /api/reports/trends`
- `GET /api/reports/distributions`
- `GET /api/reports/product-rank`

## Non-Goals

- No RAG/LangChain implementation in this phase.
- No PDF export in this phase.
- No real external marketplace crawler expansion in this phase.
- No replacement of existing JWT authentication.

## RAG Upgrade Path

Future RAG work should live mainly in the Python service. Java will add knowledge-base CRUD and task orchestration endpoints, while Python handles LangChain chains, chunking, vector search, and AI context assembly.
