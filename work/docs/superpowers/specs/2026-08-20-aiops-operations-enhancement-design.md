# AI Ops Operations Enhancement Design

## Goal

Add a focused "一期运营增强包" to the AI intelligent operations assistant so the existing Java backend and Python analysis service expose richer review insights, support manual review tagging, compare two products, and track negative reply effectiveness.

## Selected Scope

This design implements five features from the user's candidate list:

1. 评论智能标签手动编辑。
2. 分析结果增强，完整返回关键词、差评关键词、评分分布、差评原因分布、自定义标签分布。
3. 时间维度趋势分析，按 day/week/month 输出评论数、差评数、差评率、平均评分。
4. 商品 A VS 商品 B 对比分析，包含结构化指标和 AI 对比报告。
5. 差评回复效果跟踪，记录使用次数、收藏状态和效果标记。

The following items are intentionally excluded from this iteration: Excel/PDF export, scheduled alert records, prompt template management,真实平台爬虫增强, SKU-level mining, message queue based task scheduling, and A/B testing.

## Current System Context

The Java backend is a Spring Boot multi-module project:

- `aiops-common`: shared constants, exceptions, result wrappers, Redis keys, JWT utilities.
- `aiops-pojo`: DTO, entity, VO classes.
- `aiops-server`: controllers, services, mappers, Python clients, configuration, SQL initialization.

The Python service is a FastAPI application:

- Imports Olist CSV data into MySQL.
- Cleans review text.
- Tags sentiment from review score.
- Extracts keywords.
- Classifies negative review problem type.
- Calls OpenAI-compatible AI APIs for report/content/reply generation.

Redis is already used for task state, analysis result cache, AI report cache, AI content cache, and AI rate limiting.

## Data Model Changes

### `biz_comment`

Add fields:

- `manual_problem_type varchar(64) null`: merchant-corrected negative review reason.
- `custom_tags json null`: merchant-specific tags for a review, stored as a JSON array of strings.
- `tag_update_time datetime null`: latest manual tag update time.

Existing `problem_type` remains the system-generated classification. Query results will expose both fields.

### `biz_comment_analysis_result`

Add fields:

- `negative_keywords json null`: already present in schema, but Java currently does not expose it.
- `custom_tag_distribution json null`: aggregated counts from `biz_comment.custom_tags`.
- `trend_distribution json null`: list of time bucket metrics.

Trend items use:

```json
{
  "timeBucket": "2018-05",
  "commentCount": 42,
  "negativeCount": 7,
  "negativeRate": 0.1667,
  "avgScore": 4.21
}
```

### `biz_negative_reply`

Add fields:

- `effect_tag varchar(64) null`: `resolved`, `unresolved`, `positive_followup`, `no_feedback`.
- `use_count int not null default 0`: how many times merchant used/copied the reply.
- `favorite_flag tinyint not null default 0`: whether merchant marked it as useful.
- `update_time datetime null`: latest tracking update time.

### `biz_product_compare_report`

Add a new table for comparison history:

- `id bigint primary key auto_increment`
- `left_product_id varchar(64) not null`
- `right_product_id varchar(64) not null`
- `metric_snapshot json null`
- `compare_summary text null`
- `advantage_analysis text null`
- `risk_analysis text null`
- `operation_suggestions text null`
- `model_name varchar(64) null`
- `create_time datetime not null default current_timestamp`

Comparison reports are saved because they are useful as project demo artifacts and avoid repeated AI calls.

## Java API Changes

### Comment Tag Editing

Add:

- `PUT /api/comments/{commentId}/tags`

Request:

```json
{
  "manualProblemType": "logistics",
  "customTags": ["delivery_delay", "important_customer"]
}
```

Response returns the updated `CommentVO`.

`CommentVO` adds:

- `reviewTitle`
- `cleanContent`
- `systemProblemType`
- `manualProblemType`
- `effectiveProblemType`
- `customTags`
- `isNegative`

`effectiveProblemType` is `manualProblemType` when present, otherwise `problemType`.

### Analysis Result Enhancement

Existing endpoints remain:

- `GET /api/analysis/product/{productId}`
- `GET /api/analysis/seller/{sellerId}`

`AnalysisResultVO` adds:

- `negativeKeywords`
- `scoreDistribution`
- `customTagDistribution`
- `trendDistribution`
- `createTime`

Java will parse JSON strings from `biz_comment_analysis_result` into VO lists.

### Dashboard Enhancement

Existing endpoints remain:

- `GET /api/dashboard/product/{productId}`
- `GET /api/dashboard/seller/{sellerId}`

`DashboardVO` adds:

- `negativeKeywordRank`
- `customTagDistribution`
- `trendDistribution`

Java will load the latest analysis result for keyword/trend fields and keep using mapper SQL for score/sentiment/problem/category distributions.

### Product Comparison

Add:

- `POST /api/analysis/products/compare`
- `GET /api/analysis/products/compare`
- `GET /api/analysis/products/compare/{reportId}`

`POST` request:

```json
{
  "leftProductId": "product-a",
  "rightProductId": "product-b",
  "language": "zh-CN",
  "forceRefresh": false
}
```

Java reads the latest analysis result for both products, builds a metrics snapshot, checks Redis cache, calls Python when an AI report is needed, stores the report in MySQL, and returns a `ProductCompareReportVO`.

### Negative Reply Tracking

Add:

- `PUT /api/ai/negative-replies/{replyId}/effect`
- `POST /api/ai/negative-replies/{replyId}/use`
- `PUT /api/ai/negative-replies/{replyId}/favorite`

The history list will return effect fields in `NegativeReplyVO`.

## Python API Changes

### Comment Analysis

`POST /internal/analysis/comments` will continue to analyze a product or seller, and will additionally compute:

- `custom_tag_distribution`
- `trend_distribution`

Trend granularity defaults to `month`, with optional request field:

```json
{
  "trendGranularity": "month"
}
```

Allowed values: `day`, `week`, `month`.

### Product Compare AI

Add:

- `POST /internal/ai/product-compare`

Request:

```json
{
  "leftProductId": "product-a",
  "rightProductId": "product-b",
  "leftAnalysisResult": {},
  "rightAnalysisResult": {},
  "language": "zh-CN"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "compareSummary": "...",
    "advantageAnalysis": "...",
    "riskAnalysis": "...",
    "operationSuggestions": "...",
    "modelName": "deepseek-chat"
  }
}
```

## Error Handling

- Missing comments still return an analysis result with zero counts, not an exception.
- Comment tag editing returns 404 when the comment does not exist.
- Product comparison returns 404 if either product has no analysis result.
- Python AI errors are translated by Java to 503.
- AI comparison uses the existing per-user Redis rate limit.
- `forceRefresh=false` reuses Redis cached comparison report where available.

## Testing

Java:

- Add mapper/service-level tests where practical for JSON parsing helpers and DTO/VO conversion.
- Run `mvn test` for all backend modules.

Python:

- Add tests for trend aggregation and custom tag distribution.
- Run `python -m unittest discover -s tests -v`.
- Run `python -m compileall app tests`.

Manual smoke tests:

- Health check Python `/health`.
- Create or query a product analysis result.
- Edit a comment tag and confirm the effective problem type changes.
- Generate product comparison report.
- Mark a negative reply as used/favorite/effect-tagged.

## Documentation

Update:

- `outputs/AI智能运营助手项目文档.md`
- `outputs/AI智能运营助手接口文档.md`
- `aiops-backend/README.md`
- `aiops-python-service/README.md`

Document only implemented interfaces and fields.
