# AIOps Comment AI Assistant

[![English README](https://img.shields.io/badge/README-English-blue)](README_EN.md)

面向中小电商商家的评论驱动型 AI 智能运营助手。系统以 Olist 公开电商评论数据为样例，串联 CSV / 爬虫数据导入、评论清洗、情感分析、关键词提取、主题聚类、AI 运营报告、差评回复、商品对比、告警和数据报表，帮助商家把分散评论转化为可执行的运营决策。

> 本项目适合作为 AI + 电商运营方向的课程设计、创业原型和求职作品集展示。公开数据仅用于学习研究和原型验证，真实落地应接入商家授权数据或平台合规开放接口。

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
- **真实可运行版本**：支持 MySQL、Redis、阿里云 OSS、DeepSeek / OpenAI 兼容大模型接口和公开示例数据闭环。
- **可沉淀运营知识库**：支持商家维护自定义标签库和问题解决方案库，把人工校正转化为复用资产。
- **Prompt 与成本可控**：支持按业务类型维护 Prompt 模板，并记录 AI 调用次数、token 估算、耗时和失败原因。
- **中英葡三语界面**：适配国际商家场景，前端界面和 AI 请求语言可切换。
- **可交付运营报告**：归档报告可按当前界面语言导出为中、英、葡语 PDF。
- **工程化能力完整**：MyBatis-Plus、Quartz、Redis 缓存、Bucket4j 限流、Redisson 预留、Knife4j 接口文档。
- **一键部署**：提供前端、Java、Python、MySQL、Redis 的 Docker Compose 完整编排。
- **低成本可演示**：以 Kaggle Olist 数据集完成导入、分析、报表和 AI 生成闭环。

## 功能模块

| 模块 | 功能 |
| --- | --- |
| 商家驾驶舱 | 商品数、商家数、评论数、平均评分、负面率、趋势图和风险速览 |
| 数据导入 | 本地 Olist 目录导入、单 CSV 预览/字段映射/OSS 导入、示例数据一键导入、低频公开样例爬虫导入 |
| 评论分析 | 评论分页筛选、情感识别、差评识别、人工标签编辑、评论按需翻译、一键分析并生成运营报告 |
| 评论 AI Shadow | 独立抽样对照运行、样本回看、人工情感与问题标签标注、规则与 AI 的质量评估，以及通过门槛后的 Hybrid 问题标签受控启用 |
| 标签库 | 自定义标签维护、分组、颜色、启停状态，并在评论标签弹窗中直接选择 |
| 方案库 | 按问题类型和类目维护解决方案，评论工作台可推荐可复用处理方案 |
| Prompt 模板 | 按报告、文案、差评回复、翻译和商品对比等业务类型维护默认 Prompt |
| AI 调用日志 | 统计 AI 调用总量、成功率、token 估算、成本估算、耗时和异常信息 |
| AI 生成 | 运营报告、商品标题、详情文案、短视频脚本、促销话术、差评回复 |
| 类目与归档 | 类目级评论风险聚合；运营报告快照归档、组合筛选、详情回看、状态恢复和三语 PDF 导出 |
| 商品对比 | 商品 A / B 评论痛点、优势短板、风险和运营建议对比 |
| 告警中心 | 负面占比、近期差评数量、重点问题类型告警 |
| 定时同步 | Quartz 动态定时导入、启停配置、立即触发和执行历史 |
| 任务中心 | 聚合导入、爬虫、分析和同步任务，支持筛选、详情、重试和 CSV 导出 |
| 数据报表 | 全局趋势、情感分布、问题分布、商品排行、CSV 导出和归档 PDF 导出 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、ECharts、Pinia、Vue Router、vue-i18n、Axios |
| Java 后端 | Java 21、Spring Boot 3.3、Spring MVC、MyBatis-Plus、OpenPDF、Knife4j / OpenAPI |
| Python 服务 | FastAPI、Pandas、PyMySQL、Requests、规则 NLP、关键词提取、主题聚类 |
| 数据存储 | MySQL 8.x、Redis |
| 文件存储 | 阿里云 OSS |
| AI 能力 | DeepSeek 或其他 OpenAI 兼容 Chat Completion API |
| 工程增强 | Quartz 定时任务、Bucket4j AI 限流、Redisson 分布式能力预留、Docker Compose、Nginx |

## 系统架构

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
│   ├── app/sample_data/         # 内置小型 Olist 示例评论数据
│   └── tests/                  # Python 服务测试
├── compose.yml                 # 五服务 Docker Compose 编排
├── .env.example                # 一键部署环境变量模板
└── outputs/                    # 正式项目文档和接口文档
```

## 快速开始

### Docker Compose 一键启动

准备 Docker Desktop 或 Docker Engine + Compose 插件，然后执行：

```powershell
git clone https://github.com/demolition-man1/aiops-comment-ai-assistant.git
cd aiops-comment-ai-assistant
Copy-Item .env.example .env
```

编辑本地 `.env`，至少替换 MySQL 密码和 JWT 密钥；需要调用大模型时再填写 `AI_API_KEY`。随后启动完整服务：

```powershell
docker compose up -d --build
docker compose ps
```

启动完成后访问：

- Web 管理端：`http://localhost:5174`
- Backend API：`http://localhost:8080/api`
- Knife4j：`http://localhost:8080/doc.html`
- Python 健康检查：`http://localhost:8001/health`
- Docker MySQL：`localhost:3307`（容器内部仍为 `mysql:3306`）

本地初始化管理员为 `admin / 123456`，仅用于首次开发体验；公开部署后应立即修改默认密码。

查看日志或停止服务：

```powershell
docker compose logs -f backend python-service
docker compose down
```

MySQL、Redis 与 RAG 本地索引数据均保存在命名卷中，普通 `docker compose down` 不会删除业务数据或已下载的嵌入模型缓存。

### 可选：启用本地 RAG 知识库

RAG 默认关闭。开启后，系统会把已启用的问题解决方案和符合条件的历史差评回复构建为本地 Chroma 索引，用于生成可追溯来源的差评回复；业务事实仍只保存在 MySQL。

1. 在根目录本地 `.env` 中设置 `RAG_ENABLED=true`。
2. 重建 Python 服务：

```powershell
docker compose up -d --force-recreate python-service
```

3. 登录 Web 管理端，在“问题解决方案库”查看知识索引状态并点击“重建索引”。只有这次明确点击才会启动重建。
4. 状态变为 `ready` 后，在“评论分析”生成差评回复；页面会展示本次回复引用的解决方案或合格历史回复。重新生成运营报告后，在“数据报表”打开报告或归档详情，可查看并跳转其评论证据和解决方案来源。

首次重建会下载 `intfloat/multilingual-e5-small` 嵌入模型，需有网络连接，下载时间取决于网络环境。模型缓存和 Chroma 索引保存于 Docker 的 `rag-data` 命名卷；重建 `python-service` 不需要再次建立索引。系统不会新增 Chroma 对外端口。

如需回退到原有非 RAG 回复流程，将 `.env` 中的 `RAG_ENABLED` 改回 `false`，然后再次执行 `docker compose up -d --force-recreate python-service`。该操作不会删除 MySQL 中的方案库、历史回复或已保存的回复记录。

### 手动启动

#### 1. 准备环境

- JDK 21
- Maven 3.9+
- Node.js 20+
- Python 3.11+
- MySQL 8.x
- Redis 6+

#### 2. 准备数据库和 Redis

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS aiops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动 MySQL 和 Redis 后，Java 后端首次启动会按 `aiops-backend/aiops-server/src/main/resources/sql/schema.sql` 和 `data.sql` 初始化基础表和默认管理员。

#### 3. 配置 Java 后端

后端默认使用 `dev` profile。敏感配置建议放在本地 `application-secret.yml` 或环境变量中。

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

#### 4. 配置 Python 服务

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
AI_MODEL=deepseek-v4-flash
AI_NEGATIVE_REPLY_ENGINE=langchain
AI_NEGATIVE_REPLY_THINKING_ENABLED=false
AI_NEGATIVE_REPLY_MAX_TOKENS=320
AI_MAX_RETRIES=2
```

启动 Python 服务：

```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

#### 5. 启动前端

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

登录后进入“数据导入”页面，可以先点击“一键导入示例数据”快速生成可演示数据；上传单个 CSV 时，页面会先展示字段映射、预计导入行数和前 20 行预览，点击“开始导入”后才会上传到 OSS 并创建导入任务。完成导入后，可在“标签库”维护业务标签，在“方案库”维护问题处理方案，在“Prompt 模板”维护不同 AI 场景的提示词。进入“评论分析”页面后，可以只运行评论分析，也可以点击“分析并生成报告”自动完成分析任务、结果加载和 AI 运营报告生成。启用 RAG 并重建索引后，新报告会附带当前商品或商家的评论证据和解决方案来源；在“数据报表”的报告或归档详情中可跳转核查。报告归档后，可按当前中、英、葡界面语言导出 PDF。AI 调用后可在“AI 调用日志”页面查看调用量、成功率、token 和成本估算。进入“评论 AI 对照评估”后，完成抽样、人工标注和评估；仅当准入门槛全部通过时，才可显式启用独立的 Hybrid 问题标签决策。

## 环境配置

### Java 后端关键配置

| 配置 | 说明 |
| --- | --- |
| `AIOPS_MYSQL_PASSWORD` | MySQL 密码 |
| `AIOPS_JWT_SECRET` | JWT 签名密钥 |
| `AIOPS_PDF_FONT_PATH` | 可选，自定义 PDF 中日韩字体文件路径 |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 AccessKey ID |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 阿里云 AccessKey Secret |
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
| `AI_NEGATIVE_REPLY_ENGINE` | 差评回复引擎：`langchain`，异常排查时可临时切换为 `legacy` |
| `AI_NEGATIVE_REPLY_THINKING_ENABLED` | 差评回复是否启用模型思考模式，默认 `false`，关闭可缩短短回复生成耗时 |
| `AI_NEGATIVE_REPLY_MAX_TOKENS` | 差评回复最大输出 Token，范围 `64` 至 `1024`，默认 `320` |
| `AI_MAX_RETRIES` | AI provider 临时错误的最大重试次数，默认 `2` |
| `RAG_ENABLED` | 是否启用本地知识检索，默认 `false` |
| `RAG_COLLECTION` | Chroma 集合名称，默认 `aiops_knowledge_v1` |
| `RAG_TOP_K` | 单次最多使用的知识条数，范围 `1` 至 `10`，默认 `4` |
| `RAG_MIN_RELEVANCE_SCORE` | 最低相关度阈值，范围 `0.0` 至 `1.0`，默认 `0.35` |
| `RAG_MAX_CONTEXT_CHARS` | 送入回复模型的知识上下文最大长度，范围 `500` 至 `12000`，默认 `6000` |
| `RAG_REPLY_TOP_K` | 差评回复最多使用的知识条数，默认 `2`，不影响报告检索范围 |
| `RAG_REPLY_MAX_CONTEXT_CHARS` | 差评回复知识上下文最大长度，默认 `1800`，不影响报告上下文预算 |
| `RAG_REVIEW_EVIDENCE_MAX_DOCUMENTS` | 每次重建最多索引的评论证据数，范围 `0` 至 `10000`，默认 `2000`；`0` 只关闭评论证据 |
| `RAG_CHROMA_DIR` | 原生 Python 运行时的本地索引目录；Docker 自动使用持久卷内目录 |
| `EMBEDDING_MODEL` / `EMBEDDING_DEVICE` | 本地嵌入模型与运行设备，Docker 默认 `intfloat/multilingual-e5-small` / `cpu` |
| `CRAWLER_ENABLED` | 是否启用真实爬虫适配器 |

### RAG 索引范围与安全边界

- 仅索引已启用的问题解决方案、有效评论证据，以及被收藏、标记为 `resolved` / `positive_followup` 或累计使用至少 3 次的历史回复。历史回复会记录其资格原因和检索版本。
- 报告只检索当前商品或商家的评论证据；报告来源由应用代码写入并持久化，页面可跳转评论工作台或方案库核查。
- 当前评论内容始终高于检索出的运营建议；模型不会把历史记录描述为当前订单已完成的事实。
- 回复来源由应用代码根据索引元数据生成，模型不能自行编造来源 ID。
- RAG 不可用、索引为空或检索无结果时，差评回复自动使用既有 LangChain 结构化回复流程，并标记为未使用 RAG。

### 评论 AI Hybrid 准入配置

默认 `COMMENT_AI_MODE=rule`，评论聚合继续使用规则问题类型。完成“评论 AI 对照评估”中的人工标注和质量评估后，只有当前批次满足全部准入门槛，界面才会开放确认启用。启用结果写入独立决策表，不会改写原评论字段；人工问题标签始终优先。

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `COMMENT_AI_MODE` | `rule` | `rule` 使用原规则；`hybrid` 允许读取已启用的独立 AI 问题标签 |
| `COMMENT_AI_HYBRID_MIN_CONFIDENCE` | `0.80` | 单条 AI 问题标签可启用的最低置信度 |
| `COMMENT_AI_HYBRID_MIN_ANNOTATED` | `50` | 评估批次所需最少人工标注数 |
| `COMMENT_AI_HYBRID_MIN_ANNOTATION_COVERAGE` | `0.80` | 最低人工标注覆盖率 |
| `COMMENT_AI_HYBRID_MIN_CALL_SUCCESS_RATE` | `0.95` | 最低 AI 调用成功率 |
| `COMMENT_AI_HYBRID_MIN_JSON_VALID_RATE` | `0.98` | 最低结构化输出有效率 |
| `COMMENT_AI_HYBRID_MIN_EVIDENCE_VALID_RATE` | `0.98` | 最低证据有效率 |
| `COMMENT_AI_HYBRID_MAX_SENTIMENT_ACCURACY_DROP` | `0.02` | AI 情感准确率相对规则允许的最大下降 |
| `COMMENT_AI_HYBRID_MIN_PROBLEM_MICRO_F1_GAIN` | `0.05` | AI 问题 Micro-F1 相对规则要求的最小提升 |

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

RAG 持久化和离线多语言验收：

```powershell
cd aiops-python-service
.\.venv\Scripts\python.exe -m pytest tests/test_rag_acceptance.py -q
```

部署文件静态契约也包含在 Python 测试中。安装 Docker 后还可执行：

```powershell
docker compose --env-file .env.example config
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

浏览器上传适合已经合并好的单 CSV 评论数据。页面支持把商家导出的列名映射到标准字段，至少需要映射：

- `product_id`
- `review_score`

可选映射字段包括 `review_content`、`review_title`、`review_id`、`order_id`、`seller_id`、`review_time`。

### Docker 本地目录导入

使用 Docker 启动时，Python 容器默认无法直接访问 Windows 路径。先在根目录 `.env` 中配置 Olist 目录的父路径，例如：

```env
AIOPS_LOCAL_IMPORT_HOST_PATH=D:/data
```

然后重建 Python 服务：

```powershell
docker compose up -d --force-recreate python-service
```

页面仍填写完整 Windows 路径，例如 `D:\\data\\olist-brazilian-ecommerce`。系统只会将该配置目录下的路径映射到容器的只读目录 `/data/local-import`；目录外的路径不会被访问。

## 项目文档

- [项目文档](outputs/AI智能运营助手项目文档.md)
- [接口文档](outputs/AI智能运营助手接口文档.md)

## 安全说明

- 不要提交 `.env`、`.env.local`、`application-secret.yml`、真实 API Key、OSS Key 或数据库密码。
- 对外部署前必须修改初始化管理员密码，并为 MySQL、JWT 和 Redis 使用独立强密钥。
- 当前爬虫能力仅用于学习研究和低频原型演示，不应绕过登录、验证码、付费墙或平台访问控制。
- AI 结果用于辅助运营决策，不能承诺保证爆款、稳赚不赔或替代人工审核。
- 上线部署时建议使用 Nginx 或网关统一代理 `/api/**`，并使用 HTTPS、强密码和独立 RAM 子账号管理 OSS 权限。
