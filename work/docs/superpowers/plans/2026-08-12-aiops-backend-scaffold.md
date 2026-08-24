# AI 智能运营助手后端骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建一个 IDEA 可导入的 Spring Boot 多模块后端工程，实现接口文档中的主要 REST API 骨架和模拟返回。

**Architecture:** 使用 Maven 多模块结构：`aiops-common` 放公共能力，`aiops-pojo` 放实体/DTO/VO，`aiops-server` 放启动类、Controller、Service、Client 和配置。第一版不连接真实 MySQL、Redis、阿里云 OSS 或 Python 服务，只保留配置项、接口边界和模拟业务实现，确保后续可以平滑替换为真实实现。

**Tech Stack:** Java 21、Spring Boot 3.3.x、Maven、MyBatis-Plus、Spring Data Redis、Knife4j、Aliyun OSS SDK、Lombok、JUnit 5。

## Global Constraints

- 项目结构采用 `aiops-common / aiops-pojo / aiops-server` 三模块。
- 包名统一使用 `com.aiops`。
- 对外接口按照 `outputs/AI智能运营助手接口文档.md` 实现。
- CSV 文件先上传到阿里云 OSS，MySQL 只保存结构化数据和文件元信息。
- 第一版 Controller 返回模拟数据，后续再接 MySQL、Redis、OSS 和 Python。
- 当前命令行没有 Maven，项目以 IDEA 导入运行为主要验证方式。

---

### Task 1: Maven 多模块骨架

**Files:**
- Create: `aiops-backend/pom.xml`
- Create: `aiops-backend/aiops-common/pom.xml`
- Create: `aiops-backend/aiops-pojo/pom.xml`
- Create: `aiops-backend/aiops-server/pom.xml`

**Interfaces:**
- Produces: IDEA 可导入的 Maven 多模块项目。

- [ ] 创建父工程和三个子模块。
- [ ] 配置 Java 21、Spring Boot、Lombok、MyBatis-Plus、Redis、Knife4j、Aliyun OSS 依赖。

### Task 2: 公共模块

**Files:**
- Create: `aiops-common/src/main/java/com/aiops/result/Result.java`
- Create: `aiops-common/src/main/java/com/aiops/result/PageResult.java`
- Create: `aiops-common/src/main/java/com/aiops/constant/MessageConstant.java`
- Create: `aiops-common/src/main/java/com/aiops/constant/RedisKeyConstant.java`
- Create: `aiops-common/src/main/java/com/aiops/enumeration/TaskStatusEnum.java`
- Create: `aiops-common/src/main/java/com/aiops/exception/BusinessException.java`

**Interfaces:**
- Produces: Controller 和 Service 统一使用的返回结构、常量和异常。

- [ ] 添加统一返回对象。
- [ ] 添加分页返回对象。
- [ ] 添加业务常量、Redis Key 常量、任务状态枚举和业务异常。

### Task 3: POJO 模块

**Files:**
- Create: `aiops-pojo/src/main/java/com/aiops/dto/*.java`
- Create: `aiops-pojo/src/main/java/com/aiops/vo/*.java`
- Create: `aiops-pojo/src/main/java/com/aiops/entity/*.java`

**Interfaces:**
- Produces: Controller 请求 DTO、响应 VO、数据库实体类。

- [ ] 添加登录、文件上传、CSV 导入、爬虫导入、分析任务、AI 报告、AI 文案、差评回复 DTO。
- [ ] 添加登录、文件上传、任务状态、分析结果、报告、文案、看板 VO。
- [ ] 添加核心实体类。

### Task 4: Server 接口层与模拟业务

**Files:**
- Create: `aiops-server/src/main/java/com/aiops/AiOpsApplication.java`
- Create: `aiops-server/src/main/java/com/aiops/controller/*.java`
- Create: `aiops-server/src/main/java/com/aiops/service/*.java`
- Create: `aiops-server/src/main/java/com/aiops/service/impl/*.java`

**Interfaces:**
- Produces: 接口文档中的主要 REST API 可被访问。

- [ ] 添加启动类。
- [ ] 添加用户认证、文件上传、数据导入、商品、卖家、评论、分析、AI 报告、AI 文案、差评回复、看板 Controller。
- [ ] 添加对应 Service 和模拟实现。

### Task 5: 配置和初始化 SQL

**Files:**
- Create: `aiops-server/src/main/resources/application.yml`
- Create: `aiops-server/src/main/resources/application-dev.yml`
- Create: `aiops-server/src/main/resources/sql/schema.sql`

**Interfaces:**
- Produces: 后续连接 MySQL、Redis、OSS、Python 的配置入口和建表草案。

- [ ] 添加服务端口、数据库、Redis、OSS、Python 服务配置。
- [ ] 添加核心建表 SQL 草案。

### Task 6: 基础测试

**Files:**
- Create: `aiops-server/src/test/java/com/aiops/common/ResultTest.java`

**Interfaces:**
- Produces: 最小可运行测试，验证公共返回结构。

- [ ] 写 Result 成功返回测试。
- [ ] 写 Result 失败返回测试。
- [ ] 在 IDEA 中运行 Maven test。
