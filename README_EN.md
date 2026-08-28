# AIOps Comment AI Assistant

[![Chinese README](https://img.shields.io/badge/README-Chinese-blue)](README.md)

A review-driven AI operations assistant for small and medium-sized e-commerce merchants. The system uses the public Olist e-commerce review dataset as a demo source and connects CSV / crawler import, review cleaning, sentiment analysis, keyword extraction, topic clustering, AI operation reports, negative review replies, product comparison, alerts, and data reports. It helps merchants turn scattered customer reviews into actionable operation decisions.

> This project is suitable for AI + e-commerce operation course projects, entrepreneurship prototypes, and portfolio demonstrations. Public data is used only for learning, research, and prototype validation. Real deployment should use merchant-authorized data or compliant platform APIs.

## Table of Contents

- [Highlights](#highlights)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Common Tests](#common-tests)
- [Dataset](#dataset)
- [Documentation](#documentation)
- [Security Notes](#security-notes)

## Highlights

- **Review-driven operation decisions**: analyzes sentiment, keywords, issue types, trends, and risks around product reviews.
- **Java + Python dual-service architecture**: Java handles business logic, authentication, tasks, cache, and API orchestration; Python handles data cleaning, NLP, and AI generation.
- **Production-oriented runnable version**: supports MySQL, Redis, Alibaba Cloud OSS, DeepSeek / OpenAI-compatible model APIs, and a public sample-data workflow.
- **Reusable operations knowledge base**: merchants can maintain a custom tag library and a problem solution library, turning manual review corrections into reusable assets.
- **Controllable prompts and AI cost visibility**: supports business-type prompt templates and records AI calls, token estimates, latency, estimated cost, and failure reasons.
- **Chinese, English, and Portuguese UI**: designed for international merchants, with switchable frontend language and AI request language.
- **Deliverable operations reports**: archived reports can be exported as localized Chinese, English, or Portuguese PDFs.
- **Practical engineering stack**: MyBatis-Plus, Quartz, Redis cache, Bucket4j rate limiting, Redisson-ready design, and Knife4j API documentation.
- **One-command deployment**: Docker Compose provisions the frontend, Java backend, Python service, MySQL, and Redis together.
- **Low-cost demo flow**: uses the Kaggle Olist dataset to complete import, analysis, reporting, and AI generation.

## Features

| Module | Features |
| --- | --- |
| Merchant Dashboard | Product count, seller count, review count, average score, negative rate, trend charts, and risk overview |
| Data Import | Local Olist directory import, single CSV preview / field mapping / OSS import, one-click sample data import, and low-frequency public sample crawler import |
| Review Analysis | Review pagination and filtering, sentiment detection, negative review detection, manual tag editing, on-demand translation, and one-click analysis with report generation |
| Tag Library | Custom tag management, grouping, color, enabled status, and direct selection in the review tag dialog |
| Solution Library | Solutions by issue type and category, with reusable suggestions in the review workspace |
| Prompt Templates | Default prompts by business type, including reports, copywriting, negative replies, translation, and product comparison |
| AI Call Logs | AI call volume, success rate, token estimates, cost estimates, latency, and errors |
| AI Generation | Operation reports, product titles, detail copy, short video scripts, promotion copy, and negative review replies |
| Categories and Archives | Category-level review risk aggregation plus report snapshots, filtering, detail review, restore status, and localized PDF export |
| Product Comparison | Product A / B review pain points, strengths, weaknesses, risks, and operation suggestions |
| Alert Center | Alerts for negative review ratio, recent negative review count, and key issue types |
| Scheduled Sync | Quartz-based dynamic import schedules, enable / disable controls, manual trigger, and execution history |
| Task Center | Aggregated import, crawler, analysis, and sync tasks with filtering, details, retry, and CSV export |
| Data Reports | Global trends, sentiment distribution, issue distribution, product rankings, CSV export, and archived-report PDF export |

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Frontend | Vue 3, TypeScript, Vite, Element Plus, ECharts, Pinia, Vue Router, vue-i18n, Axios |
| Java Backend | Java 21, Spring Boot 3.3, Spring MVC, MyBatis-Plus, OpenPDF, Knife4j / OpenAPI |
| Python Service | FastAPI, Pandas, PyMySQL, Requests, rule-based NLP, keyword extraction, topic clustering |
| Storage | MySQL 8.x, Redis |
| File Storage | Alibaba Cloud OSS |
| AI Provider | DeepSeek or any OpenAI-compatible Chat Completion API |
| Engineering Add-ons | Quartz scheduled jobs, Bucket4j AI rate limiting, Redisson-ready distributed design, Docker Compose, Nginx |

## Architecture

```text
Vue 3 Frontend
  |
  | REST API / JWT
  v
Spring Boot Backend
  |-- Auth / User / Product / Seller / Comment
  |-- Custom Tag Library / Problem Solution Library
  |-- Prompt Template / AI Call Log
  |-- Import Task / Analysis Task / Sync Task
  |-- AI Report / Copywriting / Negative Reply / Product Compare
  |-- Redis Cache / Bucket4j Rate Limit / Quartz Schedule
  |
  | SQL
  v
MySQL

Spring Boot Backend
  |
  | Internal HTTP
  v
Python FastAPI Service
  |-- Olist CSV Import
  |-- Text Cleaning
  |-- Sentiment and Problem Classification
  |-- Keyword Extraction and Topic Clustering
  |-- AI Provider Request

Frontend / Backend
  |
  v
Alibaba Cloud OSS
```

## Project Structure

```text
.
|-- aiops-backend/              # Spring Boot multi-module backend
|   |-- aiops-common/           # Common result, constants, exceptions, properties, JWT utilities
|   |-- aiops-pojo/             # Entity, DTO, VO
|   `-- aiops-server/           # Controller, Service, Mapper, SQL, scheduled tasks
|-- aiops-frontend/             # Vue 3 admin frontend
|   |-- src/api/                # Axios and business API wrappers
|   |-- src/views/              # Business pages
|   |-- src/i18n/               # Chinese, English, and Portuguese translations
|   `-- tests/                  # Frontend navigation and utility tests
|-- aiops-python-service/       # FastAPI review analysis and AI service
|   |-- app/routers/            # Internal APIs
|   |-- app/services/           # Import, crawler, analysis, and AI generation services
|   |-- app/sample_data/         # Built-in small Olist review sample
|   `-- tests/                  # Python service tests
|-- compose.yml                 # Five-service Docker Compose stack
|-- .env.example                # Deployment environment template
`-- outputs/                    # Project document and API document
```

## Quick Start

### Docker Compose

Install Docker Desktop or Docker Engine with the Compose plugin, then run:

```powershell
git clone https://github.com/demolition-man1/aiops-comment-ai-assistant.git
cd aiops-comment-ai-assistant
Copy-Item .env.example .env
```

Edit the local `.env` file and replace at least the MySQL password and JWT secret. Add `AI_API_KEY` when AI generation is required. Start the full stack:

```powershell
docker compose up -d --build
docker compose ps
```

Default endpoints:

- Web application: `http://localhost:5174`
- Backend API: `http://localhost:8080/api`
- Knife4j: `http://localhost:8080/doc.html`
- Python health check: `http://localhost:8001/health`

The local seed administrator is `admin / 123456` for the first development login only. Change this password immediately before any public deployment.

Inspect logs or stop the stack:

```powershell
docker compose logs -f backend python-service
docker compose down
```

MySQL and Redis use named volumes, so a regular `docker compose down` keeps business data.

### Manual Development Setup

#### 1. Prepare the Environment

- JDK 21
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- MySQL 8.x
- Redis 6+

#### 2. Prepare MySQL and Redis

Create the database:

```sql
CREATE DATABASE IF NOT EXISTS aiops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

After MySQL and Redis are running, the Java backend initializes base tables and the default admin account from `aiops-backend/aiops-server/src/main/resources/sql/schema.sql` and `data.sql` on first startup.

#### 3. Configure the Java Backend

The backend uses the `dev` profile by default. Put sensitive values in a local `application-secret.yml` file or environment variables. Do not commit them to GitHub.

```yaml
aiops:
  config:
    datasource:
      password: ${AIOPS_MYSQL_PASSWORD}
    jwt:
      secret: ${AIOPS_JWT_SECRET}
    aliyun:
      oss:
        endpoint: https://oss-cn-shenzhen.aliyuncs.com
        bucket-name: your-bucket-name
        access-key-id: ${ALIYUN_ACCESS_KEY_ID}
        access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
```

Start the backend:

```powershell
cd aiops-backend
mvn -pl aiops-server -am spring-boot:run
```

Default URLs:

- Backend API: `http://localhost:8080/api`
- Knife4j: `http://localhost:8080/doc.html`

#### 4. Configure the Python Service

Copy the environment example:

```powershell
cd aiops-python-service
Copy-Item .env.example .env
```

Edit `.env` and fill in at least the MySQL and AI settings:

```env
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=aiops
MYSQL_USER=root
MYSQL_PASSWORD=your-local-password

AI_PROVIDER=deepseek
AI_BASE_URL=https://api.deepseek.com
AI_CHAT_PATH=/v1/chat/completions
AI_API_KEY=your-ai-api-key
AI_MODEL=deepseek-chat
```

Start the Python service:

```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

#### 5. Start the Frontend

Copy the frontend environment example:

```powershell
cd aiops-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

Default frontend URL:

- Frontend: `http://localhost:5173`

If the port is occupied, Vite automatically switches to the next available port, such as `5174`.

After logging in, open the Data Import page and click "Import Sample Data" to quickly create demo data. For single CSV upload, the page previews field mapping, estimated rows, and the first 20 rows before upload. OSS upload and task creation happen only after clicking "Start Import". After import, maintain business tags in "Tag Library", maintain handling playbooks in "Solution Library", and customize AI instructions in "Prompt Templates". In "Review Analytics", run analysis only or click "Analyze and Generate Report" to complete analysis and localized report generation in one flow. Archive the report and open "Data Reports" to export a PDF in the active Chinese, English, or Portuguese locale. Use "AI Call Logs" to review call volume, success rate, tokens, and estimated cost.

## Configuration

### Java Backend Key Configuration

| Config | Description |
| --- | --- |
| `AIOPS_MYSQL_PASSWORD` | MySQL password |
| `AIOPS_JWT_SECRET` | JWT signing secret |
| `AIOPS_PDF_FONT_PATH` | Optional custom CJK font path for PDF generation |
| `ALIYUN_OSS_ACCESS_KEY_ID` | Alibaba Cloud AccessKey ID |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | Alibaba Cloud AccessKey Secret |
| `aiops.config.python.base-url` | Python service URL, defaults to `http://localhost:8001` in local development |

### Python Service Key Configuration

| Config | Description |
| --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` | MySQL host and port |
| `MYSQL_DATABASE` | Database name |
| `MYSQL_USER` / `MYSQL_PASSWORD` | Database username and password |
| `AI_BASE_URL` | OpenAI-compatible API base URL |
| `AI_API_KEY` | LLM API key |
| `AI_MODEL` | Model name |
| `CRAWLER_ENABLED` | Whether to enable the real crawler adapter |

### Frontend Configuration

| Config | Description |
| --- | --- |
| `VITE_API_BASE_URL` | Browser request prefix, defaults to `/api` |
| `VITE_BACKEND_URL` | Vite local development proxy target, defaults to `http://localhost:8080` |

## Common Tests

Java backend:

```powershell
cd aiops-backend
mvn -q -pl aiops-server -am test
```

Python service:

```powershell
cd aiops-python-service
pytest
```

The Python test suite also verifies the deployment-file contract. With Docker installed, validate Compose interpolation with:

```powershell
docker compose --env-file .env.example config
```

Frontend:

```powershell
cd aiops-frontend
npm run test:navigation
npm run build
```

## Dataset

The project uses the Olist Brazilian E-Commerce Public Dataset:

- Kaggle: https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce

For full Olist import, the local directory should contain at least:

- `olist_order_reviews_dataset.csv`
- `olist_orders_dataset.csv`
- `olist_order_items_dataset.csv`
- `olist_products_dataset.csv`
- `olist_sellers_dataset.csv`
- `olist_customers_dataset.csv`
- `product_category_name_translation.csv`

Browser upload is suitable for an already merged single CSV review file. The page can map merchant-exported column names to standard fields. At minimum, map:

- `product_id`
- `review_score`

Optional mapped fields include `review_content`, `review_title`, `review_id`, `order_id`, `seller_id`, and `review_time`.

## Documentation

- [Project Document](outputs/AI智能运营助手项目文档.md)
- [API Document](outputs/AI智能运营助手接口文档.md)

## Security Notes

- Do not commit `.env`, `.env.local`, `application-secret.yml`, real API keys, OSS keys, or database passwords.
- Change the seeded administrator password before public deployment, and use separate strong secrets for MySQL, JWT, and Redis.
- The crawler capability is only for learning, research, and low-frequency prototype demos. It should not bypass login, CAPTCHA, paywalls, or platform access controls.
- AI output is only an operation decision aid. It cannot guarantee viral products, guaranteed profit, or replace human review.
- For production deployment, use Nginx or an API gateway to proxy `/api/**`, enable HTTPS, use strong passwords, and manage OSS permissions with a dedicated RAM sub-account.
