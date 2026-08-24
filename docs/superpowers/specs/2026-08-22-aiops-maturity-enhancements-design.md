# AIops Maturity Enhancements Design

## Goal

Enhance the current AI operations assistant without changing the existing JWT login flow.

## Scope

- Keep the Java backend, Python FastAPI service, and Vue frontend architecture.
- Keep JWT authentication unchanged.
- Add OpenAPI/Knife4j annotations to Java APIs.
- Add Bucket4j-based AI rate limiting.
- Add Redisson configuration for Redis-backed locks and future distributed features.
- Add Quartz jobs for periodic task maintenance.
- Improve Python keyword extraction while preserving the existing fallback behavior.
- Add topic clustering as an optional Python NLP capability with a deterministic fallback.
- Refactor the crawler module so Scrapy and Crawlee implementations can be added later without changing Java API contracts.

## Non-Goals

- Do not integrate Sa-Token.
- Do not replace the current frontend.
- Do not require a database migration for topic clustering in this iteration.
- Do not make KeyBERT, BERTopic, Scrapy, or Crawlee mandatory runtime dependencies.

## Architecture

Java remains the system of record and API surface. Redis continues to act as a cache and coordination layer. Bucket4j limits AI-heavy entry points per user. Redisson provides Redis lock primitives for scheduled jobs and later distributed execution. Quartz runs lightweight maintenance jobs in the Java backend.

Python remains responsible for text processing and AI calls. Keyword extraction and topic clustering become layered utilities: use richer libraries when installed, otherwise fall back to deterministic local logic that keeps the service runnable on a normal student laptop.

## Verification

- Java unit tests cover AI rate limiting and task maintenance behavior.
- Python tests cover keyword fallback cleanup and topic clustering fallback.
- Full Java test suite, Python test suite, and frontend build/type checks should be run after implementation.
