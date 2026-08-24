# AI Ops Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Vue3 admin frontend for the AI ecommerce operations assistant that can connect directly to the existing Java backend.

**Architecture:** The frontend is a standalone `aiops-frontend` Vite app. It uses Vue Router for pages, Pinia for auth state, Axios for backend calls, Element Plus for business controls, ECharts for dashboard charts, and Vite proxy for local frontend-backend integration.

**Tech Stack:** Vue3, TypeScript, Vite, Vue Router, Pinia, Element Plus, ECharts, Axios, lucide-vue-next.

**Spec:** User selected a combined design: C-style merchant cockpit as the home page, B-style comment analysis workbench as the core page, and A-style professional chart density for analysis dashboards.

## Global Constraints

- Use real Java backend APIs under `/api`; do not make fake backend endpoints.
- Local frontend-backend integration uses Vite proxy `/api -> http://localhost:8080`.
- The app must support JWT login and attach `Authorization: Bearer <token>` for protected APIs.
- Use dense but readable operational layouts; no marketing landing page.
- Keep visual style aligned with Vue3 + Element Plus admin systems.

---

### Task 1: Project Skeleton

**Files:**
- Create: `aiops-frontend/package.json`
- Create: `aiops-frontend/index.html`
- Create: `aiops-frontend/vite.config.ts`
- Create: `aiops-frontend/tsconfig.json`
- Create: `aiops-frontend/.env.example`
- Modify: `.gitignore`

**Interfaces:**
- Produces: a Vite app runnable with `npm run dev`.

- [ ] Create the Vite + Vue3 + TypeScript configuration.
- [ ] Add dependency scripts: `dev`, `build`, `preview`, `typecheck`.
- [ ] Add Vite proxy for `/api`.
- [ ] Ignore frontend `node_modules` and `dist`.

### Task 2: API Client And Auth

**Files:**
- Create: `aiops-frontend/src/api/http.ts`
- Create: `aiops-frontend/src/api/types.ts`
- Create: `aiops-frontend/src/api/modules.ts`
- Create: `aiops-frontend/src/stores/auth.ts`
- Create: `aiops-frontend/src/router/index.ts`

**Interfaces:**
- Consumes: Java backend result shape `{ code, msg, data }`.
- Produces: typed API modules for dashboard, products, comments, analysis, AI, files, and data import.

- [ ] Implement Axios response unwrapping.
- [ ] Attach JWT token from Pinia/localStorage.
- [ ] Redirect unauthenticated users to `/login`.
- [ ] Add task polling helper for import and analysis task progress.

### Task 3: Layout And Pages

**Files:**
- Create: `aiops-frontend/src/main.ts`
- Create: `aiops-frontend/src/App.vue`
- Create: `aiops-frontend/src/styles/main.css`
- Create: `aiops-frontend/src/layouts/MainLayout.vue`
- Create: `aiops-frontend/src/views/LoginView.vue`
- Create: `aiops-frontend/src/views/DashboardView.vue`
- Create: `aiops-frontend/src/views/CommentWorkbenchView.vue`
- Create: `aiops-frontend/src/views/DataImportView.vue`
- Create: `aiops-frontend/src/views/ProductCompareView.vue`
- Create: `aiops-frontend/src/views/AiContentView.vue`

**Interfaces:**
- Consumes: API modules from Task 2.
- Produces: usable first-screen app and core workflows.

- [ ] Implement login page with default hint `admin / 123456`.
- [ ] Implement merchant cockpit dashboard with overview cards, charts, task area, and recent negative comments.
- [ ] Implement comment workbench with filters, table, tag editing, analysis creation, AI report generation, and negative reply generation.
- [ ] Implement CSV import page with OSS upload and task polling.
- [ ] Implement product comparison page with two selectors and AI comparison report.
- [ ] Implement AI content page with style/content type controls and result history.

### Task 4: Documentation And Verification

**Files:**
- Create: `aiops-frontend/README.md`

**Interfaces:**
- Produces: frontend-backend integration instructions.

- [ ] Document startup order: MySQL/Redis, Python service, Java backend, frontend.
- [ ] Document Vite proxy and `.env` usage.
- [ ] Run `npm install`.
- [ ] Run `npm run typecheck`.
- [ ] Run `npm run build`.
- [ ] Start `npm run dev` and provide local URL.
