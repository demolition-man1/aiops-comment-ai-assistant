# AI 智能运营助手后端

这是一个 Spring Boot 多模块后端项目，结构参考苍穹外卖风格：

- `aiops-common`: 公共返回、常量、异常、配置属性、JWT 工具。
- `aiops-pojo`: Entity、DTO、VO。
- `aiops-server`: Controller、Service、Mapper、Interceptor、Python Client、SQL 初始化脚本。

## 技术栈

- Java 21
- Spring Boot 3.3.5
- MyBatis-Plus 3.5.7
- MySQL 8.x
- Redis
- Aliyun OSS
- Knife4j / OpenAPI

## 本地配置

默认 profile 为 `dev`，配置文件位于 `aiops-server/src/main/resources/application-dev.yml`。

- MySQL: `jdbc:mysql://localhost:3306/aiops`
- 用户名: `root`
- 密码: `432`
- Redis: `localhost:6379`
- Python 服务: `http://localhost:8000`

首次启动时会通过 `sql/schema.sql` 自动建表，并通过 `sql/data.sql` 初始化默认管理员：

- username: `admin`
- password: `123456`

Redis 当前用于：

- 任务状态缓存: `task:status:{taskId}`
- 任务进度缓存: `task:progress:{taskId}`
- 分析结果缓存: `analysis:product:{productId}` / `analysis:seller:{sellerId}`
- AI 报告缓存: `ai:report:{targetType}:{targetId}`
- AI 文案缓存: `ai:content:{hash}`
- 商品对比报告缓存: `ai:compare:product:{leftProductId}:{rightProductId}`
- AI 调用限流: `rate:ai:user:{userId}`, 默认每用户每分钟 20 次

Redis 作为加速层处理，服务不可用时不会阻断 MySQL 主流程。

## 已实现的一期增强

- 评论标签手动编辑: `PUT /api/comments/{commentId}/tags`
- 分析结果增强: 负面关键词、评分分布、自定义标签分布、时间趋势
- 商品 A/B 对比: `POST /api/analysis/products/compare`
- 差评回复跟踪: 使用次数、效果标记、收藏状态

如果数据库已经按旧版 `schema.sql` 初始化过，可执行升级脚本补字段：

```powershell
mysql -uroot -p432 < aiops-server\src\main\resources\sql\upgrade-2026-08-20-operations-enhancement.sql
```

## 构建与运行

项目内已经准备了 Maven：

```powershell
cd aiops-backend
..\work\tools\apache-maven-3.9.16\bin\mvn.cmd test
..\work\tools\apache-maven-3.9.16\bin\mvn.cmd package -DskipTests
java -jar aiops-server\target\aiops-server-0.0.1-SNAPSHOT.jar
```

启动后访问：

- 后端接口: `http://localhost:8080/api`
- Knife4j 文档: `http://localhost:8080/doc.html`
