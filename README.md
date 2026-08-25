# AIOps Comment AI Assistant

[![English README](https://img.shields.io/badge/README-English-blue)](README_EN.md)

面向中小电商商家的评论驱动型 AI 智能运营助手。系统以 Olist 公开电商评论数据为样例，串联 CSV / 爬虫数据导入、评论清洗、情感分析、关键词提取、主题聚类、AI 运营报告、差评回复、商品对比、告警和数据报表，帮助商家把分散评论转化为可执行的运营决策。

> 本项目适合作为大创项目、挑战杯创业计划赛道、课程设计和求职作品集展示。公开数据仅用于学习研究和原型验证，真实落地应接入商家授权数据或平台合规开放接口。

## 目录

- [项目亮点](#项目亮点)
- [功能模块](#功能模块)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [环境配置](#环境配置)
- [常用测试](#常用测试)
- [数据集](#数据集)
- [项目文档](#项目文档)
- [安全说明](#安全说明)

## 项目亮点

- **评论驱动运营决策**：围绕商品评论完成情感、关键词、问题类型、趋势和风险分析。
- **Java + Python 双服务架构**：Java 负责业务、鉴权、任务、缓存和接口编排，Python 负责数据清洗、NLP 和 AI 生成。
- **真实可运行版本**：不依赖模拟数据，支持 MySQL、Redis、阿里云 OSS、DeepSeek / OpenAI 兼容大模型接口。
- **中英葡三语界面**：适配国际商家场景，前端界面和 AI 请求语言可切换。
- **工程化能力完整**：MyBatis-Plus、Quartz、Redis 缓存、Bucket4j 限流、Redisson 预留、Knife4j 接口文档。
- **低成本可演示**：以 Kaggle Olist 数据集完成导入、分析、报表和 AI 生成闭环。

## 功能模块

| 模块 | 功能 |
| --- | --- |
| 商家驾驶舱 | 商品数、商家数、评论数、平均评分、负面率、趋势图和风险速览 |
| 数据导入 | 本地 Olist 目录导入、单 CSV 上传到阿里云 OSS 后导入、低频公开样例爬虫导入 |
| 评论分析 | 评论分页筛选、情感识别、差评识别、人工标签编辑、评论按需翻译 |
| AI 生成 | 运营报告、商品标题、详情文案、短视频脚本、促销话术、差评回复 |
| 商品对比 | 商品 A / B 评论痛点、优势短板、风险和运营建议对比 |
| 告警中心 | 负面占比、近期差评数量、重点问题类型告警 |
| 定时同步 | Quartz 动态定时导入、启停配置、立即触发和执行历史 |
| 任务中心 | 聚合导入、爬虫、分析和同步任务，支持筛选、详情、重试和 CSV 导出 |
| 数据报表 | 全局趋势、情感分布、问题分布、商品排行和 CSV 导出 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、ECharts、Pinia、Vue Router、vue-i18n、Axios |
| Java 后端 | Java 21、Spring Boot 3.3、Spring MVC、MyBatis-Plus、Knife4j / OpenAPI |
| Python 服务 | FastAPI、Pandas、PyMySQL、Requests、规则 NLP、关键词提取、主题聚类 |
| 数据存储 | MySQL 8.x、Redis |
| 文件存储 | 阿里云 OSS |
| AI 能力 | DeepSeek 或其他 OpenAI 兼容 Chat Completion API |
| 工程增强 | Quartz 定时任务、Bucket4j AI 限流、Redisson 分布式能力预留 |

## 系统架构

```text
Vue 3 Frontend
  |
  | REST API / JWT
  v
Spring Boot Backend
  |-- Auth / User / Product / Seller / Comment
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

## 项目结构

```text
.
├── aiops-backend/              # Spring Boot 多模块后端
│   ├── aiops-common/           # 公共返回、常量、异常、配置属性、JWT 工具
│   ├── aiops-pojo/             # Entity、DTO、VO
│   └── aiops-server/           # Controller、Service、Mapper、SQL、任务调度
├── aiops-frontend/             # Vue 3 管理端
│   ├── src/api/                # Axios 和业务 API 封装
│   ├── src/views/              # 业务页面
│   ├── src/i18n/               # 中英葡三语文案
│   └── tests/                  # 前端结构与工具测试
├── aiops-python-service/       # FastAPI 评论分析与 AI 服务
│   ├── app/routers/            # 内部接口
│   ├── app/services/           # 导入、爬虫、分析、AI 生成服务
│   └── tests/                  # Python 服务测试
└── outputs/                    # 正式项目文档和接口文档
```

## 快速开始

### 1. 准备环境

- JDK 21
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- MySQL 8.x
- Redis 6+

### 2. 准备数据库和 Redis

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS aiops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动 MySQL 和 Redis 后，Java 后端首次启动会按 `aiops-backend/aiops-server/src/main/resources/sql/schema.sql` 和 `data.sql` 初始化基础表和默认管理员。

### 3. 配置 Java 后端

后端默认使用 `dev` profile。敏感配置建议放在本地 `application-secret.yml` 或环境变量中，不要提交到 GitHub。

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

启动后端：

```powershell
cd aiops-backend
mvn -pl aiops-server -am spring-boot:run
```

默认地址：

- Backend API: `http://localhost:8080/api`
- Knife4j: `http://localhost:8080/doc.html`

### 4. 配置 Python 服务

复制环境变量示例：

```powershell
cd aiops-python-service
Copy-Item .env.example .env
```

编辑 `.env`，至少填写 MySQL 和 AI 配置：

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

启动 Python 服务：

```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### 5. 启动前端

复制前端环境变量示例：

```powershell
cd aiops-frontend
Copy-Item .env.example .env.local
npm install
npm run dev
```

默认前端地址：

- Frontend: `http://localhost:5173`

如果端口被占用，Vite 会自动切到下一个端口，例如 `5174`。

## 环境配置

### Java 后端关键配置

| 配置 | 说明 |
| --- | --- |
| `AIOPS_MYSQL_PASSWORD` | MySQL 密码 |
| `AIOPS_JWT_SECRET` | JWT 签名密钥 |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey ID |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret |
| `aiops.config.python.base-url` | Python 服务地址，默认本地开发为 `http://localhost:8001` |

### Python 服务关键配置

| 配置 | 说明 |
| --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` | MySQL 地址和端口 |
| `MYSQL_DATABASE` | 数据库名 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库账号和密码 |
| `AI_BASE_URL` | OpenAI 兼容接口地址 |
| `AI_API_KEY` | 大模型 API Key |
| `AI_MODEL` | 模型名称 |
| `CRAWLER_ENABLED` | 是否启用真实爬虫适配器 |

### 前端配置

| 配置 | 说明 |
| --- | --- |
| `VITE_API_BASE_URL` | 浏览器请求前缀，默认 `/api` |
| `VITE_BACKEND_URL` | Vite 本地开发代理目标，默认 `http://localhost:8080` |

## 常用测试

Java 后端：

```powershell
cd aiops-backend
mvn -q -pl aiops-server -am test
```

Python 服务：

```powershell
cd aiops-python-service
pytest
```

前端：

```powershell
cd aiops-frontend
npm run test:navigation
npm run build
```

## 数据集

项目使用 Olist Brazilian E-Commerce Public Dataset：

- Kaggle: https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce

完整 Olist 导入建议使用本地目录方式，目录中至少包含：

- `olist_order_reviews_dataset.csv`
- `olist_orders_dataset.csv`
- `olist_order_items_dataset.csv`
- `olist_products_dataset.csv`
- `olist_sellers_dataset.csv`
- `olist_customers_dataset.csv`
- `product_category_name_translation.csv`

浏览器上传适合已经合并好的单 CSV 评论数据，字段至少包含：

- `product_id`
- `review_score`

## 项目文档

- [项目文档](outputs/AI智能运营助手项目文档.md)
- [接口文档](outputs/AI智能运营助手接口文档.md)

## 安全说明

- 不要提交 `.env`、`.env.local`、`application-secret.yml`、真实 API Key、OSS Key 或数据库密码。
- 当前爬虫能力仅用于学习研究和低频原型演示，不应绕过登录、验证码、付费墙或平台访问控制。
- AI 结果用于辅助运营决策，不能承诺保证爆款、稳赚不赔或替代人工审核。
- 上线部署时建议使用 Nginx 或网关统一代理 `/api/**`，并使用 HTTPS、强密码和独立 RAM 子账号管理 OSS 权限。
