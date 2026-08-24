# AI 智能运营助手接口文档

## 文档说明

本文档采用类似 YAPI 的接口描述形式。每个接口包含“基本信息”“请求参数”“返回数据”三部分。

当前 Java 后端 Controller、核心 DTO 和核心 VO 已补充 OpenAPI 注解，可通过 Knife4j / Swagger 页面查看接口分组、接口说明、请求参数和返回字段。本文档作为正式书面接口文档，字段说明与代码注解保持一致。

## 通用说明

### 通用请求头

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json | JSON 请求 |
| Authorization | Bearer Token | 否 | Bearer eyJhbGciOiJIUzI1NiJ9 | 除登录、注册外均需要 |

说明：本版本保留 JWT Bearer Token 鉴权方案，不升级 Sa-Token；前端仍在 `Authorization` 请求头中携带登录后获得的 Token。

### 通用返回结构

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| msg | string | 非必须 |  | 提示信息 |  |
| data | object | 非必须 |  | 响应数据 |  |

### 通用状态码

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

### 接口注解说明

- Controller 层使用 `@Tag` 和 `@Operation` 标注模块名称与接口用途。
- 路径参数和查询参数使用 `@Parameter` 标注含义。
- 核心 DTO / VO 使用 `@Schema` 标注字段说明，便于 Knife4j 自动生成接口文档。

### AI 限流说明

AI 报告、AI 文案、差评回复、商品对比报告等高成本接口接入 Bucket4j 用户级限流。默认配置为每个用户、每类 AI 业务 60 秒最多 20 次；超过限制时返回 `429`，前端应提示用户稍后再试。

### 异步任务维护说明

评论分析、CSV 导入、爬虫导入等异步任务会写入 MySQL 与 Redis。系统通过 Quartz 定时扫描长时间处于 `processing` 的任务，默认 30 分钟未更新则自动标记为 `failed`，避免任务状态长期卡住。

Redisson 属于后端工程治理能力，用于后续多实例部署、分布式锁和任务互斥扩展，不单独暴露 HTTP 接口。

# 文件上传相关接口

## 上传 CSV 文件

### 基本信息

**Path：** /api/files/upload

**Method：** POST

**接口描述：** 上传 Olist CSV 或商家授权 CSV 文件到阿里云 OSS，返回文件 ID、OSS 对象 Key 和文件访问地址。该接口用于后续创建 CSV 数据导入任务。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | multipart/form-data | 是 | multipart/form-data | 文件上传 |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**FormData**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| file | 是 | olist_order_reviews_dataset.csv | CSV 文件 |
| businessType | 否 | csv_import | 业务类型 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 上传结果 |  |
| ├─ fileId | integer | 非必须 |  | 文件记录 ID | format: int64 |
| ├─ originalName | string | 非必须 |  | 原始文件名 |  |
| ├─ objectKey | string | 非必须 |  | OSS 对象 Key |  |
| ├─ fileUrl | string | 非必须 |  | 文件访问地址或临时访问地址 |  |
| ├─ fileSize | integer | 非必须 |  | 文件大小 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 获取文件临时访问地址

### 基本信息

**Path：** /api/files/{fileId}/url

**Method：** GET

**接口描述：** 获取指定 OSS 文件的临时签名访问地址，供预览或 Python 服务下载使用。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| fileId | 是 | 60001 | 文件记录 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 文件访问数据 |  |
| ├─ fileId | integer | 非必须 |  | 文件记录 ID | format: int64 |
| ├─ objectKey | string | 非必须 |  | OSS 对象 Key |  |
| ├─ signedUrl | string | 非必须 |  | 临时签名 URL |  |
| ├─ expireSeconds | integer | 非必须 | 3600 | 过期时间 | format: int32 |
| msg | string | 非必须 |  | 提示信息 |  |

# 用户认证相关接口

## 用户登录

### 基本信息

**Path：** /api/auth/login

**Method：** POST

**接口描述：** 用户登录，登录成功后返回 JWT Token。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| username | string | 必须 |  | 用户名 |  |
| password | string | 必须 |  | 密码 |  |

**请求示例**

```json
{
  "username": "admin",
  "password": "123456"
}
```

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 登录返回数据 |  |
| ├─ token | string | 非必须 |  | JWT 令牌 |  |
| ├─ userId | integer | 非必须 |  | 用户 ID | format: int64 |
| ├─ username | string | 非必须 |  | 用户名 |  |
| ├─ role | string | 非必须 |  | 用户角色 | admin / merchant |
| msg | string | 非必须 |  | 提示信息 |  |

## 用户注册

### 基本信息

**Path：** /api/auth/register

**Method：** POST

**接口描述：** 注册普通商家用户。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| username | string | 必须 |  | 用户名 |  |
| password | string | 必须 |  | 密码 |  |
| nickname | string | 非必须 |  | 昵称 |  |
| email | string | 非必须 |  | 邮箱 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | string | 非必须 |  | 返回数据 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询当前用户信息

### 基本信息

**Path：** /api/user/profile

**Method：** GET

**接口描述：** 查询当前登录用户信息。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 用户信息 |  |
| ├─ userId | integer | 非必须 |  | 用户 ID | format: int64 |
| ├─ username | string | 非必须 |  | 用户名 |  |
| ├─ nickname | string | 非必须 |  | 昵称 |  |
| ├─ role | string | 非必须 |  | 角色 |  |
| msg | string | 非必须 |  | 提示信息 |  |

# 数据导入相关接口

## CSV 数据导入

### 基本信息

**Path：** /api/data/import/csv

**Method：** POST

**接口描述：** 创建 CSV 数据导入任务，可导入 Olist 数据集或商家授权导出的商品评论 CSV，由 Java 后端调用 Python 服务完成数据读取、清洗和入库。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| fileId | integer | 非必须 |  | 文件记录 ID | format: int64 |
| objectKey | string | 非必须 |  | 阿里云 OSS 对象 Key | 与 fileId、fileUrl 至少传一个 |
| fileUrl | string | 非必须 |  | 文件访问地址或临时签名 URL | 与 fileId、objectKey 至少传一个 |
| dataPath | string | 非必须 |  | 本地 CSV 文件或目录路径 | 本地开发可用 |
| dataSource | string | 非必须 | olist | 数据来源 | olist / merchant_csv |
| importMode | string | 非必须 | full | 导入模式 | full / incremental |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 任务数据 |  |
| ├─ taskId | integer | 非必须 |  | 任务 ID | format: int64 |
| ├─ taskStatus | string | 非必须 |  | 任务状态 | processing |
| ├─ fileId | integer | 非必须 |  | 文件记录 ID | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 爬虫数据导入

### 基本信息

**Path：** /api/data/import/crawler

**Method：** POST

**接口描述：** 创建爬虫采集任务，采集公开商品信息和评论样例。Java 后端负责任务管理，Python 服务负责实际采集、清洗和结构化处理。当前默认使用低频样例采集适配器，后续可切换 Scrapy / Crawlee 适配器增强采集能力。仅用于学习研究和原型演示，不用于大规模商业爬取。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| platform | string | 必须 |  | 平台类型 | taobao / pdd / temu / tiktok / other |
| targetUrl | string | 必须 |  | 商品页或评论页 URL |  |
| targetType | string | 非必须 | product_comment | 采集目标 | product / comment / product_comment |
| maxCount | integer | 非必须 | 100 | 最大采集数量 | format: int32 |
| delaySeconds | integer | 非必须 | 3 | 请求间隔秒数 | format: int32 |
| remark | string | 非必须 |  | 任务备注 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 任务数据 |  |
| ├─ taskId | integer | 非必须 |  | 任务 ID | format: int64 |
| ├─ taskStatus | string | 非必须 |  | 任务状态 | processing |
| ├─ platform | string | 非必须 |  | 平台类型 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询导入任务详情

### 基本信息

**Path：** /api/data/import/tasks/{taskId}

**Method：** GET

**接口描述：** 查询 CSV 导入或爬虫导入任务状态和导入结果。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| taskId | 是 | 10001 | 任务 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 导入任务详情 |  |
| ├─ taskId | integer | 非必须 |  | 任务 ID | format: int64 |
| ├─ taskStatus | string | 非必须 |  | 任务状态 | pending / processing / success / failed |
| ├─ importType | string | 非必须 |  | 导入方式 | csv / crawler |
| ├─ progress | integer | 非必须 |  | 任务进度 | format: int32 |
| ├─ importedRows | integer | 非必须 |  | 导入行数 | format: int32 |
| ├─ successCount | integer | 非必须 |  | 成功条数 | format: int32 |
| ├─ failCount | integer | 非必须 |  | 失败条数 | format: int32 |
| ├─ errorMessage | string | 非必须 |  | 错误信息 |  |
| msg | string | 非必须 |  | 提示信息 |  |

# 商品相关接口

## 商品分页查询

### 基本信息

**Path：** /api/products

**Method：** GET

**接口描述：** 分页查询商品列表，支持按类目、评分和价格区间筛选。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |
| categoryNameEn | 否 | health_beauty | 英文类目 |
| minScore | 否 | 4 | 最低评分 |
| maxScore | 否 | 5 | 最高评分 |
| keyword | 否 | beauty | 商品关键字 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 商品列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| ├─ pageNum | integer | 非必须 |  | 当前页 | format: int32 |
| ├─ pageSize | integer | 非必须 |  | 每页条数 | format: int32 |
| msg | string | 非必须 |  | 提示信息 |  |

## 根据 ID 查询商品

### 基本信息

**Path：** /api/products/{productId}

**Method：** GET

**接口描述：** 查询商品详情。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| productId | 是 | abc123 | Olist 商品 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 商品详情 |  |
| ├─ productId | string | 非必须 |  | 商品 ID |  |
| ├─ sellerId | string | 非必须 |  | 卖家 ID |  |
| ├─ categoryNameEn | string | 非必须 |  | 英文类目 |  |
| ├─ avgPrice | number | 非必须 |  | 平均价格 | format: decimal |
| ├─ reviewCount | integer | 非必须 |  | 评论数量 | format: int32 |
| ├─ avgScore | number | 非必须 |  | 平均评分 | format: decimal |
| ├─ negativeRate | number | 非必须 |  | 差评率 | format: decimal |
| msg | string | 非必须 |  | 提示信息 |  |

# 卖家相关接口

## 卖家分页查询

### 基本信息

**Path：** /api/sellers

**Method：** GET

**接口描述：** 分页查询卖家列表。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |
| state | 否 | SP | 卖家所在州 |
| minScore | 否 | 4 | 最低平均评分 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 卖家列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询卖家经营概览

### 基本信息

**Path：** /api/sellers/{sellerId}/overview

**Method：** GET

**接口描述：** 查询卖家商品数量、订单数量、平均评分、差评率和主要类目。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| sellerId | 是 | seller001 | Olist 卖家 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 卖家概览 |  |
| ├─ sellerId | string | 非必须 |  | 卖家 ID |  |
| ├─ productCount | integer | 非必须 |  | 商品数量 | format: int32 |
| ├─ orderCount | integer | 非必须 |  | 订单数量 | format: int32 |
| ├─ avgScore | number | 非必须 |  | 平均评分 | format: decimal |
| ├─ negativeRate | number | 非必须 |  | 差评率 | format: decimal |
| ├─ mainCategories | array | 非必须 |  | 主要类目 |  |
| msg | string | 非必须 |  | 提示信息 |  |

# 评论相关接口

## 评论分页查询

### 基本信息

**Path：** /api/comments

**Method：** GET

**接口描述：** 分页查询评论，支持商品、卖家、评分、情感类型和差评原因筛选。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |
| productId | 否 | abc123 | 商品 ID |
| sellerId | 否 | seller001 | 卖家 ID |
| sentiment | 否 | negative | 情感类型 |
| problemType | 否 | logistics | 差评原因类型 |
| minScore | 否 | 1 | 最低评分 |
| maxScore | 否 | 3 | 最高评分 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 评论列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 根据 ID 查询评论

### 基本信息

**Path：** /api/comments/{commentId}

**Method：** GET

**接口描述：** 查询评论详情。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| commentId | 是 | 101 | 评论主键 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 评论详情 |  |
| ├─ id | integer | 非必须 |  | 评论 ID | format: int64 |
| ├─ productId | string | 非必须 |  | 商品 ID |  |
| ├─ sellerId | string | 非必须 |  | 卖家 ID |  |
| ├─ reviewScore | integer | 非必须 |  | 评分 | format: int32 |
| ├─ reviewContent | string | 非必须 |  | 评论内容 |  |
| ├─ sentiment | string | 非必须 |  | 情感类型 |  |
| ├─ systemProblemType | string | 非必须 |  | 系统识别差评原因 |  |
| ├─ manualProblemType | string | 非必须 |  | 人工修正差评原因 |  |
| ├─ effectiveProblemType | string | 非必须 |  | 最终生效差评原因，优先人工修正 |  |
| ├─ customTags | array | 非必须 |  | 商家自定义标签 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 更新评论标签

### 基本信息

**Path：** /api/comments/{commentId}/tags

**Method：** PUT

**接口描述：** 手动修正单条评论的差评原因，并维护商家自定义标签。修改后会清理对应商品和卖家的分析缓存。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| commentId | 是 | 101 | 评论主键 ID |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| manualProblemType | string | 非必须 |  | 人工差评原因 | logistics / quality / size / service / package / price |
| customTags | array | 非必须 |  | 自定义标签 | 如 ["物流慢", "包装破损"] |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 更新后的评论详情 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询负面评论

### 基本信息

**Path：** /api/comments/negative

**Method：** GET

**接口描述：** 查询负面评论列表，用于差评回复生成。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| productId | 否 | abc123 | 商品 ID |
| sellerId | 否 | seller001 | 卖家 ID |
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 负面评论列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

# 评论分析相关接口

## 创建评论分析任务

### 基本信息

**Path：** /api/analysis/tasks

**Method：** POST

**接口描述：** 按商品或卖家创建评论分析任务。任务异步执行，状态写入 MySQL 和 Redis；如果任务异常卡住，Quartz 维护任务会按配置自动标记失败。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| targetType | string | 必须 |  | 分析对象类型 | product / seller |
| targetId | string | 必须 |  | 分析对象 ID |  |
| analysisType | string | 非必须 | comment_analysis | 分析类型 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 任务数据 |  |
| ├─ taskId | integer | 非必须 |  | 任务 ID | format: int64 |
| ├─ taskStatus | string | 非必须 |  | 任务状态 | processing |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询分析任务状态

### 基本信息

**Path：** /api/analysis/tasks/{taskId}

**Method：** GET

**接口描述：** 查询分析任务状态和执行进度。前端可通过轮询该接口展示任务进度。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| taskId | 是 | 20001 | 任务 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 任务状态 |  |
| ├─ taskId | integer | 非必须 |  | 任务 ID | format: int64 |
| ├─ taskStatus | string | 非必须 |  | 任务状态 | pending / processing / success / failed |
| ├─ progress | integer | 非必须 |  | 任务进度 | format: int32 |
| ├─ errorMessage | string | 非必须 |  | 错误信息 | 任务失败或 Quartz 自动标记失败时返回 |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询商品评论分析结果

### 基本信息

**Path：** /api/analysis/product/{productId}

**Method：** GET

**接口描述：** 查询指定商品的评论情感分布、关键词、主题聚类和差评原因分布。当前主题聚类结果与差评原因分布统一放入 `problemDistribution`，避免一期额外新增主题表。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| productId | 是 | abc123 | 商品 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分析结果 |  |
| ├─ targetType | string | 非必须 |  | 分析对象类型 | product |
| ├─ targetId | string | 非必须 |  | 商品 ID |  |
| ├─ totalCount | integer | 非必须 |  | 评论总数 | format: int32 |
| ├─ positiveCount | integer | 非必须 |  | 正面评论数 | format: int32 |
| ├─ neutralCount | integer | 非必须 |  | 中性评论数 | format: int32 |
| ├─ negativeCount | integer | 非必须 |  | 负面评论数 | format: int32 |
| ├─ positiveRate | number | 非必须 |  | 好评率 | format: decimal |
| ├─ negativeRate | number | 非必须 |  | 差评率 | format: decimal |
| ├─ topKeywords | array | 非必须 |  | 高频关键词 |  |
| ├─ negativeKeywords | array | 非必须 |  | 负面关键词排行 |  |
| ├─ scoreDistribution | array | 非必须 |  | 评分分布 |  |
| ├─ problemDistribution | array | 非必须 |  | 差评原因分布 / 主题聚类分布 | 包含物流、质量、包装、价格、服务、尺寸、other 等主题 |
| ├─ customTagDistribution | array | 非必须 |  | 自定义标签分布 |  |
| ├─ trendDistribution | array | 非必须 |  | 评论趋势 | 按日 / 周 / 月 |
| ├─ summary | string | 非必须 |  | 分析摘要 |  |
| ├─ createTime | string | 非必须 |  | 分析生成时间 | format: date-time |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询卖家评论分析结果

### 基本信息

**Path：** /api/analysis/seller/{sellerId}

**Method：** GET

**接口描述：** 查询指定卖家的评论分析结果，包含情感统计、关键词、主题聚类、差评原因和评论趋势。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| sellerId | 是 | seller001 | 卖家 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 卖家分析结果 |  |
| ├─ targetType | string | 非必须 |  | 分析对象类型 | seller |
| ├─ targetId | string | 非必须 |  | 卖家 ID |  |
| ├─ totalCount | integer | 非必须 |  | 评论总数 | format: int32 |
| ├─ positiveRate | number | 非必须 |  | 好评率 | format: decimal |
| ├─ negativeRate | number | 非必须 |  | 差评率 | format: decimal |
| ├─ topKeywords | array | 非必须 |  | 高频关键词 |  |
| ├─ negativeKeywords | array | 非必须 |  | 负面关键词排行 |  |
| ├─ scoreDistribution | array | 非必须 |  | 评分分布 |  |
| ├─ problemDistribution | array | 非必须 |  | 差评原因分布 / 主题聚类分布 | 包含物流、质量、包装、价格、服务、尺寸、other 等主题 |
| ├─ customTagDistribution | array | 非必须 |  | 自定义标签分布 |  |
| ├─ trendDistribution | array | 非必须 |  | 评论趋势 | 按日 / 周 / 月 |
| msg | string | 非必须 |  | 提示信息 |  |

## 商品对比分析

### 基本信息

**Path：** /api/analysis/products/compare

**Method：** POST

**接口描述：** 选择两个已完成评论分析的商品，生成 A/B 对比报告。默认优先读取 Redis 缓存，forceRefresh=true 时重新调用 Python AI 服务。重新生成时受 Bucket4j AI 限流控制，超过限制返回 `429`。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| leftProductId | string | 必须 |  | 左侧商品 ID |  |
| rightProductId | string | 必须 |  | 右侧商品 ID |  |
| language | string | 非必须 | zh-CN | 报告语言 |  |
| forceRefresh | boolean | 非必须 | false | 是否跳过缓存重新生成 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 商品对比报告 |  |
| ├─ reportId | integer | 非必须 |  | 报告 ID | format: int64 |
| ├─ leftProductId | string | 非必须 |  | 左侧商品 ID |  |
| ├─ rightProductId | string | 非必须 |  | 右侧商品 ID |  |
| ├─ metricSnapshot | string | 非必须 |  | 两个商品分析指标快照 JSON |  |
| ├─ compareSummary | string | 非必须 |  | 对比摘要 |  |
| ├─ advantageAnalysis | string | 非必须 |  | 优势分析 |  |
| ├─ riskAnalysis | string | 非必须 |  | 风险分析 |  |
| ├─ operationSuggestions | string | 非必须 |  | 运营建议 |  |
| ├─ modelName | string | 非必须 |  | 模型名称 |  |
| ├─ createTime | string | 非必须 |  | 创建时间 | format: date-time |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询商品对比报告列表

### 基本信息

**Path：** /api/analysis/products/compare

**Method：** GET

**接口描述：** 分页查询商品对比报告历史。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| leftProductId | 否 | abc123 | 左侧商品 ID |
| rightProductId | 否 | def456 | 右侧商品 ID |
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 对比报告列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询商品对比报告详情

### 基本信息

**Path：** /api/analysis/products/compare/{reportId}

**Method：** GET

**接口描述：** 查询单个商品对比报告详情。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| reportId | 是 | 90001 | 报告 ID |

### 返回数据

同“商品对比分析”返回数据。

# AI 运营报告相关接口

## 生成商品运营报告

### 基本信息

**Path：** /api/ai/reports/product

**Method：** POST

**接口描述：** 基于商品评论分析结果生成 AI 运营报告。默认优先读取 Redis 缓存，forceRefresh=true 或无缓存时进入 Bucket4j AI 限流并调用 Python 大模型服务；超过限制返回 `429`。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| productId | string | 必须 |  | 商品 ID |  |
| forceRefresh | boolean | 非必须 | false | 是否强制重新生成 |  |
| language | string | 非必须 | zh-CN | 生成语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 报告数据 |  |
| ├─ reportId | integer | 非必须 |  | 报告 ID | format: int64 |
| ├─ reportTitle | string | 非必须 |  | 报告标题 |  |
| ├─ consumerPainPoints | string | 非必须 |  | 消费者痛点 |  |
| ├─ productAdvantages | string | 非必须 |  | 商品优势 |  |
| ├─ productDisadvantages | string | 非必须 |  | 商品不足 |  |
| ├─ operationSuggestions | string | 非必须 |  | 运营建议 |  |
| ├─ copywritingSuggestions | string | 非必须 |  | 文案建议 |  |
| ├─ serviceSuggestions | string | 非必须 |  | 客服建议 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 生成卖家运营报告

### 基本信息

**Path：** /api/ai/reports/seller

**Method：** POST

**接口描述：** 基于卖家整体评论分析结果生成 AI 运营报告。默认优先读取 Redis 缓存，forceRefresh=true 或无缓存时进入 Bucket4j AI 限流并调用 Python 大模型服务；超过限制返回 `429`。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| sellerId | string | 必须 |  | 卖家 ID |  |
| forceRefresh | boolean | 非必须 | false | 是否强制重新生成 |  |
| language | string | 非必须 | zh-CN | 生成语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 报告数据 |  |
| ├─ reportId | integer | 非必须 |  | 报告 ID | format: int64 |
| ├─ reportTitle | string | 非必须 |  | 报告标题 |  |
| ├─ operationSuggestions | string | 非必须 |  | 运营建议 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询报告列表

### 基本信息

**Path：** /api/ai/reports

**Method：** GET

**接口描述：** 分页查询 AI 运营报告历史。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| targetType | 否 | product | product / seller |
| targetId | 否 | abc123 | 对象 ID |
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 报告列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询报告详情

### 基本信息

**Path：** /api/ai/reports/{reportId}

**Method：** GET

**接口描述：** 查询 AI 运营报告详情。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| reportId | 是 | 30001 | 报告 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 报告详情 |  |
| ├─ reportId | integer | 非必须 |  | 报告 ID | format: int64 |
| ├─ fullReport | string | 非必须 |  | 完整报告 |  |
| ├─ modelName | string | 非必须 |  | 模型名称 |  |
| ├─ createTime | string | 非必须 |  | 创建时间 | datetime |
| msg | string | 非必须 |  | 提示信息 |  |

# AI 文案相关接口

## 生成营销文案

### 基本信息

**Path：** /api/ai/contents

**Method：** POST

**接口描述：** 生成商品标题、详情页文案、促销话术或短视频脚本。该接口属于高成本 AI 接口，受 Bucket4j 用户级限流控制；超过限制返回 `429`。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| targetType | string | 必须 |  | 对象类型 | product / seller |
| targetId | string | 必须 |  | 对象 ID |  |
| contentType | string | 必须 |  | 文案类型 | title / detail / promotion / short_video |
| styleType | string | 非必须 | simple | 文案风格 | simple / viral / value / professional |
| language | string | 非必须 | zh-CN | 生成语言 |  |
| extraRequirement | string | 非必须 |  | 额外要求 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 生成结果 |  |
| ├─ contentId | integer | 非必须 |  | 文案记录 ID | format: int64 |
| ├─ generatedContent | string | 非必须 |  | 生成内容 |  |
| ├─ modelName | string | 非必须 |  | 模型名称 |  |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询文案生成历史

### 基本信息

**Path：** /api/ai/contents

**Method：** GET

**接口描述：** 分页查询 AI 文案生成历史。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| targetType | 否 | product | 对象类型 |
| targetId | 否 | abc123 | 对象 ID |
| contentType | 否 | title | 文案类型 |
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 文案记录列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

# 差评回复相关接口

## 生成差评回复

### 基本信息

**Path：** /api/ai/negative-replies

**Method：** POST

**接口描述：** 根据负面评论内容生成客服回复模板。系统会读取单条评论内容、评分、商品 ID、卖家 ID 和差评原因，生成尽量贴合该评论的独立回复；该接口受 Bucket4j 用户级限流控制，超过限制返回 `429`。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Content-Type | application/json | 是 | application/json |  |
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| commentId | integer | 必须 |  | 评论 ID | format: int64 |
| toneType | string | 非必须 | sincere | 回复语气 | polite / sincere / professional |
| language | string | 非必须 | zh-CN | 生成语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 回复结果 |  |
| ├─ replyId | integer | 非必须 |  | 回复记录 ID | format: int64 |
| ├─ commentId | integer | 非必须 |  | 评论 ID | format: int64 |
| ├─ productId | string | 非必须 |  | 商品 ID |  |
| ├─ sellerId | string | 非必须 |  | 卖家 ID |  |
| ├─ problemType | string | 非必须 |  | 差评原因类型 |  |
| ├─ commentContent | string | 非必须 |  | 原始评论内容 |  |
| ├─ toneType | string | 非必须 |  | 回复语气 |  |
| ├─ replyContent | string | 非必须 |  | 回复内容 |  |
| ├─ modelName | string | 非必须 |  | 模型名称 |  |
| ├─ effectTag | string | 非必须 |  | 回复效果标记 |  |
| ├─ useCount | integer | 非必须 | 0 | 使用次数 | format: int32 |
| ├─ favoriteFlag | integer | 非必须 | 0 | 是否收藏 | 1 收藏，0 未收藏 |
| ├─ createTime | string | 非必须 |  | 创建时间 | format: date-time |
| ├─ updateTime | string | 非必须 |  | 更新时间 | format: date-time |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询差评回复历史

### 基本信息

**Path：** /api/ai/negative-replies

**Method：** GET

**接口描述：** 分页查询差评回复生成历史。

### 请求参数

**Query**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| productId | 否 | abc123 | 商品 ID |
| sellerId | 否 | seller001 | 卖家 ID |
| pageNum | 否 | 1 | 页码 |
| pageSize | 否 | 10 | 每页条数 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 分页数据 |  |
| ├─ records | array | 非必须 |  | 回复记录列表 |  |
| ├─ total | integer | 非必须 |  | 总数 | format: int64 |
| msg | string | 非必须 |  | 提示信息 |  |

## 记录差评回复使用

### 基本信息

**Path：** /api/ai/negative-replies/{replyId}/use

**Method：** POST

**接口描述：** 商家复制或使用某条回复模板后调用，系统将 useCount 加 1。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| replyId | 是 | 70001 | 回复记录 ID |

### 返回数据

同“生成差评回复”返回数据。

## 更新差评回复效果

### 基本信息

**Path：** /api/ai/negative-replies/{replyId}/effect

**Method：** PUT

**接口描述：** 标记某条回复模板的后续处理效果，用于统计哪些话术更有效。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| replyId | 是 | 70001 | 回复记录 ID |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| effectTag | string | 非必须 |  | 回复效果 | resolved / unresolved / positive_followup / no_feedback |

### 返回数据

同“生成差评回复”返回数据。

## 更新差评回复收藏状态

### 基本信息

**Path：** /api/ai/negative-replies/{replyId}/favorite

**Method：** PUT

**接口描述：** 收藏或取消收藏某条差评回复模板。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| replyId | 是 | 70001 | 回复记录 ID |

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| favoriteFlag | integer | 必须 |  | 收藏状态 | 1 收藏，0 取消收藏 |

### 返回数据

同“生成差评回复”返回数据。

# 数据看板相关接口

## 查询首页概览

### 基本信息

**Path：** /api/dashboard/overview

**Method：** GET

**接口描述：** 查询系统首页统计数据。

### 请求参数

**Headers**

| 参数名称 | 参数值 | 是否必须 | 示例 | 备注 |
|---|---|---|---|---|
| Authorization | Bearer Token | 是 | Bearer token | JWT 令牌 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 首页统计 |  |
| ├─ productCount | integer | 非必须 |  | 商品数量 | format: int32 |
| ├─ sellerCount | integer | 非必须 |  | 卖家数量 | format: int32 |
| ├─ commentCount | integer | 非必须 |  | 评论数量 | format: int32 |
| ├─ avgScore | number | 非必须 |  | 平均评分 | format: decimal |
| ├─ negativeRate | number | 非必须 |  | 差评率 | format: decimal |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询商品看板

### 基本信息

**Path：** /api/dashboard/product/{productId}

**Method：** GET

**接口描述：** 查询商品评分分布、情感分布、关键词排行和差评原因分布。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| productId | 是 | abc123 | 商品 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 商品看板数据 |  |
| ├─ scoreDistribution | array | 非必须 |  | 评分分布 |  |
| ├─ sentimentDistribution | array | 非必须 |  | 情感分布 |  |
| ├─ keywordRank | array | 非必须 |  | 关键词排行 |  |
| ├─ negativeKeywordRank | array | 非必须 |  | 负面关键词排行 |  |
| ├─ problemDistribution | array | 非必须 |  | 差评原因分布 |  |
| ├─ customTagDistribution | array | 非必须 |  | 自定义标签分布 |  |
| ├─ trendDistribution | array | 非必须 |  | 评论趋势 | 按日 / 周 / 月 |
| msg | string | 非必须 |  | 提示信息 |  |

## 查询卖家看板

### 基本信息

**Path：** /api/dashboard/seller/{sellerId}

**Method：** GET

**接口描述：** 查询卖家整体经营看板。

### 请求参数

**路径参数**

| 参数名称 | 是否必须 | 示例 | 备注 |
|---|---|---|---|
| sellerId | 是 | seller001 | 卖家 ID |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| code | integer | 必须 |  | 状态码 | format: int32 |
| data | object | 非必须 |  | 卖家看板数据 |  |
| ├─ scoreDistribution | array | 非必须 |  | 评分分布 |  |
| ├─ sentimentDistribution | array | 非必须 |  | 情感分布 |  |
| ├─ categoryDistribution | array | 非必须 |  | 类目分布 |  |
| ├─ keywordRank | array | 非必须 |  | 关键词排行 |  |
| ├─ negativeKeywordRank | array | 非必须 |  | 负面关键词排行 |  |
| ├─ problemDistribution | array | 非必须 |  | 差评原因分布 |  |
| ├─ customTagDistribution | array | 非必须 |  | 自定义标签分布 |  |
| ├─ trendDistribution | array | 非必须 |  | 评论趋势 | 按日 / 周 / 月 |
| msg | string | 非必须 |  | 提示信息 |  |

# Python 内部服务接口

Python 内部服务由 Java 后端调用，一般部署在内网或本机，不建议直接暴露公网。关键词提取默认使用规则模式，可通过 `AIOPS_KEYWORD_EXTRACTOR=keybert` 切换 KeyBERT；主题聚类默认使用规则模式，可通过 `AIOPS_TOPIC_CLUSTERER=bertopic` 切换 BERTopic。Scrapy / Crawlee 属于后续爬虫升级适配器，默认不作为一期必装依赖。

## CSV 数据导入

### 基本信息

**Path：** /internal/csv/import

**Method：** POST

**接口描述：** Java 后端调用 Python 服务，执行 Olist CSV 或商家授权 CSV 的数据清洗和入库。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| taskId | integer | 必须 |  | 任务 ID | format: int64 |
| fileId | integer | 非必须 |  | 文件记录 ID | format: int64 |
| objectKey | string | 非必须 |  | 阿里云 OSS 对象 Key |  |
| fileUrl | string | 非必须 |  | 文件访问地址或临时签名 URL |  |
| dataPath | string | 非必须 |  | 本地数据集路径 | 本地开发可用 |
| dataSource | string | 非必须 | olist | 数据来源 | olist / merchant_csv |
| importMode | string | 非必须 | full | 导入模式 | full / incremental |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| importedRows | integer | 非必须 |  | 导入行数 | format: int32 |
| message | string | 非必须 |  | 提示信息 |  |

## 爬虫数据导入

### 基本信息

**Path：** /internal/crawler/import

**Method：** POST

**接口描述：** Java 后端调用 Python 爬虫服务，低频采集公开商品信息和评论样例。当前提供 sample 低频样例适配器，后续可接入 Scrapy / Crawlee 适配器。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| taskId | integer | 必须 |  | 任务 ID | format: int64 |
| platform | string | 必须 |  | 平台类型 | taobao / pdd / temu / tiktok / other |
| targetUrl | string | 必须 |  | 商品页或评论页 URL |  |
| targetType | string | 非必须 | product_comment | 采集目标 | product / comment / product_comment |
| maxCount | integer | 非必须 | 100 | 最大采集数量 | format: int32 |
| delaySeconds | integer | 非必须 | 3 | 请求间隔秒数 | format: int32 |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| data | object | 非必须 |  | 爬虫导入结果 |  |
| ├─ productCount | integer | 非必须 |  | 商品数量 | format: int32 |
| ├─ commentCount | integer | 非必须 |  | 评论数量 | format: int32 |
| ├─ successCount | integer | 非必须 |  | 成功数量 | format: int32 |
| ├─ failCount | integer | 非必须 |  | 失败数量 | format: int32 |
| message | string | 非必须 |  | 提示信息 |  |

## 评论分析

### 基本信息

**Path：** /internal/analysis/comments

**Method：** POST

**接口描述：** Java 后端调用 Python 服务，对评论进行情感分析、关键词提取、主题聚类和差评原因分类。关键词提取会过滤 nan / none / null / undefined 等无效词；主题聚类默认覆盖 logistics、quality、package、price、service、size、other 等主题。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| taskId | integer | 必须 |  | 任务 ID | format: int64 |
| targetType | string | 必须 |  | 分析对象类型 | product / seller |
| targetId | string | 必须 |  | 分析对象 ID |  |
| trendGranularity | string | 非必须 | month | 趋势粒度 | day / week / month |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| analysisResultId | integer | 非必须 |  | 分析结果 ID | format: int64 |
| totalCount | integer | 非必须 |  | 本次分析评论总数 | format: int32 |
| message | string | 非必须 |  | 提示信息 |  |

说明：高频关键词、负面关键词、差评原因 / 主题聚类分布、时间趋势等详细结果由 Python 写入 `biz_comment_analysis_result`，Java 后端再通过“查询商品评论分析结果”或“查询卖家评论分析结果”接口返回给前端。

## AI 报告生成

### 基本信息

**Path：** /internal/ai/report

**Method：** POST

**接口描述：** Java 后端调用 Python 服务，根据评论分析结果生成 AI 运营报告。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| targetType | string | 必须 |  | 对象类型 | product / seller |
| targetId | string | 必须 |  | 对象 ID |  |
| analysisResult | object | 必须 |  | 评论分析结果 |  |
| language | string | 非必须 | zh-CN | 生成语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| data | object | 非必须 |  | AI 报告结果 |  |
| message | string | 非必须 |  | 提示信息 |  |

## AI 文案生成

### 基本信息

**Path：** /internal/ai/content

**Method：** POST

**接口描述：** Java 后端调用 Python 服务生成营销文案。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| targetType | string | 必须 |  | 对象类型 | product / seller |
| targetId | string | 必须 |  | 对象 ID |  |
| contentType | string | 必须 |  | 文案类型 | title / detail / promotion / short_video |
| styleType | string | 非必须 | simple | 文案风格 |  |
| analysisSummary | string | 非必须 |  | 分析摘要 |  |
| extraRequirement | string | 非必须 |  | 额外要求 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| generatedContent | string | 非必须 |  | 生成内容 |  |
| modelName | string | 非必须 |  | 模型名称 |  |
| message | string | 非必须 |  | 提示信息 |  |

## AI 商品对比报告生成

### 基本信息

**Path：** /internal/ai/product-compare

**Method：** POST

**接口描述：** Java 后端调用 Python 服务，根据两个商品的评论分析结果生成商品 A/B 对比报告。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| leftProductId | string | 必须 |  | 左侧商品 ID |  |
| rightProductId | string | 必须 |  | 右侧商品 ID |  |
| leftAnalysis | object | 必须 |  | 左侧商品分析结果 |  |
| rightAnalysis | object | 必须 |  | 右侧商品分析结果 |  |
| language | string | 非必须 | zh-CN | 报告语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| data | object | 非必须 |  | 对比报告结果 |  |
| ├─ compareSummary | string | 非必须 |  | 对比摘要 |  |
| ├─ advantageAnalysis | string | 非必须 |  | 优势分析 |  |
| ├─ riskAnalysis | string | 非必须 |  | 风险分析 |  |
| ├─ operationSuggestions | string | 非必须 |  | 运营建议 |  |
| ├─ modelName | string | 非必须 |  | 模型名称 |  |
| message | string | 非必须 |  | 提示信息 |  |

## 差评回复生成

### 基本信息

**Path：** /internal/ai/negative-reply

**Method：** POST

**接口描述：** Java 后端调用 Python 服务，根据负面评论生成客服回复。

### 请求参数

**Body**

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| commentId | integer | 必须 |  | 评论 ID | format: int64 |
| commentContent | string | 必须 |  | 评论内容 |  |
| problemType | string | 非必须 |  | 差评原因类型 |  |
| toneType | string | 非必须 | sincere | 回复语气 | polite / sincere / professional |
| language | string | 非必须 | zh-CN | 生成语言 |  |

### 返回数据

| 名称 | 类型 | 是否必须 | 默认值 | 备注 | 其他信息 |
|---|---|---|---|---|---|
| success | boolean | 必须 |  | 是否成功 |  |
| replyContent | string | 非必须 |  | 回复内容 |  |
| modelName | string | 非必须 |  | 模型名称 |  |
| message | string | 非必须 |  | 提示信息 |  |
