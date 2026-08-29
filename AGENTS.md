# AGENTS.md

## Project Purpose

This repository contains an AI operations assistant for small and medium-sized
ecommerce merchants. It analyzes customer reviews and provides operational
reports, copywriting, personalized negative-review replies, translations,
product comparisons and category-level insights.

The Olist Brazilian Ecommerce dataset is used for development and demonstration.

## Repository Structure

- `aiops-backend/`: Java backend, REST APIs, JWT authentication, business logic,
  MyBatis-Plus mappers, MySQL, Redis, Quartz and Aliyun OSS integration.
- `aiops-python-service/`: FastAPI service for CSV import, crawler adapters,
  text cleaning, sentiment analysis, keyword extraction, topic clustering and
  AI provider calls.
- `aiops-frontend/`: Vue 3, TypeScript, Vite, Element Plus and ECharts frontend.
- `docs/`: project and API documentation and local phase tracking.
- `outputs/`: intentionally versioned documents only; generated runtime output
  must remain local.

## Technology Baseline

- Backend: Java 21, Spring Boot 3.3.x, MyBatis-Plus, MySQL 8.x, Redis,
  Quartz, JWT, Aliyun OSS and Knife4j/OpenAPI.
- Python: Python 3.11+, FastAPI, Pandas, PyMySQL, Requests, LangChain Core,
  LangChain DeepSeek and Pytest.
- Frontend: Vue 3, TypeScript, Vite, Axios, Element Plus, Pinia, Vue I18n and
  ECharts.
- Optional Python packages: KeyBERT, BERTopic, Scrapy and Crawlee. Keep them
  optional unless the current task requires them.

Vector databases, embeddings and RAG are planned capabilities, not current
capabilities. Do not document or report them as implemented until the code and
tests support them.

## Local Services

Default local ports:

- Frontend: `5174`
- Java backend: `8080`
- Python service: `8001`
- Native MySQL: `3306`
- Docker MySQL from Windows: `3307` (containers use `mysql:3306`)
- Redis: `6379`

The normal request path is:

```text
Frontend -> Java backend -> Python service
```

The frontend should call public Java APIs. It must not call Python internal
endpoints directly during normal application flows.

Do not start, stop or restart services unless the user explicitly asks. When
services are started or stopped, report the relevant ports and status.

## Configuration and Secrets

Never commit real credentials or private configuration. This includes:

- MySQL and Redis passwords
- JWT secrets
- Aliyun OSS AccessKey ID and AccessKey Secret
- DeepSeek or other AI provider API keys
- Personal access tokens and authorization headers
- Production URLs, private callback URLs and infrastructure addresses

Use environment variables, ignored local files and placeholder values in
`.env.example` or configuration templates. Do not log credentials, full bearer
tokens, passwords, JWT secrets or sensitive merchant/customer data.

Before committing, inspect the complete diff for secrets, local absolute paths,
debug dumps, generated logs, datasets and temporary output files.

## General Development Rules

- Read the relevant implementation and documentation before changing code.
- Search for an existing equivalent before creating a class, endpoint, utility,
  component, store or helper.
- Preserve user changes in the working tree.
- Do not use `git reset --hard`, destructive checkout operations or broad file
  deletion unless explicitly approved.
- Use focused, reviewable edits and avoid unrelated refactoring.
- Prefer existing project conventions over new architectural patterns.
- Maintain backward compatibility unless a breaking change is requested.
- Add or update tests when behavior changes.
- Update user-facing or developer-facing documentation when contracts change.
- Do not claim success before relevant verification is complete.

## Backend Rules

Use the established layering:

```text
Controller -> Service -> Mapper -> Database
```

- Keep controllers thin.
- Put business logic in services.
- Use MyBatis-Plus mappers for database access.
- Keep Entity, DTO and VO responsibilities separate.
- Use JWT authentication and the existing authorization model.
- Keep Python internal endpoints separate from merchant-facing APIs.
- Use Redis for task status, rate limiting and cache data where the existing
  design already does so.
- Use Quartz for configurable scheduled synchronization.
- Preserve the existing response wrapper and exception conventions.
- Do not expose stack traces or database details to frontend users.
- Prefer existing constants and enums over new magic values.

## Database Rules

- Do not delete tables or columns without explicit approval.
- Prefer additive and backward-compatible schema changes.
- Keep Java entities aligned with the actual schema.
- Make migration scripts explicit and reviewable.
- Document schema changes and update related tests.
- Do not truncate production-like data during normal development.
- Do not silently change the meaning of an existing column or index.

## Python Rules

- Keep FastAPI routers thin; put business logic in services.
- Use typed request and response models for new endpoints.
- Validate external input before database, crawler or AI operations.
- Keep AI provider calls behind a dedicated service boundary.
- Use explicit connection and read timeouts for external requests.
- Retry only temporary provider failures; do not retry validation or
  authentication failures unnecessarily.
- Prefer schema-validated structured AI output.
- Handle empty, malformed and partial AI responses safely.
- Keep crawler implementations low-frequency and narrowly scoped.
- Follow target platform terms, access restrictions and applicable rules.
- Keep heavy NLP and browser dependencies isolated from lightweight flows.

## AI Integration Rules

- Reuse the existing database-backed Prompt template system.
- Preserve Prompt template variables unless the API contract intentionally
  changes.
- Do not silently change the configured AI provider or model.
- Send only the minimum merchant/customer data required for a task.
- Validate model output before storing or returning it.
- Preserve readable errors for provider timeouts, rate limits and failures.
- Record provider, model, status, latency, token usage and cost when available.
- Do not bypass the existing AI call logging or rate-limiting mechanisms.

## Frontend Rules

- Keep API calls in `src/api`.
- Keep authentication and locale state in stores.
- Use the existing Chinese, English and Portuguese i18n system.
- Do not hard-code user-facing text when a locale key should be added.
- Provide loading, empty, success and readable error states.
- Keep TypeScript API types aligned with backend response structures.
- Preserve existing navigation and responsive layouts.
- Do not expose backend, Python, OSS or AI credentials in frontend code.
- Reuse shared components before creating duplicates.

## Cross-Module API Verification

For changes spanning modules, check all of the following:

1. Java request fields match Python input fields.
2. Java serialization names match Python models.
3. Python response fields match Java parsing and VO structures.
4. Java response fields match frontend TypeScript types.
5. Nullability, enum values and date formats are consistent.
6. Authentication and authorization behavior remains correct.
7. Error responses remain understandable.
8. Existing callers and language switching still work.

Search every caller, parser, type definition and test before changing a public
or internal API contract.

## Documentation Rules

- Document the actual current implementation.
- Do not present planned behavior as implemented.
- Verify routes, methods, request fields, response fields, auth requirements,
  configuration names and port numbers against code.
- Use placeholders instead of real secrets.
- Avoid machine-specific absolute paths in committed documentation.
- Use the `no-negative-echo` skill when it is available before writing or
  substantially revising project documentation.
- Phase plans, progress trackers and temporary process notes are local-only
  unless the user explicitly asks to commit or push them.

## Verification Commands

Run the relevant checks for every affected module.

Backend, from `aiops-backend/`:

```bash
mvn test
```

Python, from `aiops-python-service/`:

```bash
pytest
```

Frontend, from `aiops-frontend/`:

```bash
npm run typecheck
npm run test:navigation
npm run build
```

Do not invent package scripts; inspect `package.json` when the command is
uncertain. For cross-module changes, test every affected module and run:

```bash
git diff --check
```

## Git Conventions

Use focused English Conventional Commit messages:

- `feat: add ...`
- `fix: resolve ...`
- `docs: update ...`
- `refactor: improve ...`
- `test: add ...`
- `chore: update ...`

Before committing:

1. Run `git status`.
2. Review `git diff` and, when relevant, `git diff --cached`.
3. Confirm no secrets, local configuration, datasets, logs or temporary files
   are included.
4. Confirm unrelated user changes are untouched.

Do not rewrite history or force-push unless explicitly instructed. Push only
according to the user's current instruction.

## Completion Checklist

- [ ] Relevant code and similar implementations were inspected.
- [ ] The requested behavior is implemented without unrelated changes.
- [ ] Affected tests pass.
- [ ] Cross-module contracts remain consistent.
- [ ] Database changes are intentional and documented.
- [ ] Documentation matches the actual implementation.
- [ ] No secrets or sensitive values are in the diff or logs.
- [ ] No generated datasets, logs or temporary outputs are included.
- [ ] `git diff --check` passes.
- [ ] Git status and commit contents were reviewed.
- [ ] Any unverified item is reported explicitly.
