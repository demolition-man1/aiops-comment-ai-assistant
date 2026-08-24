# 面向中小电商商家的评论驱动型 AI 智能运营助手项目文档

## 1. 项目概述

### 1.1 项目名称

面向中小电商商家的评论驱动型 AI 智能运营助手设计与实现

### 1.2 项目定位

本项目面向中小电商商家，构建一套以商品评论数据为核心的智能运营辅助系统。系统通过评论数据导入、清洗、情感分析、关键词提取、差评原因分类、人工标签修正、评论按需翻译和大模型生成能力，帮助商家快速识别消费者痛点、总结商品优缺点、生成运营建议、营销文案和差评回复模板。

项目一期提供两种数据接入方式：一种是 CSV 文件导入，用于稳定接入 Olist 公开数据集或商家授权数据；另一种是爬虫采集，用于演示系统面向真实电商平台的扩展能力。两种方式最终都会转换为统一的商品、卖家、订单和评论业务数据，供后续评论分析、AI 报告和可视化看板复用。

前端顶部快捷入口采用业务化设计：定时同步进入同步配置与执行记录页面，任务中心进入统一任务追踪页面，数据报表进入全局运营报表页面。普通商家使用时看到的是业务入口，不展示联调状态和本地调试地址。

### 1.3 核心价值

- 降低中小商家数据分析门槛。
- 将海量评论转化为结构化运营洞察。
- 辅助商家发现商品、物流、包装、客服等问题。
- 基于评论结果生成运营报告和营销内容。
- 支持自定义评论标签、时间趋势分析和商品对比分析。
- 跟踪差评回复模板使用次数、收藏状态和处理效果。
- 通过 Redis 缓存和限流降低系统响应时间与 AI 调用成本。

## 2. 项目背景

中小电商商家在平台经营中普遍存在以下痛点：

- 评论数量较多，人工阅读和整理成本高。
- 缺乏专业数据分析能力，难以及时发现用户真实痛点。
- 差评原因分散，商家难以判断问题集中在商品、物流、包装还是客服。
- 商品标题、详情页文案、促销话术和差评回复需要大量人工撰写。
- 商业化运营 SaaS 工具成本较高，不适合早期小微商家。

本项目通过 Java 后端业务系统、Python 智能分析服务和大模型 API 结合，形成“评论数据输入 - 评论智能分析 - 运营建议生成 - 内容辅助生产”的完整闭环，服务中小商家数字化转型。

## 3. 数据来源

### 3.1 数据接入方式

系统支持两种数据导入方式：

| 导入方式 | 说明 | 适用场景 |
|---|---|---|
| CSV 文件导入 | 上传或指定 Olist CSV、商家导出的评论 CSV、平台后台导出的订单评论文件 | 课程设计、大创演示、稳定批量导入 |
| 爬虫采集 | 输入商品页或评论页 URL，由 Python 爬虫服务采集公开商品和评论信息 | 拓展功能、真实平台适配演示、后续升级 |

两种数据源最终都会写入统一的业务表：

- biz_product
- biz_seller
- biz_comment
- biz_analysis_task

这样后续评论分析、AI 运营报告、AI 文案生成和差评回复模块不需要关心数据来自 CSV 还是爬虫。

### 3.2 主数据集

Kaggle Olist Brazilian E-Commerce Public Dataset

链接：https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce

### 3.3 使用的数据文件

- olist_order_reviews_dataset.csv：订单评论与评分。
- olist_orders_dataset.csv：订单状态和时间。
- olist_order_items_dataset.csv：订单明细、商品、卖家、价格和运费。
- olist_products_dataset.csv：商品信息。
- olist_sellers_dataset.csv：卖家信息。
- olist_customers_dataset.csv：客户地区信息。
- product_category_name_translation.csv：商品类目英文翻译。

### 3.4 爬虫数据说明

爬虫模块作为系统扩展能力，建议一期只做低频、少量、可控采集，用于采集公开商品信息和公开评论样例。爬虫服务由 Python 实现，Java 后端只负责任务创建、状态管理和结果落库。

爬虫目标数据：

- 商品标题。
- 商品价格。
- 商品评分。
- 评论内容。
- 评论评分。
- 评论时间。
- 评论来源平台。

爬虫合规要求：

- 仅用于学习研究和原型演示。
- 不进行高频、大规模采集。
- 设置请求间隔和最大采集数量。
- 不绕过登录、验证码、付费墙或平台访问控制。
- 后续商业化阶段优先使用平台开放接口或商家授权数据。

### 3.5 数据使用说明

Olist 数据集主要用于课程设计、大创、挑战杯原型验证和学术研究。后续商业化阶段应接入商家授权数据或平台合规接口，不直接将公开数据集用于商业运营。

## 4. 技术架构

### 4.1 总体技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue3、Element Plus、ECharts、vue-i18n |
| Java 后端 | Spring Boot、Spring MVC、MyBatis-Plus、Quartz、Bucket4j、Redisson |
| 数据库 | MySQL |
| 缓存 | Redis、Redisson |
| 文件存储 | 阿里云 OSS |
| Python 服务 | FastAPI、Pandas、关键词提取、主题聚类、大模型 SDK |
| AI 能力 | DeepSeek / 通义千问 / 文心一言等大模型 API |
| 接口文档 | Knife4j / Swagger / OpenAPI 注解 |

### 4.2 总体架构图

```text
Vue3 前端
  |
  | REST API
  v
Spring Boot 后端
  |              |             |                  |                  |
  |              |             |                  |                  |
  v              v             v                  v                  v
MySQL          Redis      阿里云 OSS       Quartz 定时任务       Python FastAPI 服务
                                                    |
                                                    v
                                           关键词提取 / 主题聚类 / 大模型 API
```

### 4.3 服务职责边界

Java Spring Boot 后端作为系统业务中台，负责：

- 用户认证与权限管理。
- 商品、卖家、评论、分析结果和报告管理。
- 分析任务创建、状态维护、结果落库。
- 定时同步配置、Quartz 调度注册、同步执行记录和统一任务中心聚合。
- 全局数据报表接口，提供趋势、分布和商品排行数据。
- Redis 缓存、任务进度缓存、分析结果缓存和 AI 调用缓存。
- Bucket4j 用户级 AI 限流，避免报告、文案、回复等接口被高频调用。
- Redisson 分布式组件预留，用于后续分布式锁、任务互斥和多实例部署。
- Quartz 定时任务维护，将长时间卡在 processing 的任务自动标记为 failed。
- 阿里云 OSS 文件上传、文件地址保存和临时访问地址生成。
- 调用 Python FastAPI 服务。
- 向 Vue 前端提供 RESTful API。

Python FastAPI 服务作为智能分析引擎，负责：

- Olist CSV 数据清洗和转换。
- 评论情感分析。
- 关键词提取：默认规则提取，过滤 nan / none / null / undefined 等无效词；可通过环境变量切换 KeyBERT。
- 主题聚类：默认规则主题聚类，覆盖物流、质量、包装、价格、服务、尺寸等主题；可通过环境变量切换 BERTopic。
- 差评原因分类。
- 自定义标签统计和时间趋势汇总。
- 爬虫采集适配层，当前保留低频样例采集，后续可接入 Scrapy / Crawlee。
- AI Agent 工作流编排。
- 调用大模型生成运营报告、营销文案、差评回复和商品对比报告。

### 4.4 三语国际化设计

系统支持中文、英文、巴西葡语三语切换，面向跨境电商和国际商家演示场景。前端通过 `vue-i18n` 维护语言包，通过 Pinia 保存当前语言，并使用 Element Plus 的 `ElConfigProvider` 同步切换组件库内置文案。

语言切换范围：

- 前端导航、按钮、表单、表格列名、图表标题、提示消息和空状态文案。
- AI 运营报告、AI 文案、差评回复和商品 A/B 对比请求中的 `language` 参数。
- Element Plus 分页、选择器等组件内置文案。

边界说明：

- 原始评论内容保持数据源原文展示，避免破坏评论证据链。
- 一期不新增数据库字段，语言偏好保存在浏览器 `localStorage`。
- 支持评论原文按需翻译：原始评论保持不变，用户点击翻译按钮后调用大模型生成当前界面语言版本，并通过 Redis 缓存减少重复调用。
- 后续可扩展用户级语言偏好和多语言报告模板。

## 5. 后端工程架构

### 5.1 多模块结构

后端可以采用类似“苍穹外卖”的多模块 Spring Boot 架构，将公共能力、数据对象和业务服务拆开。

推荐项目结构：

```text
aiops-backend
├── aiops-common
├── aiops-pojo
└── aiops-server
```

### 5.2 aiops-common 模块

该模块存放公共工具、统一返回结果、枚举、异常、上下文和配置属性。

```text
aiops-common
└── src/main/java/com/aiops
    ├── constant
    ├── context
    ├── enumeration
    ├── exception
    ├── json
    ├── properties
    ├── result
    └── utils
```

建议类：

- Result：统一响应结果。
- PageResult：分页响应结果。
- BaseContext：保存当前登录用户 ID。
- JwtUtil：JWT 工具类。
- RedisKeyConstant：Redis Key 常量。
- MessageConstant：业务提示常量。
- AiRateLimitProperties：AI 调用限流配置，默认每用户每类 AI 能力 60 秒 20 次。
- TaskMaintenanceProperties：Quartz 任务维护配置，默认 5 分钟扫描一次、30 分钟未更新的 processing 任务标记失败。
- TaskStatusEnum：任务状态枚举。
- SentimentEnum：情感类型枚举。
- ProblemTypeEnum：差评原因类型枚举。
- BusinessException：业务异常。

### 5.3 aiops-pojo 模块

该模块存放 entity、DTO 和 VO。

```text
aiops-pojo
└── src/main/java/com/aiops
    ├── entity
    ├── dto
    └── vo
```

entity 用于数据库映射：

- SysUser
- BizProduct
- BizSeller
- BizComment
- BizAnalysisTask
- BizCommentAnalysisResult
- BizOperationReport
- BizAiContentRecord
- BizNegativeReply

DTO 用于接收前端请求：

- LoginDTO
- ProductQueryDTO
- SellerQueryDTO
- CommentQueryDTO
- AnalysisTaskCreateDTO
- AiReportGenerateDTO
- AiContentGenerateDTO
- NegativeReplyGenerateDTO
- OlistImportDTO

VO 用于返回前端展示：

- LoginVO
- ProductVO
- SellerVO
- CommentVO
- AnalysisTaskVO
- AnalysisResultVO
- OperationReportVO
- AiContentVO
- NegativeReplyVO
- DashboardOverviewVO

### 5.4 aiops-server 模块

该模块是后端启动模块和业务实现模块。

```text
aiops-server
└── src/main/java/com/aiops
    ├── AiOpsApplication.java
    ├── annotation
    ├── aspect
    ├── config
    ├── controller
    ├── converter
    ├── handler
    ├── interceptor
    ├── mapper
    ├── service
    ├── task
    ├── websocket
    └── client
```

各包职责：

- annotation：自定义注解，如操作日志、接口限流。
- aspect：AOP 切面，如日志记录、AI 接口限流。
- config：Redis、Redisson、Quartz、WebMvc、Knife4j、线程池、安全配置。
- properties：可配置阿里云 OSS 的 endpoint、bucketName、accessKey 和上传目录。
- controller：REST API 接口层。
- converter：entity、DTO、VO 对象转换。
- handler：全局异常处理器。
- interceptor：JWT 登录拦截器。
- mapper：MyBatis-Plus 数据访问层。
- service：核心业务逻辑。
- task：Quartz 定时任务、异步任务。
- websocket：任务进度推送，可作为扩展功能。
- client：调用 Python FastAPI 服务的客户端。

### 5.5 client 包设计

由于本项目需要 Java 调用 Python 服务，建议在 aiops-server 中单独增加 client 包：

```text
client
├── PythonAnalysisClient
├── PythonAiClient
└── dto
```

职责：

- PythonAnalysisClient：调用评论分析、数据导入等接口。
- PythonAiClient：调用 AI 报告、AI 文案、差评回复和商品对比报告生成接口。
- client.dto：Java 与 Python 服务之间的内部请求和响应对象。

## 6. 核心业务流程

### 6.1 CSV 数据导入流程

```text
1. 管理员在前端选择 Olist CSV / 商家授权 CSV 文件。
2. 前端调用 Spring Boot 文件上传接口。
3. Spring Boot 将 CSV 文件上传到阿里云 OSS。
4. OSS 返回 objectKey 和文件访问地址 fileUrl。
5. Spring Boot 保存上传记录，并将 objectKey / fileUrl 返回给前端。
6. 前端携带 objectKey / fileUrl 创建 CSV 导入任务。
7. Spring Boot 创建 CSV 导入任务，写入 biz_analysis_task。
8. Spring Boot 将任务状态写入 Redis。
9. Spring Boot 调用 Python FastAPI CSV 导入接口，并传入 objectKey / fileUrl。
10. Python 下载或读取 OSS 中的 CSV 文件并清洗字段。
11. Python 将原始数据写入 ods_* 表。
12. Python 关联订单、商品、卖家、评论数据，生成 biz_* 业务表。
13. Spring Boot 更新任务状态和导入统计。
14. 前端展示导入进度和导入结果。
```

### 6.2 爬虫数据导入流程

```text
1. 用户输入目标平台、商品 URL、最大采集数量等参数。
2. Spring Boot 创建爬虫任务，写入 biz_analysis_task 或 biz_crawl_task。
3. Spring Boot 将任务状态和进度写入 Redis。
4. Spring Boot 调用 Python FastAPI 爬虫任务接口。
5. Python 根据平台类型选择对应爬虫策略。
6. Python 低频采集公开商品信息和评论样例。
7. Python 对采集结果进行去重、字段清洗和格式统一。
8. Python 返回商品、评论和采集统计结果。
9. Spring Boot 将结果写入 biz_product、biz_seller、biz_comment。
10. Spring Boot 更新爬虫任务状态。
11. 前端展示采集结果，并允许用户继续发起评论分析。
```

当前爬虫模块采用适配器设计：`sample` 适配器用于低频公开样例演示，`scrapy` 和 `crawlee` 适配器作为后续升级预留。这样一期可以稳定演示，二期可以逐步替换为更成熟的采集框架。

### 6.3 评论分析流程

```text
1. 用户选择商品或卖家。
2. 前端调用 Spring Boot 创建评论分析任务。
3. Spring Boot 写入 biz_analysis_task，状态为 processing。
4. Spring Boot 写入 Redis 任务状态和进度。
5. Spring Boot 查询目标对象下的评论数据。
6. Spring Boot 调用 Python 评论分析接口。
7. Python 执行情感分析、关键词提取、主题聚类、差评原因分类、负面关键词统计、自定义标签统计和时间趋势汇总。
8. Python 返回结构化分析结果。
9. Spring Boot 保存结果到 biz_comment_analysis_result。
10. Spring Boot 更新任务状态为 success。
11. 前端查询并展示分析图表。
```

如果 Java 服务异常退出或 Python 服务长时间无响应，Quartz 维护任务会定期扫描 `biz_analysis_task`，将超过配置时间仍处于 processing 的任务标记为 failed，并同步刷新 Redis 中的任务状态和进度，避免前端一直显示处理中。

### 6.4 AI 运营报告流程

```text
1. 用户在分析结果页点击生成 AI 运营报告。
2. Spring Boot 执行 Bucket4j AI 限流校验。
3. Spring Boot 查询 Redis 是否存在有效报告缓存。
4. 如果有缓存，直接返回报告。
5. 如果无缓存，Spring Boot 查询评论分析结果。
6. Spring Boot 调用 Python AI 报告生成接口。
7. Python 组织提示词并调用大模型 API。
8. 大模型返回消费者痛点、商品优缺点、运营建议和文案建议。
9. Spring Boot 保存报告到 biz_operation_report。
10. Spring Boot 写入 Redis 缓存。
11. 前端展示报告内容。
```

### 6.5 差评回复生成流程

```text
1. 用户选择一条负面评论。
2. 前端提交评论 ID 和回复语气。
3. Spring Boot 查询评论内容和差评类型。
4. Spring Boot 执行 Bucket4j AI 限流校验。
5. Spring Boot 调用 Python 差评回复生成接口。
6. Python 基于单条评论内容、评分、差评原因和语气要求调用大模型生成独立回复。
7. Spring Boot 保存回复记录到 biz_negative_reply。
8. 前端展示可复制的回复内容，并支持查看回复历史。
```

### 6.6 单条评论翻译流程

```text
1. 用户在评论列表点击某条评论的翻译按钮。
2. 前端读取当前界面语言 zh-CN / en-US / pt-BR，调用 Spring Boot 评论翻译接口。
3. Spring Boot 查询评论原文、评分、商品 ID 和卖家 ID。
4. Spring Boot 优先读取 Redis 翻译缓存。
5. 如果无缓存或用户强制刷新，Spring Boot 执行 Bucket4j AI 限流校验。
6. Spring Boot 调用 Python 评论翻译接口。
7. Python 将单条评论翻译为目标语言，只返回翻译结果，不改写原始评论。
8. Spring Boot 写入 Redis 翻译缓存。
9. 前端弹窗展示原文和译文，并支持复制译文。
```

### 6.7 商品对比分析流程

```text
1. 用户选择两个已经完成评论分析的商品。
2. 前端调用 Spring Boot 商品对比接口。
3. Spring Boot 校验两个商品 ID，检查 Redis 是否存在有效对比缓存。
4. 如果无缓存，Spring Boot 查询两个商品最新的 biz_comment_analysis_result。
5. Spring Boot 组装情感占比、关键词、差评原因、自定义标签和趋势数据。
6. Spring Boot 调用 Python AI 商品对比接口。
7. Python 调用大模型生成优势、风险和运营建议。
8. Spring Boot 保存对比报告到 biz_product_compare_report。
9. Spring Boot 写入 Redis 缓存。
10. 前端展示 A/B 对比报告和图表。
```

### 6.8 差评回复效果跟踪流程

```text
1. 用户复制或使用某条差评回复模板。
2. 前端调用使用记录接口，Spring Boot 将 use_count 加 1。
3. 用户后续根据处理结果标记 resolved、unresolved、positive_followup 或 no_feedback。
4. 商家可以收藏高质量回复模板，便于后续复用。
5. 前端通过回复历史接口查看使用次数、收藏状态和效果统计。
```

### 6.9 定时同步流程

```text
1. 用户进入定时同步页面，创建 Olist 目录、单 CSV 文件或公开样例爬虫同步配置。
2. Spring Boot 校验来源参数和 Cron 表达式，写入 biz_sync_config。
3. 若配置启用，Spring Boot 通过 Quartz 注册对应定时任务。
4. 到达执行时间或用户点击立即执行时，系统写入 biz_sync_execution。
5. Spring Boot 根据配置调用 CSV 导入或爬虫导入服务。
6. 导入任务继续沿用原有异步任务和 Redis 进度缓存。
7. 同步执行记录保存触发方式、关联任务、执行状态和错误信息。
8. 前端展示同步配置、下次运行时间和历史执行记录。
```

### 6.10 任务中心与数据报表流程

```text
1. 用户点击任务中心，前端调用 /api/tasks。
2. Spring Boot 聚合 biz_analysis_task、biz_crawl_task 和 biz_sync_execution，生成统一任务视图。
3. 用户可按任务类型、状态和关键词筛选，也可查看详情或重试历史任务。
4. 用户点击导出 CSV 时，前端调用 /api/tasks/export，后端复用相同筛选条件导出完整任务记录。
5. 用户点击数据报表，前端调用 /api/reports。
6. Spring Boot 优先读取 Redis 报表缓存，缓存不存在时聚合首页概览、评论趋势、情感分布、问题分布和商品排行。
7. 前端通过 ECharts 和表格展示全局运营复盘数据。
8. 用户点击报表导出时，前端调用 /api/reports/export，后端导出 UTF-8 BOM CSV，方便 Excel 打开。
```

## 7. 功能模块

### 7.1 用户认证模块

- 用户登录。
- 用户注册。
- JWT Token 签发与校验。
- 当前用户信息查询。

### 7.2 数据导入模块

- 阿里云 OSS 文件上传。
- CSV 数据导入。
- Olist 数据集导入。
- 爬虫数据采集。
- 爬虫任务状态查询。
- 导入任务状态查询。
- 导入结果统计。

### 7.3 商品管理模块

- 商品分页查询。
- 商品详情查询。
- 商品评论查询。
- 商品分析结果查询。

### 7.4 卖家管理模块

- 卖家分页查询。
- 卖家详情查询。
- 卖家经营概览查询。
- 卖家评论分析结果查询。

### 7.5 评论管理模块

- 评论分页查询。
- 评论详情查询。
- 按评分、情感、差评原因筛选。
- 负面评论列表查询。
- 评论智能标签手动编辑。
- 自定义评论标签保存与展示。
- 单条评论按当前界面语言翻译，并弹窗展示原文与译文。

### 7.6 评论分析模块

- 创建评论分析任务。
- 查询任务状态。
- 查询商品分析结果。
- 查询卖家分析结果。
- 负面关键词排行。
- 主题聚类统计。
- 自定义标签分布统计。
- 按日、周、月汇总评论情感趋势。
- 商品 A/B 对比分析。

### 7.7 AI 运营报告模块

- 生成商品运营报告。
- 生成卖家运营报告。
- 查询报告列表。
- 查询报告详情。

### 7.8 AI 文案生成模块

- 商品标题生成。
- 详情页文案生成。
- 促销话术生成。
- 短视频脚本生成。
- 生成历史查询。

### 7.9 差评回复模块

- 根据负面评论生成回复。
- 支持礼貌客观、真诚安抚、专业正式等语气。
- 回复历史查询。
- 回复模板使用次数记录。
- 回复效果标记。
- 高质量回复收藏。

### 7.10 数据看板模块

- 首页概览。
- 商品看板。
- 卖家看板。
- 情感分布。
- 评分分布。
- 关键词排行。
- 负面关键词排行。
- 差评原因分布。
- 主题聚类分布。
- 自定义标签分布。
- 评论时间趋势。

### 7.11 三语国际化模块

- 支持简体中文、英文、巴西葡语切换。
- 支持语言偏好本地持久化。
- 支持 AI 生成内容跟随当前语言输出。
- 保留原始评论数据原文，不对数据源内容做自动改写。
- 评论列表提供按需翻译按钮，翻译结果跟随当前界面语言展示。

### 7.12 系统治理与运维增强模块

- OpenAPI / Knife4j 接口注解：Controller、DTO、VO 补充接口说明，便于前后端联调和答辩展示。
- AI 限流：通过 Bucket4j 对 AI 报告、营销文案、差评回复和商品对比等高成本接口做用户级限流。
- Redis 缓存：缓存任务状态、分析结果、AI 报告、AI 文案和商品对比报告，降低重复查询和重复调用成本。
- 评论翻译缓存：单条评论翻译结果按评论 ID 和目标语言缓存，减少同一评论重复翻译成本。
- Redisson 扩展：预留分布式锁能力，为后续多实例部署、任务互斥和高并发场景做准备。
- Quartz 定时任务：定期扫描异常任务，自动修复长时间 processing 状态，提升演示和上线稳定性。
- 爬虫升级预留：当前低频样例爬虫保留，后续可切换 Scrapy / Crawlee 适配器增强采集能力。
- 生产展示控制：系统设置页普通商家仅展示任务缓存、告警阈值和 AI 调用偏好；开发环境或管理员可查看脱敏后的系统运行状态，不展示真实后端代理地址和本地调试地址。

### 7.13 定时同步中心模块

- 支持 Olist 本地目录、单 CSV 文件和公开样例爬虫三类同步配置。
- 支持 Cron 表达式配置、启用停用、立即触发和执行历史查询。
- 启动时自动恢复已启用 Quartz 任务，避免服务重启后定时同步丢失。
- 同步执行记录关联实际导入任务，便于从任务中心继续追踪进度。

### 7.14 统一任务中心与数据报表模块

- 统一任务中心聚合 CSV 导入、爬虫导入、评论分析和定时同步执行记录。
- 支持按任务类型、状态和关键词筛选，支持任务详情查看和重试。
- 支持任务记录 CSV 导出，导出内容与当前筛选条件一致，便于排查失败任务和项目汇报。
- 数据报表中心展示全局概览、评论趋势、情感分布、差评问题分布和商品排行。
- 数据报表支持 Redis 缓存，减少首页报表、趋势分布和商品排行的重复聚合查询。
- 支持全局运营报表 CSV 导出，包含概览指标、趋势、情感分布、问题分布和商品排行。
- 顶部快捷按钮分别进入独立页面，而不是页面内锚点或占位按钮。

## 8. 数据库设计

### 8.1 数据库分层

| 层级 | 表前缀 | 说明 |
|---|---|---|
| 原始数据层 | ods_* | 基本对应 Olist 原始 CSV |
| 业务分析层 | biz_* | 面向系统业务查询和分析展示 |
| 系统管理层 | sys_* | 用户、角色等系统数据 |

### 8.2 原始数据表

| 表名 | 说明 |
|---|---|
| ods_customers | Olist 客户原始数据 |
| ods_orders | Olist 订单原始数据 |
| ods_order_items | Olist 订单明细原始数据 |
| ods_order_reviews | Olist 评论原始数据 |
| ods_products | Olist 商品原始数据 |
| ods_sellers | Olist 卖家原始数据 |
| ods_category_translation | 商品类目翻译数据 |

### 8.3 业务核心表

| 表名 | 说明 |
|---|---|
| sys_user | 系统用户表 |
| sys_file_upload | 文件上传记录表 |
| biz_seller | 卖家业务表 |
| biz_product | 商品业务表 |
| biz_comment | 评论业务表 |
| biz_analysis_task | 分析任务表 |
| biz_crawl_task | 爬虫任务表，可选，也可并入 biz_analysis_task |
| biz_sync_config | 定时同步配置表 |
| biz_sync_execution | 定时同步执行记录表 |
| biz_task_record | 统一任务记录扩展表 |
| biz_comment_analysis_result | 评论分析结果表 |
| biz_operation_report | AI 运营报告表 |
| biz_ai_content_record | AI 文案生成记录表 |
| biz_negative_reply | 差评回复记录表 |
| biz_product_compare_report | 商品对比分析报告表 |

### 8.4 核心表关系

```text
sys_user 1 - n biz_analysis_task
biz_seller 1 - n biz_product
biz_seller 1 - n biz_comment
biz_product 1 - n biz_comment
biz_analysis_task 1 - 1 biz_comment_analysis_result
biz_analysis_task 1 - 1 biz_operation_report
biz_product 1 - n biz_ai_content_record
biz_comment 1 - n biz_negative_reply
biz_product n - n biz_product_compare_report
biz_sync_config 1 - n biz_sync_execution
biz_sync_execution n - 1 biz_analysis_task / biz_crawl_task
```

### 8.5 一期增强表与字段

| 表名 | 新增字段 / 新增表 | 说明 |
|---|---|---|
| biz_comment | manual_problem_type | 商家手动修正后的差评原因 |
| biz_comment | custom_tags | 商家自定义标签数组 |
| biz_comment | tag_update_time | 标签最后更新时间 |
| biz_comment_analysis_result | negative_keywords | 负面评论关键词排行 |
| biz_comment_analysis_result | score_distribution | 评分分布 |
| biz_comment_analysis_result | custom_tag_distribution | 自定义标签统计结果 |
| biz_comment_analysis_result | trend_distribution | 按时间粒度汇总的评论趋势 |
| biz_comment_analysis_result | problem_distribution | 同时承载差评原因分布和主题聚类分布，避免一期新增额外主题表 |
| biz_negative_reply | effect_tag | 回复效果标记：resolved / unresolved / positive_followup / no_feedback |
| biz_negative_reply | use_count | 回复模板被使用次数 |
| biz_negative_reply | favorite_flag | 是否收藏 |
| biz_product_compare_report | 整表新增 | 保存商品 A/B 对比分析报告 |
| biz_sync_config | 整表新增 | 保存定时同步来源、Cron、启用状态和下次运行时间 |
| biz_sync_execution | 整表新增 | 保存每次同步触发方式、执行状态和关联任务 |
| biz_task_record | 整表新增 | 预留统一任务中心扩展记录 |

已有数据库可执行 `aiops-server/src/main/resources/sql/upgrade-2026-08-20-operations-enhancement.sql` 完成字段升级；新库直接执行 `schema.sql` 即可。

### 8.6 爬虫任务表 biz_crawl_task

如果希望爬虫任务与普通分析任务分开管理，可以新增 `biz_crawl_task`；如果想简化实现，也可以把爬虫任务统一放入 `biz_analysis_task`，通过 `task_type = crawler_import` 区分。

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 创建用户 ID |
| platform | varchar(32) | 平台类型，如 taobao、pdd、temu、tiktok、other |
| target_url | varchar(1024) | 目标商品页或评论页 URL |
| target_type | varchar(64) | 采集目标：product / comment / product_comment |
| task_status | varchar(32) | pending / processing / success / failed |
| progress | int | 任务进度 0-100 |
| max_count | int | 最大采集数量 |
| success_count | int | 成功采集数量 |
| fail_count | int | 失败数量 |
| delay_seconds | int | 请求间隔秒数 |
| request_param | json | 请求参数快照 |
| error_message | text | 错误信息 |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### 8.7 文件上传记录表 sys_file_upload

CSV 原文件不建议直接存入 MySQL，建议存入阿里云 OSS，MySQL 只保存文件元信息。

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| user_id | bigint | 上传用户 ID |
| original_name | varchar(255) | 原始文件名 |
| file_name | varchar(255) | 存储后的文件名 |
| object_key | varchar(512) | OSS 对象 Key |
| file_url | varchar(1024) | 文件访问地址或临时访问地址 |
| file_type | varchar(64) | 文件类型，如 csv、xlsx |
| file_size | bigint | 文件大小 |
| business_type | varchar(64) | 业务类型，如 csv_import |
| status | tinyint | 状态：1 有效，0 删除 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### 8.8 定时同步与任务中心表

`biz_sync_config` 保存定时同步配置。

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| sync_name | varchar(128) | 同步名称 |
| source_type | varchar(32) | olist_directory / csv_file / crawler |
| data_source | varchar(32) | 数据来源 |
| import_mode | varchar(32) | full / incremental |
| data_path | varchar(1024) | 本地 Olist 数据目录 |
| file_id | bigint | 上传文件 ID |
| object_key | varchar(512) | OSS 对象 Key |
| file_url | varchar(1024) | CSV 文件 URL |
| platform | varchar(32) | 爬虫平台 |
| target_url | varchar(1024) | 爬虫目标 URL |
| cron_expression | varchar(128) | Quartz Cron 表达式 |
| auto_analysis | tinyint | 导入后是否自动分析 |
| enabled | tinyint | 是否启用 |
| last_run_time | datetime | 最近运行时间 |
| next_run_time | datetime | 下次运行时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

`biz_sync_execution` 保存每次同步执行记录。

| 字段名 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| config_id | bigint | 同步配置 ID |
| trigger_type | varchar(32) | manual / scheduled |
| execution_status | varchar(32) | processing / success / failed |
| linked_task_id | bigint | 关联导入任务 ID |
| linked_task_type | varchar(64) | csv_import / crawler_import |
| error_message | text | 错误信息 |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

`biz_task_record` 作为统一任务中心扩展表，当前主要用于同步任务记录，后续可逐步承接更多异步任务快照。

## 9. Redis 设计

| Key | Value | 说明 |
|---|---|---|
| login:token:{token} | userId | 登录状态缓存 |
| upload:csv:{fileId} | JSON | CSV 上传文件元信息 |
| task:status:{taskId} | processing / success / failed | 任务状态 |
| task:progress:{taskId} | 0-100 | 任务进度 |
| analysis:product:{productId} | JSON | 商品分析结果缓存 |
| analysis:seller:{sellerId} | JSON | 卖家分析结果缓存 |
| ai:report:{targetType}:{targetId} | JSON | AI 报告缓存 |
| ai:content:{hash} | text | AI 文案缓存 |
| ai:compare:product:{leftProductId}:{rightProductId} | JSON | 商品对比报告缓存 |
| ai:translation:comment:{commentId}:{language} | JSON | 单条评论翻译结果缓存 |
| report:overview | JSON | 全局报表总览缓存，默认 10 分钟 |
| report:distributions | JSON | 全局报表统计分布缓存，默认 10 分钟 |
| report:product-rank:{limit} | JSON | 商品排行缓存，默认 10 分钟 |
| rate:ai:user:{userId} | number / bucket | 用户 AI 调用限流标识，当前由 Bucket4j 执行令牌桶算法 |
| hot:keywords:product:{productId} | JSON | 商品高频关键词缓存 |

当商家手动修改评论标签或差评原因时，需要清理对应商品和卖家的分析结果缓存、AI 报告缓存，避免前端继续读取旧统计结果。

AI 限流默认配置为每个用户、每类 AI 业务 60 秒最多 20 次，可通过 `aiops.ai-rate-limit.capacity`、`refill-tokens` 和 `refill-period-seconds` 调整。任务维护默认 300 秒执行一次，超过 30 分钟未更新的 processing 任务会自动失败，可通过 `aiops.task-maintenance` 配置调整。

## 10. 非功能需求

### 10.1 性能需求

- 普通分页查询接口响应时间控制在 1 秒内。
- 评论分析和 AI 生成任务支持异步执行。
- 热点分析结果优先从 Redis 读取。
- AI 接口通过缓存避免重复调用。
- AI 高成本接口通过 Bucket4j 做用户级限流。
- Quartz 定时任务自动处理卡住的异步任务，减少人工修复。

### 10.2 安全需求

- 密码使用 BCrypt 加密。
- 接口使用 JWT 鉴权。
- 本版本保留 JWT Token 鉴权方案，不引入 Sa-Token，降低改造成本并保持现有登录逻辑稳定。
- 管理员接口需要角色校验。
- 阿里云 OSS 文件上传需要限制文件类型、文件大小和上传目录。
- OSS 文件访问建议使用私有 Bucket + 临时签名 URL，避免长期公开暴露。
- Python 内部接口不直接暴露给公网。
- AI 调用接口需要做用户级限流。

### 10.3 可扩展性需求

- 数据源可扩展到商家授权 CSV 或平台开放接口。
- 数据导入模块支持 CSV 导入和爬虫采集两种方式。
- 文件存储可从阿里云 OSS 扩展到 MinIO、腾讯云 COS 或本地文件存储。
- 大模型供应商可通过配置切换。
- 评论分析策略可从规则分析扩展为机器学习模型或大模型分类。
- 关键词提取可从规则模式扩展为 KeyBERT，主题聚类可从规则模式扩展为 BERTopic。
- 爬虫采集可从低频样例适配器升级为 Scrapy / Crawlee。
- 任务进度可从前端轮询升级为 WebSocket 推送。

## 11. 项目创新点

- 评论驱动运营决策：从用户真实反馈出发生成运营建议。
- Java 与 Python 双服务解耦：Java 管业务，Python 管分析和 AI。
- Redis 降本增效：缓存热点结果并限制 AI 高频调用。
- Bucket4j + Quartz 提升工程成熟度：既控制 AI 成本，又能自动处理异常任务。
- 关键词提取与主题聚类结合：不仅统计高频词，还能归纳物流、质量、价格等运营主题。
- 面向中小商家的低成本数字化工具：降低数据分析和内容生产门槛。
- 支持跨境电商多语言评论分析：Olist 评论以葡萄牙语为主，可生成中文运营报告。
- 前端三语切换：同一套系统可面向中文答辩、英文国际商家和葡语 Olist 数据场景展示。

## 12. 项目边界

- 系统定位为辅助决策工具，不承诺保证销量增长或盈利。
- Olist 数据集仅用于原型验证和学术研究。
- 一期以 CSV 导入作为稳定主流程，爬虫作为可演示的扩展数据接入方式。
- 爬虫模块仅做低频、少量公开数据采集，不绕过平台限制。
- AI 生成内容需要商家人工审核后再使用。

## 13. 一期开发计划

| 阶段 | 内容 | 周期 |
|---|---|---|
| 第 1 阶段 | 后端多模块工程搭建、公共类、配置、数据库连接 | 1 周 |
| 第 2 阶段 | MySQL 建表、Olist 数据导入、基础查询接口 | 1-2 周 |
| 第 3 阶段 | 商品、卖家、评论管理接口 | 1 周 |
| 第 4 阶段 | 分析任务机制、Redis 任务状态和缓存 | 1 周 |
| 第 5 阶段 | Java 调 Python 服务、评论分析结果落库 | 1-2 周 |
| 第 6 阶段 | AI 报告、AI 文案、差评回复接口 | 1-2 周 |
| 第 7 阶段 | 前后端联调、可视化看板、测试和答辩材料 | 1-2 周 |

## 14. 答辩表述

本系统以 Olist 公开电商数据集为基础，围绕中小电商商家在评论分析、用户痛点识别、运营建议生成和营销文案创作中的实际需求，设计并实现了评论驱动型 AI 智能运营助手。系统采用 Vue3、Spring Boot、MySQL、Redis 和 Python FastAPI 架构，由 Java 后端统一负责业务数据管理、任务调度、缓存限流和服务编排，由 Python 服务完成评论分析和大模型生成能力。系统能够将分散的用户评论转化为结构化运营洞察，帮助商家降低数据分析成本，提高数字化运营能力。
