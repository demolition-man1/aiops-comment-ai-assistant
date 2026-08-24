# AIops Maturity Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add production-oriented backend and Python service enhancements while preserving the current JWT login flow.

**Architecture:** Java keeps business APIs and auth. Bucket4j handles AI rate limits, Redisson supplies Redis locks, and Quartz runs maintenance jobs. Python NLP utilities gain optional semantic extraction and topic clustering with safe fallbacks.

**Tech Stack:** Spring Boot 3.3.5, MyBatis Plus, Knife4j/OpenAPI, Redis, Redisson, Bucket4j, Quartz, FastAPI, pandas, optional KeyBERT/BERTopic/Scrapy/Crawlee.

**Spec:** `docs/superpowers/specs/2026-08-22-aiops-maturity-enhancements-design.md`

## Global Constraints

- Keep JWT authentication unchanged.
- Do not add Sa-Token.
- Do not require new MySQL columns for topic clustering in this iteration.
- Optional Python NLP/crawler libraries must not break service startup when absent.

---

### Task 1: Java Dependencies And Configuration

**Files:**
- Modify: `aiops-backend/pom.xml`
- Modify: `aiops-backend/aiops-server/pom.xml`
- Create: `aiops-backend/aiops-common/src/main/java/com/aiops/properties/AiRateLimitProperties.java`
- Create: `aiops-backend/aiops-common/src/main/java/com/aiops/properties/TaskMaintenanceProperties.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/config/RedissonConfig.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/config/QuartzConfig.java`
- Modify: `aiops-backend/aiops-server/src/main/resources/application.yml`
- Modify: `aiops-backend/aiops-server/src/main/resources/application-dev.yml`

**Interfaces:**
- Produces: `AiRateLimitProperties#getCapacity()`, `#getRefillTokens()`, `#getRefillPeriodSeconds()`
- Produces: `TaskMaintenanceProperties#getStaleProcessingMinutes()`, `#getLockWaitSeconds()`, `#getLockLeaseSeconds()`

- [ ] Add dependency management versions for Redisson and Bucket4j.
- [ ] Add server dependencies for Redisson, Bucket4j, and `spring-boot-starter-quartz`.
- [ ] Add configuration properties for AI limits and task maintenance.
- [ ] Add Redisson client bean using the existing Spring Redis host/port/password values.
- [ ] Add Quartz scheduler configuration.

### Task 2: Bucket4j AI Rate Limiting

**Files:**
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/AiRateLimitService.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/Bucket4jAiRateLimitService.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/AiServiceImpl.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/AnalysisServiceImpl.java`
- Test: `aiops-backend/aiops-server/src/test/java/com/aiops/service/impl/Bucket4jAiRateLimitServiceTest.java`

**Interfaces:**
- Produces: `boolean AiRateLimitService.tryConsume(String businessType, Long userId)`
- Consumes: `BaseContext.getCurrentId()`

- [ ] Write a failing test proving the service rejects calls after capacity is consumed.
- [ ] Implement a Bucket4j-backed per-user, per-business limiter.
- [ ] Replace duplicated Redis counter checks in AI services with `AiRateLimitService`.
- [ ] Keep Redis counter code available for non-AI cache use.

### Task 3: Quartz Task Maintenance

**Files:**
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/TaskMaintenanceService.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/TaskMaintenanceServiceImpl.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/task/TaskMaintenanceJob.java`
- Test: `aiops-backend/aiops-server/src/test/java/com/aiops/service/impl/TaskMaintenanceServiceImplTest.java`

**Interfaces:**
- Produces: `int TaskMaintenanceService.failStaleProcessingTasks()`

- [ ] Write a failing test proving stale processing analysis tasks are marked failed.
- [ ] Implement stale task scanning for `biz_analysis_task`.
- [ ] Use Redisson lock in the Quartz job so multiple backend instances do not run the same maintenance scan concurrently.
- [ ] Register a durable Quartz job and trigger from configuration.

### Task 4: OpenAPI Annotation Pass

**Files:**
- Modify: all Java controller classes under `aiops-backend/aiops-server/src/main/java/com/aiops/controller`
- Modify: key DTO/VO classes under `aiops-backend/aiops-pojo/src/main/java/com/aiops`

**Interfaces:**
- Produces: Knife4j/OpenAPI-visible tags, operation names, and request/response descriptions.

- [ ] Add `@Tag` to controllers.
- [ ] Add `@Operation` to public API methods.
- [ ] Add `@Parameter` where path/query parameters need names.
- [ ] Add `@Schema` to the DTO/VO objects most often used by frontend calls.

### Task 5: Python Keyword Extraction And Topic Clustering

**Files:**
- Modify: `aiops-python-service/app/utils/keyword_extractor.py`
- Create: `aiops-python-service/app/utils/topic_clusterer.py`
- Modify: `aiops-python-service/app/services/comment_analysis_service.py`
- Modify: `aiops-python-service/requirements.txt`
- Test: `aiops-python-service/tests/test_text_tools.py`

**Interfaces:**
- Produces: `keyword_rank(texts: list[str], limit: int = 20) -> list[dict[str, int | str]]`
- Produces: `topic_distribution(texts: list[str], limit: int = 8) -> list[dict[str, int | str]]`

- [ ] Write failing tests for filtering placeholder tokens such as `nan`.
- [ ] Write failing tests for fallback topic clustering.
- [ ] Implement optional KeyBERT usage guarded by import failure handling.
- [ ] Implement optional BERTopic usage guarded by import failure handling.
- [ ] Merge topic output into the existing analysis summary and problem-style distribution without requiring a new DB column.

### Task 6: Crawler Upgrade Seam

**Files:**
- Modify: `aiops-python-service/app/services/crawler_service.py`
- Create: `aiops-python-service/app/services/crawlers/base.py`
- Create: `aiops-python-service/app/services/crawlers/sample.py`
- Create: `aiops-python-service/app/services/crawlers/scrapy_adapter.py`
- Create: `aiops-python-service/app/services/crawlers/crawlee_adapter.py`
- Test: `aiops-python-service/tests/test_crawler_service.py`

**Interfaces:**
- Produces: `BaseCrawler.crawl(request: dict[str, Any]) -> dict[str, Any]`
- Produces: `CrawlerService.crawl(request: dict[str, Any]) -> dict[str, Any]`

- [ ] Write a failing test proving default sample crawler returns the current safe demo response.
- [ ] Extract the current crawler behavior behind `BaseCrawler`.
- [ ] Add Scrapy and Crawlee placeholder adapters that fail with clear configuration messages when selected before dependencies are installed.

### Task 7: Verification

**Files:**
- No production files.

**Interfaces:**
- Consumes: all prior tasks.

- [ ] Run backend tests with Maven.
- [ ] Run Python tests with pytest.
- [ ] Run frontend typecheck/build if frontend files are touched.
- [ ] Report any dependency download or environment blockers with exact commands and failure output.
