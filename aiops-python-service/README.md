# AI Ops Python Service

这是“评论驱动型 AI 智能运营助手”的 Python 内部服务，供 Java 后端调用。当前承担三类能力：

- Olist CSV 数据导入：解析公开数据集 CSV，写入 MySQL 业务表。
- 评论分析：基于评分和文本规则完成清洗、情感标记、关键词提取、问题类型识别和分析结果落库。
- AI 生成：调用 OpenAI 兼容大模型接口，生成运营报告、营销文案、差评回复和商品对比报告。

## 技术栈

- FastAPI
- Pandas
- PyMySQL
- Requests
- python-dotenv

## 本地启动

```powershell
cd C:\Users\o1893\Documents\Codex\2026-08-10\4-ai-python-ai-web-api\aiops-python-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

启动后 Java 后端配置：

```yaml
aiops:
  python:
    base-url: http://localhost:8000
```

## 环境变量

| 变量名 | 说明 | 示例 |
| --- | --- | --- |
| `MYSQL_HOST` | MySQL 地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名 | `aiops` |
| `MYSQL_USER` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | `432` |
| `AI_PROVIDER` | AI 供应商标识 | `deepseek` |
| `AI_BASE_URL` | OpenAI 兼容接口基础地址 | `https://api.deepseek.com` |
| `AI_CHAT_PATH` | Chat Completion 路径 | `/v1/chat/completions` |
| `AI_API_KEY` | 大模型 API Key | 留空则 AI 接口不可用 |
| `AI_MODEL` | 模型名 | `deepseek-chat` |
| `AI_TIMEOUT` | AI 请求超时时间，单位秒 | `30` |
| `CRAWLER_ENABLED` | 是否启用真实爬虫适配器 | `false` |

不要把 `.env` 提交到 Git 仓库。

## Olist 数据目录

CSV 导入接口的 `dataPath` 需要指向 Olist 解压后的目录，目录中至少包含：

- `olist_order_reviews_dataset.csv`
- `olist_order_items_dataset.csv`
- `olist_products_dataset.csv`
- `olist_sellers_dataset.csv`
- `product_category_name_translation.csv`

## 内部接口

### 健康检查

`GET /health`

### Olist CSV 导入

`POST /internal/csv/import`

```json
{
  "taskId": 1,
  "dataSource": "olist",
  "dataPath": "D:/datasets/olist",
  "importMode": "full"
}
```

### 爬虫导入

`POST /internal/crawler/import`

当前不返回模拟数据。未接入真实平台爬虫时会返回失败，避免把假数据写入生产表。

### 评论分析

`POST /internal/analysis/comments`

```json
{
  "taskId": 1,
  "targetType": "product",
  "targetId": "abc123",
  "trendGranularity": "month"
}
```

### AI 运营报告

`POST /internal/ai/report`

返回字段会包在 `data` 中，供 Java 后端生成报告记录。

### AI 文案生成

`POST /internal/ai/content`

返回顶层字段 `generatedContent` 和 `modelName`。

### AI 差评回复

`POST /internal/ai/negative-reply`

返回顶层字段 `replyContent` 和 `modelName`。

### AI 商品对比报告

`POST /internal/ai/product-compare`

```json
{
  "leftProductId": "product-a",
  "rightProductId": "product-b",
  "leftAnalysis": {},
  "rightAnalysis": {},
  "language": "zh-CN"
}
```

返回字段位于 `data` 中：`compareSummary`、`advantageAnalysis`、`riskAnalysis`、`operationSuggestions` 和 `modelName`。
