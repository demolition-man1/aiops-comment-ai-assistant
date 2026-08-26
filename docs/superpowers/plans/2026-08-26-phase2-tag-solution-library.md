# Phase 2 Tag and Solution Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a usable custom tag library and problem solution library that connect to comment tagging and negative-review handling.

**Architecture:** Java Spring Boot owns persistence, validation, pagination, and API contracts. Vue pages provide library management and comment-page integration. Existing `biz_comment.custom_tags` and `manual_problem_type` remain the comment annotation fields.

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus, MySQL, Vue 3, TypeScript, Element Plus, node:test, JUnit 5.

**Spec:** `docs/PHASE2_TODO.md`

## Global Constraints

- Keep JWT auth and existing API response shape.
- Use MyBatis-Plus Mapper interfaces, not XML mapper files.
- Keep README copy positive and final-state oriented.
- Add tests before production behavior changes.
- Keep Phase 2 focused on custom tags and problem solutions.

---

### Task 1: Backend Tag Library

**Files:**
- Modify: `aiops-backend/aiops-server/src/main/resources/sql/schema.sql`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizCustomTag.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/CustomTagDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/CustomTagQueryDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/CustomTagVO.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/mapper/BizCustomTagMapper.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/TagLibraryService.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/TagLibraryServiceImpl.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/controller/TagLibraryController.java`
- Test: `aiops-backend/aiops-server/src/test/java/com/aiops/service/impl/TagLibraryServiceImplTest.java`

**Interfaces:**
- Produces: `GET /api/tags`, `POST /api/tags`, `PUT /api/tags/{tagId}`, `PUT /api/tags/{tagId}/status`

- [x] Write failing service tests for create, page, and status update.
- [x] Add table `biz_custom_tag`.
- [x] Add entity, DTO, VO, mapper, service, controller.
- [x] Run `mvn -pl aiops-server -am -Dtest=TagLibraryServiceImplTest test`.

### Task 2: Backend Problem Solution Library

**Files:**
- Modify: `aiops-backend/aiops-server/src/main/resources/sql/schema.sql`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/entity/BizProblemSolution.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/ProblemSolutionDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/dto/ProblemSolutionQueryDTO.java`
- Create: `aiops-backend/aiops-pojo/src/main/java/com/aiops/vo/ProblemSolutionVO.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/mapper/BizProblemSolutionMapper.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/ProblemSolutionService.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/service/impl/ProblemSolutionServiceImpl.java`
- Create: `aiops-backend/aiops-server/src/main/java/com/aiops/controller/ProblemSolutionController.java`
- Test: `aiops-backend/aiops-server/src/test/java/com/aiops/service/impl/ProblemSolutionServiceImplTest.java`

**Interfaces:**
- Produces: `GET /api/problem-solutions`, `GET /api/problem-solutions/recommend`, `POST /api/problem-solutions`, `PUT /api/problem-solutions/{solutionId}`, `PUT /api/problem-solutions/{solutionId}/status`

- [x] Write failing service tests for create, page, and recommendation by problem type/category/keyword.
- [x] Add table `biz_problem_solution`.
- [x] Add entity, DTO, VO, mapper, service, controller.
- [x] Run `mvn -pl aiops-server -am -Dtest=ProblemSolutionServiceImplTest test`.

### Task 3: Frontend Library Pages and Comment Integration

**Files:**
- Modify: `aiops-frontend/src/api/types.ts`
- Modify: `aiops-frontend/src/api/modules.ts`
- Modify: `aiops-frontend/src/router/index.ts`
- Modify: `aiops-frontend/src/layouts/MainLayout.vue`
- Modify: `aiops-frontend/src/i18n/locales/zh-CN.ts`
- Modify: `aiops-frontend/src/i18n/locales/en-US.ts`
- Modify: `aiops-frontend/src/i18n/locales/pt-BR.ts`
- Create: `aiops-frontend/src/views/TagLibraryView.vue`
- Create: `aiops-frontend/src/views/ProblemSolutionView.vue`
- Modify: `aiops-frontend/src/views/CommentWorkbenchView.vue`
- Test: `aiops-frontend/tests/navigation.test.mjs`

**Interfaces:**
- Consumes: tag and solution APIs from Tasks 1 and 2.
- Produces: `/tags`, `/solutions`, tag selector in comment tag dialog, and solution recommendation panel in comment workspace.

- [x] Write failing frontend structure tests for new routes, API modules, and comment integration.
- [x] Add API wrappers and types.
- [x] Add two management pages.
- [x] Wire navigation entries and i18n labels.
- [x] Update comment tag dialog to load active tags.
- [x] Add recommendation panel for selected negative comment.
- [x] Run `npm run test:navigation` and `npm run build`.

### Task 4: Docs, Todo, and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `README_EN.md`
- Modify: `docs/PHASE2_TODO.md`

**Interfaces:**
- Produces: updated public documentation and checked Phase 2 todo state.

- [x] Update README feature list and quick-start usage notes.
- [x] Update English README with the same final-state information.
- [x] Mark Phase 2 complete in `docs/PHASE2_TODO.md`.
- [ ] Run Java, frontend, and patch checks.
- [ ] Scan staged files for secrets.
- [ ] Commit and push.
