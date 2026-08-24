# AI Ops Operations Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add review tag editing, richer analysis/dashboard payloads, time trends, product comparison, and negative reply effect tracking.

**Architecture:** Keep Java as the business API layer and Python as the analysis/AI engine. Java owns MySQL persistence, Redis cache keys, API DTO/VO conversion, and calls Python for comment analysis and AI product comparison.

**Tech Stack:** Spring Boot 3.3.5, MyBatis-Plus 3.5.7, MySQL 8.x, Redis, FastAPI, Pandas, PyMySQL, Requests.

**Spec:** `work/docs/superpowers/specs/2026-08-20-aiops-operations-enhancement-design.md`

## Global Constraints

- Keep the existing multi-module Java layout: `aiops-common`, `aiops-pojo`, `aiops-server`.
- Use MyBatis-Plus mappers and existing service/controller patterns.
- Do not add Excel/PDF dependencies in this iteration.
- Do not add a real crawler adapter or mock crawler data in this iteration.
- Do not commit downloaded `data/` files, `.env`, `.venv`, logs, or build output.
- Preserve existing public endpoints and add fields in a backward-compatible way.

---

### Task 1: Database And Entity Fields

**Files:**
- Modify: `aiops-backend/aiops-server/src/main/resources/sql/schema.sql`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizComment.java`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizCommentAnalysisResult.java`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizNegativeReply.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizProductCompareReport.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/mapper/BizProductCompareReportMapper.java`

**Interfaces:**
- Produces entity fields used by later service tasks.
- Produces `BizProductCompareReportMapper extends BaseMapper<BizProductCompareReport>`.

- [ ] Add `manual_problem_type`, `custom_tags`, and `tag_update_time` to `biz_comment`.
- [ ] Add `custom_tag_distribution` and `trend_distribution` to `biz_comment_analysis_result`.
- [ ] Add `effect_tag`, `use_count`, `favorite_flag`, and `update_time` to `biz_negative_reply`.
- [ ] Add `biz_product_compare_report`.
- [ ] Mirror the fields in entity classes with Lombok `@Data`.
- [ ] Run Java compile after entity updates.

### Task 2: Comment Tag Editing API

**Files:**
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/CommentTagUpdateDTO.java`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/CommentVO.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/CommentService.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/CommentServiceImpl.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/controller/CommentController.java`

**Interfaces:**
- Consumes `BizComment.manualProblemType` and `BizComment.customTags`.
- Produces `CommentService.updateTags(Long commentId, CommentTagUpdateDTO dto)`.

- [ ] Add DTO with `manualProblemType` and `customTags`.
- [ ] Extend `CommentVO` with title, clean content, system/manual/effective problem type, custom tags, and negative flag.
- [ ] Implement JSON serialization for `customTags`.
- [ ] Implement `PUT /api/comments/{commentId}/tags`.
- [ ] Invalidate relevant analysis/dashboard Redis caches when a tag is edited.

### Task 3: Analysis And Dashboard Payload Enhancement

**Files:**
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/AnalysisResultVO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/TrendItemVO.java`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/DashboardVO.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/AnalysisServiceImpl.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/DashboardServiceImpl.java`

**Interfaces:**
- Consumes JSON fields from `BizCommentAnalysisResult`.
- Produces enriched `AnalysisResultVO` and `DashboardVO`.

- [ ] Add `negativeKeywords`, `scoreDistribution`, `customTagDistribution`, `trendDistribution`, and `createTime` to `AnalysisResultVO`.
- [ ] Add `TrendItemVO`.
- [ ] Add `negativeKeywordRank`, `customTagDistribution`, and `trendDistribution` to `DashboardVO`.
- [ ] Parse JSON arrays into strongly typed VO lists.
- [ ] Keep old fields working for existing frontend calls.

### Task 4: Python Trend And Custom Tag Aggregation

**Files:**
- Modify: `aiops-python-service/app/repositories/comment_repository.py`
- Modify: `aiops-python-service/app/repositories/analysis_repository.py`
- Modify: `aiops-python-service/app/services/comment_analysis_service.py`
- Add/modify tests: `aiops-python-service/tests/test_text_tools.py`

**Interfaces:**
- Consumes comment rows with `review_time`, `review_score`, `is_negative`, and `custom_tags`.
- Produces `custom_tag_distribution` and `trend_distribution` JSON columns.

- [ ] Fetch `custom_tags` from `biz_comment`.
- [ ] Add custom tag aggregation.
- [ ] Add day/week/month trend aggregation.
- [ ] Store both JSON fields in `biz_comment_analysis_result`.
- [ ] Add tests for trend aggregation and tag distribution.

### Task 5: Product Comparison API

**Files:**
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/ProductCompareDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/ProductCompareReportVO.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/client/PythonAiClient.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/AnalysisService.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/AnalysisServiceImpl.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/controller/AnalysisController.java`
- Modify: `aiops-python-service/app/services/ai_service.py`
- Modify: `aiops-python-service/app/routers/ai_router.py`

**Interfaces:**
- Consumes latest product analysis results.
- Produces `POST /api/analysis/products/compare`, `GET /api/analysis/products/compare`, and `GET /api/analysis/products/compare/{reportId}`.
- Produces Python `POST /internal/ai/product-compare`.

- [ ] Add compare DTO and VO.
- [ ] Add Redis key `ai:compare:product:{leftProductId}:{rightProductId}`.
- [ ] Implement Java comparison creation, cache lookup, Python call, and MySQL persistence.
- [ ] Implement comparison history pagination and detail query.
- [ ] Implement Python AI comparison endpoint with JSON output parsing.

### Task 6: Negative Reply Tracking

**Files:**
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/NegativeReplyEffectDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/NegativeReplyFavoriteDTO.java`
- Modify: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/NegativeReplyVO.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/AiService.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/AiServiceImpl.java`
- Modify: `aiops-backend/aiops-server/src/main/java/com/aiops/controller/NegativeReplyController.java`

**Interfaces:**
- Consumes extended `BizNegativeReply` fields.
- Produces effect/favorite/use tracking endpoints.

- [ ] Add DTOs for effect and favorite updates.
- [ ] Extend `NegativeReplyVO`.
- [ ] Set default `useCount=0` and `favoriteFlag=0` when generating a reply.
- [ ] Implement `PUT /api/ai/negative-replies/{replyId}/effect`.
- [ ] Implement `POST /api/ai/negative-replies/{replyId}/use`.
- [ ] Implement `PUT /api/ai/negative-replies/{replyId}/favorite`.

### Task 7: Documentation And Verification

**Files:**
- Modify: `outputs/AI智能运营助手项目文档.md`
- Modify: `outputs/AI智能运营助手接口文档.md`
- Modify: `aiops-backend/README.md`
- Modify: `aiops-python-service/README.md`

**Interfaces:**
- Documents all implemented endpoints and fields.

- [ ] Update project document with selected enhancement package.
- [ ] Update API document with new Java and Python endpoints.
- [ ] Run Python compile and unittest.
- [ ] Run Maven test for Java backend.
- [ ] Check Git status and commit implementation.

## Self-Review

- Spec coverage: All five selected features are represented by Tasks 1 through 6, and documentation/verification is Task 7.
- Placeholder scan: No placeholders, no open implementation gaps are described as future TODOs.
- Type consistency: DTO, VO, entity, mapper, and endpoint names match across tasks.
