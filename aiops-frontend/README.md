# 面向中小电商商家的 AI 智能运营助手前端

本目录是项目的 Vue3 管理端前端，直接通过 `/api` 调用 Java 后端接口，不包含模拟后端。

## 技术栈

- Vue3 + TypeScript + Vite
- Vue Router + Pinia
- Element Plus
- vue-i18n
- ECharts
- Axios
- lucide-vue-next

## 语言切换

前端支持简体中文、英文和巴西葡语。当前语言保存在浏览器 `localStorage` 的 `aiops_locale` 中。

- 界面文案、Element Plus 组件内置文案会跟随当前语言切换。
- AI 运营报告、AI 文案、差评回复和商品对比请求会把当前语言作为 `language` 参数传给后端。
- 原始评论内容保持数据源原文展示，一期不自动翻译评论原文。

## 本地联调启动顺序

1. 启动 MySQL 和 Redis。

2. 启动 Python AI/NLP 服务。

```powershell
cd C:\Users\o1893\Documents\Codex\2026-08-10\4-ai-python-ai-web-api\aiops-python-service
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

3. 启动 Java 后端。

```powershell
cd C:\Users\o1893\Documents\Codex\2026-08-10\4-ai-python-ai-web-api\aiops-backend
..\work\tools\apache-maven-3.9.16\bin\mvn.cmd -pl aiops-server -am spring-boot:run
```

默认后端地址是 `http://localhost:8080`，Java 后端会再调用 Python 服务 `http://localhost:8000`。

4. 启动前端。

```powershell
cd C:\Users\o1893\Documents\Codex\2026-08-10\4-ai-python-ai-web-api\aiops-frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 环境变量

复制 `.env.example` 为 `.env.local`，按需修改：

```env
VITE_API_BASE_URL=/api
VITE_BACKEND_URL=http://localhost:8080
```

开发环境下，浏览器请求 `/api/**`，Vite 会代理到 `VITE_BACKEND_URL`。如果 Java 后端端口改成 `8081`，只需要把 `.env.local` 中的 `VITE_BACKEND_URL` 改成 `http://localhost:8081`。

## 登录与鉴权

- 默认账号：`admin`
- 默认密码：请以本地初始化脚本或部署环境配置为准，上线后立即修改

登录成功后，前端会把 JWT 保存到浏览器 `localStorage` 的 `aiops_token`，后续请求自动携带：

```http
Authorization: Bearer <token>
```

## 已接入页面

- 商家驾驶舱：概览指标、评论趋势、情感分布、差评关键词、近期负面评论
- 评论分析：评论筛选、标签编辑、创建分析任务、任务轮询、AI 报告、差评回复
- 数据导入：CSV 上传到 OSS 后导入、公开样例爬虫任务、任务轮询
- 商品对比：商品 A/B 选择、AI 对比报告、历史记录
- AI 文案：商品标题、详情介绍、短视频、促销话术生成
- 告警中心：负面占比、负面评论数量、重点问题类型告警
- 系统设置：联调参数、告警阈值、AI 调用与缓存偏好

## 联调检查点

- 前端能打开 `http://localhost:5173`
- 登录接口 `POST /api/auth/login` 返回 `code=200`
- 浏览器 Network 中接口地址显示为 `/api/...`
- Java 控制台能看到对应请求日志
- 创建分析任务后，前端每 3 秒轮询 `/api/analysis/tasks/{taskId}`
- CSV 导入后，前端每 3 秒轮询 `/api/data/import/tasks/{taskId}`

如果页面提示接口失败，优先检查 Java 后端是否启动、JWT 是否过期、`.env.local` 的 `VITE_BACKEND_URL` 是否正确。
