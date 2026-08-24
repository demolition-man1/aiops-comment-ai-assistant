# 面向中小电商商家的评论驱动型 AI 智能运营助手设计与实现

## 1. 项目概述

### 1.1 项目名称

面向中小电商商家的评论驱动型 AI 智能运营助手设计与实现

### 1.2 项目定位

本项目面向中小电商商家，构建一套基于商品评论数据的智能运营辅助系统。系统以用户评论为核心数据来源，通过评论清洗、情感分析、关键词提取、差评原因分类和大模型生成能力，帮助商家快速识别消费者痛点、总结商品优缺点、生成运营优化建议、营销文案和差评回复模板。

项目采用前后端分离架构：

- 前端使用 Vue3、Element Plus、ECharts 实现数据可视化和业务操作界面。
- 后端使用 Java Spring Boot 负责业务管理、任务调度、接口鉴权、数据持久化、缓存和服务编排。
- Python FastAPI 服务负责数据清洗、评论分析、AI Agent 工作流和大模型 API 调用。
- MySQL 用于存储业务数据和分析结果。
- Redis 用于任务状态缓存、热点结果缓存、AI 调用限流和生成结果缓存。

### 1.3 项目背景

大量中小电商商家在经营过程中面临以下问题：

- 缺乏专业数据分析能力，难以及时理解用户反馈。
- 评论数量较多，人工阅读和归纳成本高。
- 差评原因分散，商家难以快速定位商品、物流、服务等问题。
- 商品标题、详情页、短视频脚本和客服回复需要大量人工撰写。
- 商业 SaaS 工具费用较高，中小商家数字化转型成本较大。

本项目以公开电商数据集 Olist Brazilian E-Commerce Public Dataset 作为原型验证数据来源，通过轻量化系统设计，为中小商家提供低成本、可解释、可演示的智能运营工具。

### 1.4 数据来源

项目一期使用 Kaggle 上的 Olist Brazilian E-Commerce Public Dataset：

https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce

该数据集包含巴西电商平台订单、客户、卖家、商品、支付、物流和评论等数据。系统主要使用以下 CSV：

- olist_order_reviews_dataset.csv
- olist_orders_dataset.csv
- olist_order_items_dataset.csv
- olist_products_dataset.csv
- olist_sellers_dataset.csv
- olist_customers_dataset.csv
- product_category_name_translation.csv

说明：Olist 数据集用于课程设计、大创和原型系统验证。后续商业化阶段应接入商家授权数据或合规平台接口。

## 2. 建设目标

### 2.1 总体目标

构建一套评论驱动型 AI 智能运营助手，实现从电商评论数据导入、清洗、分析到 AI 运营报告生成的完整闭环。

### 2.2 具体目标

- 支持 Olist 数据集导入和业务数据入库。
- 支持商品、卖家、订单、评论等基础数据管理。
- 支持按商品或卖家发起评论分析任务。
- 支持评论情感分析，识别正面、中性、负面评论。
- 支持高频关键词、负面关键词和差评原因提取。
- 支持生成商品或卖家的 AI 运营分析报告。
- 支持生成商品标题、详情页文案、促销文案和短视频脚本。
- 支持根据差评内容生成客服回复模板。
- 支持通过图表展示评分分布、情感分布、差评原因占比、关键词排行和趋势数据。
- 支持 Redis 缓存任务状态、热点分析结果和 AI 生成结果。

## 3. 系统角色

### 3.1 普通商家用户

- 登录系统。
- 查看商品、订单、评论数据。
- 发起商品评论分析。
- 查看分析图表和 AI 运营报告。
- 生成营销文案和差评回复。

### 3.2 管理员

- 管理用户账号。
- 管理数据导入任务。
- 查看系统任务执行情况。
- 查看 AI 调用记录和异常日志。

## 4. 总体架构设计

### 4.1 技术架构

```text
Vue3 前端
  |
  | REST API
  v
Spring Boot 后端
  |             |               |
  |             |               |
  v             v               v
MySQL         Redis       Python FastAPI 服务
                                |
                                v
                         大模型 API / NLP 分析
```

### 4.2 分层架构

```text
表现层：Vue3 + Element Plus + ECharts
接口层：Spring Boot Controller
业务层：Spring Service
数据访问层：MyBatis-Plus Mapper
数据层：MySQL
缓存层：Redis
智能服务层：Python FastAPI
AI 能力层：DeepSeek / 通义千问 / 文心一言等大模型 API
```

### 4.3 Java 后端职责

- 用户认证与权限控制。
- 商品、卖家、评论和报告管理。
- 分析任务创建、状态维护和结果落库。
- 调用 Python FastAPI 服务。
- Redis 缓存管理和接口限流。
- 统一异常处理、日志记录和接口文档。

### 4.4 Python 服务职责

- Olist CSV 数据读取、清洗和转换。
- 评论文本预处理。
- 情感分析。
- 关键词提取。
- 差评原因分类。
- AI Agent 工作流编排。
- 调用大模型生成运营报告、营销文案和回复模板。

## 5. 核心业务流程

### 5.1 数据导入流程

```text
1. 管理员上传或指定 Olist CSV 文件。
2. Spring Boot 创建数据导入任务。
3. Spring Boot 调用 Python FastAPI 导入接口。
4. Python 读取 CSV 并进行字段清洗。
5. Python 将原始数据写入 ods_* 原始数据表。
6. Python 根据订单、商品、卖家、评论关系生成 biz_* 业务表数据。
7. Spring Boot 更新导入任务状态。
8. 前端展示导入结果。
```

### 5.2 评论分析流程

```text
1. 用户选择商品或卖家。
2. 前端调用 Spring Boot 创建评论分析任务。
3. Spring Boot 在 MySQL 中写入分析任务记录。
4. Spring Boot 在 Redis 中写入任务状态和进度。
5. Spring Boot 调用 Python FastAPI 评论分析接口。
6. Python 查询或接收评论数据。
7. Python 执行情感分析、关键词提取和差评原因分类。
8. Python 返回结构化分析结果。
9. Spring Boot 保存分析结果到 MySQL。
10. Spring Boot 缓存热点结果到 Redis。
11. 前端查询并展示分析图表。
```

### 5.3 AI 运营报告生成流程

```text
1. 用户在分析结果页点击生成 AI 运营报告。
2. Spring Boot 判断 Redis 中是否已有有效缓存。
3. 如果存在缓存，直接返回。
4. 如果不存在缓存，Spring Boot 调用 Python FastAPI AI 报告接口。
5. Python 将评论分析结果整理成提示词。
6. Python 调用大模型 API。
7. 大模型返回用户痛点、商品优缺点、运营建议和文案建议。
8. Spring Boot 保存报告到 MySQL。
9. Spring Boot 将报告缓存到 Redis。
10. 前端展示 AI 报告。
```

### 5.4 差评回复生成流程

```text
1. 用户选择一条负面评论。
2. 前端提交评论 ID 和回复风格。
3. Spring Boot 查询评论内容。
4. Spring Boot 调用 Python FastAPI 差评回复生成接口。
5. Python 根据评论内容、问题类型和语气风格调用大模型。
6. 大模型返回可复制的客服回复。
7. Spring Boot 保存回复记录。
8. 前端展示回复模板。
```

## 6. 功能模块设计

### 6.1 用户认证模块

功能：

- 用户注册。
- 用户登录。
- JWT Token 签发。
- 用户信息查询。
- 管理员用户管理。

核心接口：

- POST /api/auth/login
- POST /api/auth/register
- GET /api/user/profile

### 6.2 数据导入模块

功能：

- 导入 Olist CSV 数据。
- 查看导入任务状态。
- 查看导入统计结果。

核心接口：

- POST /api/data/import/olist
- GET /api/data/import/tasks
- GET /api/data/import/tasks/{taskId}

### 6.3 商品管理模块

功能：

- 商品列表查询。
- 商品详情查询。
- 按品类、评分、价格区间筛选商品。
- 查看商品关联评论和分析结果。

核心接口：

- GET /api/products
- GET /api/products/{productId}
- GET /api/products/{productId}/comments
- GET /api/products/{productId}/analysis

### 6.4 卖家管理模块

功能：

- 卖家列表查询。
- 卖家详情查询。
- 查看卖家商品数量、订单数量、平均评分和差评率。

核心接口：

- GET /api/sellers
- GET /api/sellers/{sellerId}
- GET /api/sellers/{sellerId}/overview

### 6.5 评论管理模块

功能：

- 评论分页查询。
- 按商品、卖家、评分、情感类型筛选评论。
- 查看评论详情。
- 查询负面评论列表。

核心接口：

- GET /api/comments
- GET /api/comments/{commentId}
- GET /api/comments/negative

### 6.6 评论分析模块

功能：

- 创建评论分析任务。
- 查询任务状态。
- 查询商品评论分析结果。
- 查询卖家评论分析结果。

核心接口：

- POST /api/analysis/tasks
- GET /api/analysis/tasks/{taskId}
- GET /api/analysis/product/{productId}
- GET /api/analysis/seller/{sellerId}

### 6.7 AI 运营报告模块

功能：

- 生成商品运营报告。
- 生成卖家运营报告。
- 查询历史报告。
- 查看报告详情。

核心接口：

- POST /api/ai/reports/product
- POST /api/ai/reports/seller
- GET /api/ai/reports
- GET /api/ai/reports/{reportId}

### 6.8 AI 文案生成模块

功能：

- 生成商品标题。
- 生成详情页文案。
- 生成促销话术。
- 生成短视频脚本。
- 查询生成历史。

核心接口：

- POST /api/ai/contents
- GET /api/ai/contents
- GET /api/ai/contents/{contentId}

### 6.9 差评回复模块

功能：

- 根据负面评论生成客服回复。
- 支持不同语气风格。
- 查询回复历史。

核心接口：

- POST /api/ai/negative-replies
- GET /api/ai/negative-replies

### 6.10 可视化统计模块

功能：

- 商品情感分布。
- 差评原因分布。
- 评分趋势。
- 高频关键词排行。
- 卖家经营概览。

核心接口：

- GET /api/dashboard/overview
- GET /api/dashboard/product/{productId}
- GET /api/dashboard/seller/{sellerId}

## 7. 数据库设计

### 7.1 数据库分层

数据库采用两层设计：

- ods_*：原始数据层，尽量保留 Olist CSV 原始字段。
- biz_*：业务分析层，用于系统查询、分析和展示。

### 7.2 原始数据表

#### 7.2.1 ods_customers

| 字段名 | 类型 | 说明 |
|---|---|---|
| customer_id | varchar(64) | 客户 ID |
| customer_unique_id | varchar(64) | 客户唯一 ID |
| customer_zip_code_prefix | varchar(32) | 邮编前缀 |
| customer_city | varchar(128) | 城市 |
| customer_state | varchar(32) | 州 |

#### 7.2.2 ods_orders

| 字段名 | 类型 | 说明 |
|---|---|---|
| order_id | varchar(64) | 订单 ID |
| customer_id | varchar(64) | 客户 ID |
| order_status | varchar(32) | 订单状态 |
| order_purchase_timestamp | datetime | 下单时间 |
| order_approved_at | datetime | 支付确认时间 |
| order_delivered_carrier_date | datetime | 承运商接收时间 |
| order_delivered_customer_date | datetime | 客户收货时间 |
| order_estimated_delivery_date | datetime | 预计送达时间 |

#### 7.2.3 ods_order_items

| 字段名 | 类型 | 说明 |
|---|---|---|
| order_id | varchar(64) | 订单 ID |
| order_item_id | int | 订单项序号 |
| product_id | varchar(64) | 商品 ID |
| seller_id | varchar(64) | 卖家 ID |
| shipping_limit_date | datetime | 最晚发货时间 |
| price | decimal(10,2) | 商品价格 |
| freight_value | decimal(10,2) | 运费 |

#### 7.2.4 ods_order_reviews

| 字段名 | 类型 | 说明 |
|---|---|---|
| review_id | varchar(64) | 评论 ID |
| order_id | varchar(64) | 订单 ID |
| review_score | int | 评分 |
| review_comment_title | text | 评论标题 |
| review_comment_message | text | 评论正文 |
| review_creation_date | datetime | 评论创建时间 |
| review_answer_timestamp | datetime | 评论回复时间 |

#### 7.2.5 ods_products

| 字段名 | 类型 | 说明 |
|---|---|---|
| product_id | varchar(64) | 商品 ID |
| product_category_name | varchar(128) | 商品类目 |
| product_name_lenght | int | 商品名称长度 |
| product_description_lenght | int | 商品描述长度 |
| product_photos_qty | int | 商品图片数量 |
| product_weight_g | int | 商品重量 |
| product_length_cm | int | 商品长度 |
| product_height_cm | int | 商品高度 |
| product_width_cm | int | 商品宽度 |

#### 7.2.6 ods_sellers

| 字段名 | 类型 | 说明 |
|---|---|---|
| seller_id | varchar(64) | 卖家 ID |
| seller_zip_code_prefix | varchar(32) | 邮编前缀 |
| seller_city | varchar(128) | 城市 |
| seller_state | varchar(32) | 州 |

#### 7.2.7 ods_category_translation

| 字段名 | 类型 | 说明 |
|---|---|---|
| product_category_name | varchar(128) | 葡萄牙语类目 |
| product_category_name_english | varchar(128) | 英文类目 |

### 7.3 业务数据表

#### 7.3.1 sys_user

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| username | varchar(64) | 用户名 |
| password | varchar(255) | 加密密码 |
| nickname | varchar(64) | 昵称 |
| email | varchar(128) | 邮箱 |
| role | varchar(32) | 角色：admin / merchant |
| status | tinyint | 状态：1 启用，0 禁用 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 7.3.2 biz_seller

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| seller_id | varchar(64) | Olist 卖家 ID |
| seller_city | varchar(128) | 城市 |
| seller_state | varchar(32) | 州 |
| product_count | int | 商品数量 |
| order_count | int | 订单数量 |
| avg_score | decimal(4,2) | 平均评分 |
| negative_rate | decimal(6,4) | 差评率 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 7.3.3 biz_product

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| product_id | varchar(64) | Olist 商品 ID |
| seller_id | varchar(64) | 主要卖家 ID |
| category_name | varchar(128) | 原始类目 |
| category_name_en | varchar(128) | 英文类目 |
| avg_price | decimal(10,2) | 平均售价 |
| avg_freight | decimal(10,2) | 平均运费 |
| order_count | int | 订单数量 |
| review_count | int | 评论数量 |
| avg_score | decimal(4,2) | 平均评分 |
| negative_rate | decimal(6,4) | 差评率 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 7.3.4 biz_comment

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| review_id | varchar(64) | Olist 评论 ID |
| order_id | varchar(64) | 订单 ID |
| product_id | varchar(64) | 商品 ID |
| seller_id | varchar(64) | 卖家 ID |
| review_score | int | 评分 |
| review_title | text | 评论标题 |
| review_content | text | 评论正文 |
| review_time | datetime | 评论时间 |
| sentiment | varchar(32) | positive / neutral / negative |
| sentiment_score | decimal(6,4) | 情感分数 |
| keywords | json | 关键词 |
| problem_type | varchar(64) | 差评原因类型 |
| is_negative | tinyint | 是否负面评论 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 7.3.5 biz_analysis_task

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 创建用户 ID |
| target_type | varchar(32) | product / seller |
| target_id | varchar(64) | 商品 ID 或卖家 ID |
| task_type | varchar(64) | comment_analysis / ai_report / import |
| task_status | varchar(32) | pending / processing / success / failed |
| progress | int | 进度 0-100 |
| request_param | json | 请求参数 |
| error_message | text | 错误信息 |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

#### 7.3.6 biz_comment_analysis_result

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| task_id | bigint | 分析任务 ID |
| target_type | varchar(32) | product / seller |
| target_id | varchar(64) | 分析对象 ID |
| total_count | int | 评论总数 |
| positive_count | int | 正面评论数 |
| neutral_count | int | 中性评论数 |
| negative_count | int | 负面评论数 |
| positive_rate | decimal(6,4) | 好评率 |
| negative_rate | decimal(6,4) | 差评率 |
| top_keywords | json | 高频关键词 |
| negative_keywords | json | 负面关键词 |
| problem_distribution | json | 差评原因分布 |
| score_distribution | json | 评分分布 |
| summary | text | 分析摘要 |
| create_time | datetime | 创建时间 |

#### 7.3.7 biz_operation_report

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| task_id | bigint | 任务 ID |
| target_type | varchar(32) | product / seller |
| target_id | varchar(64) | 分析对象 ID |
| report_title | varchar(255) | 报告标题 |
| consumer_pain_points | text | 消费者痛点 |
| product_advantages | text | 商品优势 |
| product_disadvantages | text | 商品不足 |
| operation_suggestions | text | 运营建议 |
| copywriting_suggestions | text | 文案建议 |
| service_suggestions | text | 客服建议 |
| risk_tips | text | 风险提示 |
| full_report | longtext | 完整报告 |
| model_name | varchar(64) | 大模型名称 |
| create_time | datetime | 创建时间 |

#### 7.3.8 biz_ai_content_record

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| target_type | varchar(32) | product / seller |
| target_id | varchar(64) | 对象 ID |
| content_type | varchar(64) | title / detail / promotion / short_video |
| style_type | varchar(64) | simple / viral / value / professional |
| prompt | text | 提示词 |
| generated_content | longtext | 生成内容 |
| model_name | varchar(64) | 模型名称 |
| token_usage | int | Token 消耗 |
| create_time | datetime | 创建时间 |

#### 7.3.9 biz_negative_reply

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| comment_id | bigint | 评论 ID |
| product_id | varchar(64) | 商品 ID |
| seller_id | varchar(64) | 卖家 ID |
| problem_type | varchar(64) | 差评类型 |
| comment_content | text | 原评论 |
| tone_type | varchar(64) | polite / sincere / professional |
| reply_content | text | 回复内容 |
| model_name | varchar(64) | 模型名称 |
| create_time | datetime | 创建时间 |

## 8. Redis 设计

| Key | Value | 说明 | 过期时间 |
|---|---|---|---|
| login:token:{token} | userId | 登录 Token 缓存 | 24 小时 |
| task:status:{taskId} | processing/success/failed | 任务状态 | 2 小时 |
| task:progress:{taskId} | 0-100 | 任务进度 | 2 小时 |
| analysis:product:{productId} | JSON | 商品分析结果缓存 | 6 小时 |
| analysis:seller:{sellerId} | JSON | 卖家分析结果缓存 | 6 小时 |
| ai:report:{targetType}:{targetId} | JSON | AI 报告缓存 | 12 小时 |
| ai:content:{hash} | 文案内容 | AI 文案结果缓存 | 12 小时 |
| rate:ai:user:{userId} | 调用次数 | AI 接口限流 | 1 分钟 |
| hot:keywords:product:{productId} | JSON | 热门关键词 | 6 小时 |

Redis 主要用于提升系统响应速度、减少数据库查询压力，并降低大模型重复调用成本。

## 9. 接口规范

### 9.1 通用请求头

```http
Authorization: Bearer {token}
Content-Type: application/json
```

### 9.2 通用返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-08-12 10:00:00"
}
```

### 9.3 分页返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": "2026-08-12 10:00:00"
}
```

### 9.4 状态码约定

| code | 说明 |
|---|---|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 Token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求过于频繁 |
| 500 | 系统异常 |
| 503 | Python 分析服务不可用 |

## 10. Java 后端 REST API 文档

### 10.1 用户认证

#### 用户登录

```http
POST /api/auth/login
```

请求参数：

```json
{
  "username": "admin",
  "password": "your-password"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "username": "admin",
    "role": "admin"
  }
}
```

#### 用户注册

```http
POST /api/auth/register
```

请求参数：

```json
{
  "username": "merchant01",
  "password": "your-password",
  "nickname": "测试商家",
  "email": "merchant@example.com"
}
```

### 10.2 数据导入

#### 导入 Olist 数据集

```http
POST /api/data/import/olist
```

请求参数：

```json
{
  "dataPath": "D:/datasets/olist",
  "importMode": "full"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "taskStatus": "processing"
  }
}
```

#### 查询导入任务详情

```http
GET /api/data/import/tasks/{taskId}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 10001,
    "taskStatus": "success",
    "progress": 100,
    "importedRows": 98765,
    "errorMessage": null
  }
}
```

### 10.3 商品接口

#### 分页查询商品

```http
GET /api/products?pageNum=1&pageSize=10&categoryNameEn=health_beauty&minScore=4
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "productId": "abc123",
        "categoryNameEn": "health_beauty",
        "avgPrice": 89.90,
        "reviewCount": 238,
        "avgScore": 4.31,
        "negativeRate": 0.08
      }
    ],
    "total": 1200,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

#### 查询商品详情

```http
GET /api/products/{productId}
```

### 10.4 卖家接口

#### 分页查询卖家

```http
GET /api/sellers?pageNum=1&pageSize=10&state=SP
```

#### 查询卖家经营概览

```http
GET /api/sellers/{sellerId}/overview
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sellerId": "seller001",
    "sellerState": "SP",
    "productCount": 56,
    "orderCount": 1200,
    "avgScore": 4.18,
    "negativeRate": 0.11,
    "mainCategories": ["health_beauty", "watches_gifts"]
  }
}
```

### 10.5 评论接口

#### 分页查询评论

```http
GET /api/comments?pageNum=1&pageSize=10&productId=abc123&sentiment=negative
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 101,
        "reviewId": "review001",
        "productId": "abc123",
        "sellerId": "seller001",
        "reviewScore": 1,
        "reviewContent": "Produto chegou atrasado",
        "sentiment": "negative",
        "problemType": "logistics",
        "reviewTime": "2018-01-01 12:00:00"
      }
    ],
    "total": 56,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

#### 查询评论详情

```http
GET /api/comments/{commentId}
```

#### 查询负面评论

```http
GET /api/comments/negative?productId=abc123&pageNum=1&pageSize=10
```

### 10.6 评论分析接口

#### 创建评论分析任务

```http
POST /api/analysis/tasks
```

请求参数：

```json
{
  "targetType": "product",
  "targetId": "abc123",
  "analysisType": "comment_analysis"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 20001,
    "taskStatus": "processing"
  }
}
```

#### 查询分析任务状态

```http
GET /api/analysis/tasks/{taskId}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 20001,
    "taskStatus": "success",
    "progress": 100,
    "errorMessage": null
  }
}
```

#### 查询商品评论分析结果

```http
GET /api/analysis/product/{productId}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "productId": "abc123",
    "totalCount": 238,
    "positiveCount": 185,
    "neutralCount": 27,
    "negativeCount": 26,
    "positiveRate": 0.7773,
    "negativeRate": 0.1092,
    "topKeywords": [
      {"keyword": "quality", "count": 42},
      {"keyword": "price", "count": 31}
    ],
    "problemDistribution": [
      {"type": "logistics", "count": 12},
      {"type": "quality", "count": 8}
    ],
    "summary": "该商品整体评价较好，负面反馈主要集中在物流延迟和包装问题。"
  }
}
```

#### 查询卖家评论分析结果

```http
GET /api/analysis/seller/{sellerId}
```

### 10.7 AI 运营报告接口

#### 生成商品运营报告

```http
POST /api/ai/reports/product
```

请求参数：

```json
{
  "productId": "abc123",
  "forceRefresh": false,
  "language": "zh-CN"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": 30001,
    "reportTitle": "商品评论驱动运营分析报告",
    "consumerPainPoints": "用户主要痛点集中在物流慢、包装破损和描述不够清晰。",
    "productAdvantages": "价格接受度较高，部分用户认可商品质量。",
    "productDisadvantages": "低分评论集中反映配送体验不稳定。",
    "operationSuggestions": "建议优化发货时效提示、加强包装保护，并在详情页明确配送周期。",
    "copywritingSuggestions": "标题可突出高性价比、稳定品质和售后保障。",
    "serviceSuggestions": "针对物流差评应优先解释原因并提供补偿方案。"
  }
}
```

#### 生成卖家运营报告

```http
POST /api/ai/reports/seller
```

请求参数：

```json
{
  "sellerId": "seller001",
  "forceRefresh": false,
  "language": "zh-CN"
}
```

#### 查询报告列表

```http
GET /api/ai/reports?targetType=product&targetId=abc123&pageNum=1&pageSize=10
```

#### 查询报告详情

```http
GET /api/ai/reports/{reportId}
```

### 10.8 AI 文案生成接口

#### 生成营销文案

```http
POST /api/ai/contents
```

请求参数：

```json
{
  "targetType": "product",
  "targetId": "abc123",
  "contentType": "title",
  "styleType": "value",
  "language": "zh-CN",
  "extraRequirement": "突出高性价比和售后保障"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "contentId": 40001,
    "generatedContent": "高性价比实用好物，品质稳定，售后无忧，日常使用更省心",
    "modelName": "deepseek-chat"
  }
}
```

contentType 可选值：

| 值 | 说明 |
|---|---|
| title | 商品标题 |
| detail | 商品详情页 |
| promotion | 促销话术 |
| short_video | 短视频脚本 |

styleType 可选值：

| 值 | 说明 |
|---|---|
| simple | 简洁专业风 |
| viral | 网红种草风 |
| value | 平价性价比风 |
| professional | 专业说明风 |

#### 查询文案生成历史

```http
GET /api/ai/contents?targetId=abc123&pageNum=1&pageSize=10
```

### 10.9 差评回复接口

#### 生成差评回复

```http
POST /api/ai/negative-replies
```

请求参数：

```json
{
  "commentId": 101,
  "toneType": "sincere",
  "language": "zh-CN"
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "replyId": 50001,
    "replyContent": "非常抱歉给您带来了不好的购物体验。关于物流延迟问题，我们会继续优化发货和配送跟踪流程。您可以联系我们的客服，我们将为您提供进一步处理方案。"
  }
}
```

toneType 可选值：

| 值 | 说明 |
|---|---|
| polite | 礼貌客观 |
| sincere | 真诚安抚 |
| professional | 专业正式 |

#### 查询差评回复历史

```http
GET /api/ai/negative-replies?productId=abc123&pageNum=1&pageSize=10
```

### 10.10 数据看板接口

#### 查询首页概览

```http
GET /api/dashboard/overview
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "productCount": 32951,
    "sellerCount": 3095,
    "commentCount": 99224,
    "avgScore": 4.08,
    "negativeRate": 0.13
  }
}
```

#### 查询商品看板

```http
GET /api/dashboard/product/{productId}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scoreDistribution": [
      {"score": 1, "count": 10},
      {"score": 2, "count": 16},
      {"score": 3, "count": 27},
      {"score": 4, "count": 55},
      {"score": 5, "count": 130}
    ],
    "sentimentDistribution": [
      {"sentiment": "positive", "count": 185},
      {"sentiment": "neutral", "count": 27},
      {"sentiment": "negative", "count": 26}
    ],
    "keywordRank": [
      {"keyword": "quality", "count": 42},
      {"keyword": "delivery", "count": 36}
    ]
  }
}
```

#### 查询卖家看板

```http
GET /api/dashboard/seller/{sellerId}
```

## 11. Python FastAPI 内部接口文档

Python 服务不直接暴露给前端，由 Java 后端调用。

### 11.1 Olist 数据导入

```http
POST /internal/olist/import
```

请求参数：

```json
{
  "taskId": 10001,
  "dataPath": "D:/datasets/olist",
  "importMode": "full"
}
```

响应示例：

```json
{
  "success": true,
  "importedRows": 98765,
  "message": "import completed"
}
```

### 11.2 评论分析

```http
POST /internal/analysis/comments
```

请求参数：

```json
{
  "taskId": 20001,
  "targetType": "product",
  "targetId": "abc123",
  "comments": [
    {
      "commentId": 101,
      "reviewScore": 1,
      "reviewContent": "Produto chegou atrasado"
    }
  ]
}
```

响应示例：

```json
{
  "success": true,
  "data": {
    "totalCount": 238,
    "positiveCount": 185,
    "neutralCount": 27,
    "negativeCount": 26,
    "topKeywords": [
      {"keyword": "quality", "count": 42}
    ],
    "problemDistribution": [
      {"type": "logistics", "count": 12}
    ],
    "summary": "该商品负面反馈主要集中在物流延迟。"
  }
}
```

### 11.3 AI 运营报告生成

```http
POST /internal/ai/report
```

请求参数：

```json
{
  "targetType": "product",
  "targetId": "abc123",
  "analysisResult": {
    "totalCount": 238,
    "positiveRate": 0.7773,
    "negativeRate": 0.1092,
    "topKeywords": ["quality", "price"],
    "negativeKeywords": ["delay", "package"],
    "problemDistribution": [
      {"type": "logistics", "count": 12}
    ]
  },
  "language": "zh-CN"
}
```

### 11.4 AI 文案生成

```http
POST /internal/ai/content
```

请求参数：

```json
{
  "targetType": "product",
  "targetId": "abc123",
  "contentType": "title",
  "styleType": "value",
  "analysisSummary": "用户认可商品价格，但抱怨物流较慢。",
  "extraRequirement": "突出高性价比和售后保障"
}
```

### 11.5 差评回复生成

```http
POST /internal/ai/negative-reply
```

请求参数：

```json
{
  "commentId": 101,
  "commentContent": "Produto chegou atrasado",
  "problemType": "logistics",
  "toneType": "sincere",
  "language": "zh-CN"
}
```

## 12. 后端包结构建议

```text
com.example.aiops
├── AiOpsApplication.java
├── common
│   ├── result
│   ├── exception
│   ├── constants
│   └── util
├── config
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   ├── WebMvcConfig.java
│   └── Knife4jConfig.java
├── modules
│   ├── auth
│   ├── user
│   ├── product
│   ├── seller
│   ├── comment
│   ├── analysis
│   ├── report
│   ├── ai
│   ├── dashboard
│   └── dataimport
└── integration
    └── python
        ├── PythonAnalysisClient.java
        ├── dto
        └── fallback
```

## 13. 非功能需求

### 13.1 性能需求

- 普通分页接口响应时间应控制在 1 秒内。
- 评论分析任务允许异步执行。
- AI 生成任务允许 10-60 秒内完成。
- 热点分析结果优先从 Redis 读取。

### 13.2 安全需求

- 登录密码使用 BCrypt 加密。
- 后端接口使用 JWT 鉴权。
- AI 接口调用需要做用户级限流。
- 内部 Python 服务接口不对公网开放。
- 系统不得承诺保证销量增长或盈利。

### 13.3 可扩展性需求

- 数据源可从 Olist 扩展为商家授权 CSV、平台开放接口或合规爬虫。
- 大模型供应商可通过配置切换。
- 评论分析策略可从规则分析扩展为机器学习模型或大模型分类。

## 14. 项目创新点

- 评论驱动运营决策：从消费者反馈出发，生成可执行运营建议。
- Java 与 Python 双服务解耦：Java 负责业务系统，Python 负责 AI 和数据分析。
- 低成本数字化工具：面向中小商家，降低数据分析和内容生产门槛。
- 多语言跨境评论分析：Olist 评论以葡萄牙语为主，可生成中文运营报告。
- Redis 降本增效：通过缓存和限流减少重复分析和 AI 调用成本。

## 15. 项目边界说明

- 本系统为辅助决策工具，不保证商品销量提升或盈利结果。
- Olist 数据集用于原型验证和学术研究，不代表实时市场数据。
- 项目一期不依赖真实平台爬虫，爬虫作为后续扩展模块。
- AI 生成内容需要商家人工审核后再使用。

## 16. 一期开发里程碑

| 阶段 | 内容 | 预计周期 |
|---|---|---|
| 第 1 阶段 | 数据库设计、Spring Boot 项目初始化、基础表创建 | 1 周 |
| 第 2 阶段 | Olist 数据导入、商品/卖家/评论查询接口 | 1-2 周 |
| 第 3 阶段 | Python 评论分析服务、情感分析、关键词提取 | 1-2 周 |
| 第 4 阶段 | 分析任务机制、Redis 缓存、任务状态查询 | 1 周 |
| 第 5 阶段 | AI 报告、文案生成、差评回复生成 | 1-2 周 |
| 第 6 阶段 | 前端可视化、联调、测试和演示视频 | 1-2 周 |

## 17. 答辩总结表述

本系统以 Olist 公开电商数据集为基础，围绕中小电商商家在评论分析、用户痛点识别、运营建议生成和营销文案创作中的实际需求，设计并实现了评论驱动型 AI 智能运营助手。系统采用 Vue3、Spring Boot、MySQL、Redis 和 Python FastAPI 架构，由 Java 后端统一负责业务数据管理、任务调度、缓存限流和服务编排，由 Python 服务完成评论分析和大模型生成能力。系统能够将分散的用户评论转化为结构化运营洞察，帮助商家降低数据分析成本，提高数字化运营能力。
