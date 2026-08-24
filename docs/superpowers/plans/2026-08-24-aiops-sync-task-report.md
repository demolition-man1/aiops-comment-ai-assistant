# AI Ops Sync Task Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real scheduled sync, unified task center, and standalone report center for the AI Ops project.

**Architecture:** Java remains the business orchestration layer with MySQL, Redis, MyBatis-Plus, and Quartz. Python remains the analysis and AI worker. Vue routes become real feature pages instead of anchor shortcuts.

**Tech Stack:** Spring Boot 3.3.5, Java 21, MyBatis-Plus 3.5.7, MySQL 8.x, Redis, Quartz, Vue 3, Element Plus, ECharts.

**Spec:** `docs/superpowers/specs/2026-08-24-aiops-sync-task-report-design.md`

## Global Constraints

- Keep JWT unchanged.
- Do not commit secrets or local `application-secret.yml`.
- Use MyBatis-Plus mapper interfaces and annotation SQL where lightweight custom queries are needed.
- Reuse existing `DataImportService` and `AnalysisService` behavior instead of duplicating Python client calls.
- Keep this phase to scheduled sync, task center, and report center; leave RAG/LangChain for a later phase.

---

### Task 1: Database And POJO Model

**Files:**
- Modify: `aiops-backend/aiops-server/src/main/resources/sql/schema.sql`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizSyncConfig.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizSyncExecution.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizTaskRecord.java`
- Create DTO and VO classes under `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto` and `.../vo`
- Create mapper interfaces under `aiops-backend/aiops-server/src/main/java/com/aiops/mapper`

**Deliverable:** New tables and Java models compile.

- [ ] Add SQL tables and indexes.
- [ ] Add entity classes using existing Lombok and MyBatis-Plus conventions.
- [ ] Add DTO/VO classes for sync configs, executions, task records, and report responses.
- [ ] Add mapper interfaces extending `BaseMapper`.
- [ ] Run Maven tests.

### Task 2: Sync And Quartz Services

**Files:**
- Create: `SyncConfigService`, `QuartzScheduleService`
- Create: `SyncConfigServiceImpl`, `QuartzScheduleServiceImpl`
- Create: `SyncImportJob`
- Create: `SyncConfigController`

**Deliverable:** `/api/sync/*` endpoints support config CRUD, enable/disable, manual trigger, and execution list.

- [ ] Write service tests for creating an enabled config and scheduling the Quartz job.
- [ ] Implement config validation and default cron.
- [ ] Implement Quartz register/delete/reschedule methods.
- [ ] Implement sync execution by reusing `DataImportService`.
- [ ] Expose documented REST endpoints.
- [ ] Run Maven tests.

### Task 3: Unified Task Center Backend

**Files:**
- Create: `TaskCenterService`, `TaskCenterServiceImpl`
- Create: `TaskCenterController`
- Modify: `DataImportServiceImpl`, `AnalysisServiceImpl`, AI service classes only where needed to write task records.

**Deliverable:** `/api/tasks` lists normalized records and supports retry for import and analysis tasks.

- [ ] Write service tests for task list filtering.
- [ ] Implement task record creation/update helpers.
- [ ] Integrate CSV import, crawler import, and analysis task creation with `biz_task_record`.
- [ ] Implement retry for supported failed tasks.
- [ ] Expose REST endpoints.
- [ ] Run Maven tests.

### Task 4: Report Center Backend

**Files:**
- Create: `ReportService`, `ReportServiceImpl`
- Create: `ReportController`
- Extend mapper SQL methods if required.

**Deliverable:** `/api/reports/*` returns KPI, trend, distribution, and product ranking data from MySQL.

- [ ] Write service tests for overview aggregation.
- [ ] Implement overview response.
- [ ] Implement trend and distribution responses.
- [ ] Implement product ranking response.
- [ ] Expose REST endpoints.
- [ ] Run Maven tests.

### Task 5: Frontend Routes And APIs

**Files:**
- Modify: `aiops-frontend/src/router/index.ts`
- Modify: `aiops-frontend/src/layouts/MainLayout.vue`
- Modify: `aiops-frontend/src/api/modules.ts`
- Modify: `aiops-frontend/src/api/types.ts`
- Create: `SyncCenterView.vue`, `TaskCenterView.vue`, `ReportsView.vue`
- Modify locale dictionaries.

**Deliverable:** Topbar buttons route to `/sync`, `/tasks`, and `/reports`, and all pages render real API data.

- [ ] Add failing frontend navigation tests.
- [ ] Add route entries.
- [ ] Add API client methods and TS types.
- [ ] Build the three pages with Element Plus tables/forms/charts.
- [ ] Add Chinese, English, and Portuguese labels.
- [ ] Run frontend tests and typecheck.

### Task 6: Verification And GitHub Sync

**Files:**
- Modify project docs if API paths change during implementation.

**Deliverable:** Tests pass, frontend builds, backend compiles, and changes are pushed.

- [ ] Run backend Maven tests.
- [ ] Run Python tests if Python files were touched.
- [ ] Run frontend tests, typecheck, and build.
- [ ] Check `git status` and ensure no secrets are staged.
- [ ] Commit with an English message.
- [ ] Push to GitHub private repository.
