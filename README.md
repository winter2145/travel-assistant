# 行旅 · AI 旅游规划工作台
行旅是一个基于 Vue 3 + Spring AI Alibaba 的 AI 旅游规划项目，为用户提供旅行对话、行程规划与编辑、旅行记录管理，以及旅游攻略预览和 PDF 导出等功能。

# 前端 (ai-agent-front)
## 功能

- **旅行工作台**：通过对话描述旅行需求，并以流式方式接收 AI 回复。
- **行程管理**：查看、编辑、复制和删除行程，浏览每日活动安排与预算。
- **攻略预览与导出**：预览行程攻略并请求后端生成 PDF。
- **旅行记录**：查看已保存的行程。
- **后端健康检查**：界面可检查后端服务是否可用。

## 技术栈

- Vue 3、TypeScript、Vite
- Vue Router、Pinia
- Element Plus、`@element-plus/icons-vue`
- Axios、Markdown-it

## 环境要求

- Node.js 18 或更高版本
- npm
- 后端服务运行在 `http://localhost:8123`，并提供前端使用的 `/api` 接口

## 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

开发服务器默认监听 `5173` 端口。启动后，使用终端显示的地址访问工作台（通常为 `http://localhost:5173`）。

开发模式下，Vite 会将 `/api` 请求代理到 `http://localhost:8123`，路径不会被重写。因此开发时需先启动后端；如果后端地址或端口不同，请修改 [vite.config.ts](./vite.config.ts) 中的代理目标。

## 常用命令

| 命令 | 说明 |
| --- | --- |
| `npm run build` | 执行 Vue/TypeScript 类型检查并构建生产文件 |
| `npm run dev` | 启动本地开发服务器 |
| `npm run preview` | 本地预览 `dist` 中的生产构建结果 |

## 页面路由

| 路径 | 页面 |
| --- | --- |
| `/`、`/travel` | 旅行工作台 |
| `/trips`、`/travel/history` | 我的旅行 |
| `/plans/:id`、`/travel/:id/itinerary` | 行程详情 |
| `/travel/:id/export`、`/guide/:id` | 攻略预览 |

## 项目结构

```text
src/
├── api/          # HTTP、对话、行程、路线、PDF 和健康检查接口
├── assets/       # 全局样式等静态资源
├── components/   # 对话、行程、侧边栏和通用组件
├── layouts/      # 页面布局
├── mock/         # 示例数据
├── router/       # Vue Router 路由配置
├── stores/       # Pinia 状态管理
├── types/        # TypeScript 类型定义
├── utils/        # SSE、Markdown 和行程处理工具
└── views/        # 工作台、旅行列表、行程详情和攻略预览页面
```

## 后端连接说明

前端 API 使用 `/api` 作为基础路径。开发环境由 Vite 代理到 `http://localhost:8123`；例如后端健康检查路径为 `/api/health`。AI 对话通过 SSE 接收流式响应，相关请求及行程、会话和 PDF 接口定义在 `src/api/`。

`npm run build` 会生成 `dist/` 目录。部署生产构建时，应由 Web 服务器托管该目录，并配置 `/api` 到后端的反向代理；若使用 Vue Router 的 history 模式，还需将前端页面路由回退到 `index.html`。

# 后端 (ai-agent-backend)

基于 **Spring AI Alibaba** 的 AI 智能体（Agent）后端服务。项目以「旅行规划智能体」为业务载体，实现自主规划式超级智能体（Manus 模式），支持工具调用（Function Calling）、MCP 协议接入外部服务（高德地图 / Pexels 图片搜索）、RAG 知识库增强检索、多轮会话管理与旅行计划持久化。

## 功能特性

- **超级智能体（Manus）**：基于 Spring AI Alibaba Agent Framework 实现的自主规划 Agent，具备「思考 → 调用工具 → 观察结果 → 再行动」的循环执行能力（ToolCallAgent / ReActAgent 模式），可自动串联景点搜索、行程生成、预算校验等工具完成复杂任务。
- **工具调用（Function Calling）**：内置 5 个本地旅行工具（预算计算、预算校验、冲突检测、行程生成、行程调整），由 LLM 按需自动调用。
- **MCP 服务接入**：通过 stdio/SSE 两种方式接入两个 MCP 子服务——
  - `ai-agent-amap-mcp-server`：高德地图能力（景点/美食搜索、天气查询、路线规划），主应用启动时以 stdio 方式自动拉起；
  - `ai-agent-pexelssearch-mcp-server`：Pexels 高清图片搜索，支持 stdio（默认）/ SSE 两种模式。
- **RAG 知识库增强检索**：启动时将 `resources/document` 下的 Markdown 业务规范文档切分为 Chunk，经 DashScope Embedding（`text-embedding-v3`，1536 维）写入 **pgvector**，对话时通过 `RetrievalAugmentationAdvisor` 检索增强回答（幂等初始化，已有数据自动跳过）。
- **会话管理**：会话（`travel_conversation`）与消息（`travel_message`）持久化到 PostgreSQL，携带会话 ID 对话时自动恢复最近 20 条历史消息，支持流式（SSE）与非流式两种对话方式，并防止同一会话并发生成。
- **旅行计划管理**：旅行计划（`travel_plan`）CRUD，行程以 JSONB 存储，支持乐观锁版本控制、按会话查询、复制副本，并支持导出 **PDF 旅行攻略**（PDFBox 渲染）。
- **路线查询接口**：地点名称经高德地理编码解析为经纬度后查询路线（驾车/步行/公交等模式）。
- **接口文档**：内置 Knife4j（springdoc-openapi）接口文档，中文界面，开箱即用。
- **流式输出**：对话接口基于 SSE（`text/event-stream`）流式返回 Agent 的思考与执行过程。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.16 |
| AI 框架 | Spring AI 1.1.2（BOM）、Spring AI Alibaba Agent Framework 1.1.2.0、DashScope Starter 1.1.2.1 |
| 大模型 | 阿里云百炼 DashScope（对话模型 `qwen3.8-max`、Embedding 模型 `text-embedding-v3`） |
| 向量存储 | PostgreSQL + pgvector（HNSW 索引、余弦距离） |
| MCP | spring-ai-starter-mcp-client（stdio / SSE） |
| 数据库 | PostgreSQL + JDBC（spring-boot-starter-jdbc） |
| PDF 导出 | Apache PDFBox 3.0.8 |
| 接口文档 | Knife4j 4.6.0（springdoc-openapi） |
| 工具库 | Hutool 5.8.38、DashScope SDK 2.22.23、victools jsonschema-generator 4.38.0 |
| 构建 | Maven（含 Maven Wrapper `mvnw`） |

## 项目结构

```
ai-agent-backend
├── ai-agent-amap-mcp-server/          # MCP 子模块：高德地图能力服务（stdio，由主应用自动拉起）
│   └── src/main/java/.../tools/       # AttractionSearch / FoodCuisineSearch / WeatherSearch / MapRoute
├── ai-agent-pexelssearch-mcp-server/  # MCP 子模块：Pexels 图片搜索服务（stdio 默认 / SSE 可选）
├── src/main/java/com/xin/aiagentbackend/
│   ├── agent/                         # Agent 框架：BaseAgent → ToolCallAgent → ReActAgent → Manus
│   ├── app/                           # LoveApp 应用服务 + AIController 对话接口（SSE / 非流式）
│   ├── conversation/                  # 会话与消息管理（REST + Repository）
│   ├── plan/                          # 旅行计划管理 + PDF 攻略导出
│   ├── route/                         # 路线查询（高德地理编码 + 路径规划）
│   ├── tools/                         # 本地旅行工具（预算 / 冲突 / 行程生成与调整）
│   ├── rag/                           # Markdown 知识库加载、Chunk 切分、pgvector 初始化
│   ├── config/                        # DashScope / 工具装配配置
│   └── api/                           # 统一响应、异常处理、健康检查
└── src/main/resources/
    ├── application.yml                # 主配置（激活 dev profile）
    ├── application-dev.yml            # 开发环境配置（端口 8123、数据源、模型、MCP）
    ├── mcp-servers.json               # MCP stdio 服务端声明
    ├── db/migration/001_travel_business.sql   # 业务表结构（幂等，启动自动执行）
    └── document/                      # 旅行规划智能体知识库（Markdown 规范文档，RAG 数据源）
```

## 环境要求

| 依赖 | 版本/说明 |
| --- | --- |
| JDK | **21**（`pom.xml` 中 `java.version=21`） |
| Maven | 3.9+（或直接使用项目自带的 `mvnw` / `mvnw.cmd`） |
| PostgreSQL | 15+，需已安装 **pgvector** 扩展 |
| 阿里云百炼 API Key | 必填，用于调用 DashScope 对话/Embedding 模型，[开通地址](https://bailian.console.aliyun.com/) |
| 高德开放平台 Web 服务 Key | 必填（使用 MCP 地图能力时），[申请地址](https://console.amap.com/dev/key/app) |
| Pexels API Key | 图片搜索功能需要，[申请地址](https://www.pexels.com/api/) (目前申请入口关闭)|

## 本地启动

### 1. 准备数据库

可使用docker desktop在其终端运行如下命令，创建数据库：

docker 拉取pgvector
```
docker pull pgvector/pgvector:pg16
```
启动并创建用户密码、DB
```
docker run -d `
 --name pgvector-agent `
 -e POSTGRES_USER=自定义USERNAME `
 -e POSTGRES_PASSWORD=自定义PASSWORD `
 -e POSTGRES_DB=agent_db `
 -e POSTGRES_DB=agent_db `
 -p 5432:5432 `
 -v pgvector_data:/var/lib/postgresql/data `
 pgvector/pgvector:pg16
```
应用启动时会自动执行 `db/migration/001_travel_business.sql`（幂等）初始化业务表，并自动创建 pgvector 向量表 `vector_store`（含扩展、HNSW 索引）。

> 默认连接配置见 `application-dev.yml`：`jdbc:postgresql://localhost:5432/agent_db`，用户 `自定义USERNAME` / 密码 `自定义PASSWORD`。请按本机实际修改。

### 2. 配置密钥

默认配置在 `src/main/resources/application-dev.yml`，所有密钥均支持环境变量覆盖（优先级高于文件内默认值），建议通过环境变量注入，避免密钥入库：

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-xxx"          # 阿里云百炼
$env:AMAP_WEB_KEY="xxx"                  # 高德 Web 服务 Key
$env:PEXELS_API_KEY="xxx"                # Pexels（可选）

# Linux / macOS
export DASHSCOPE_API_KEY="sk-xxx"
export AMAP_WEB_KEY="xxx"
export PEXELS_API_KEY="xxx"
```

### 3. 构建 MCP 子模块

主应用通过 stdio 自动拉起两个 MCP 服务，需**先构建**出对应 jar：

```bash
# 高德 MCP 服务
cd ai-agent-amap-mcp-server
mvn clean package -DskipTests

# Pexels MCP 服务
cd ../ai-agent-pexelssearch-mcp-server
mvn clean package -DskipTests
```

> 未使用 Maven 也可执行 `./mvnw.cmd clean package -DskipTests`（Windows）或 `./mvnw clean package -DskipTests`（Linux/macOS）。

### 4. 构建并启动主应用

```bash
cd ..

# 构建
mvn clean package -DskipTests

# 方式一：直接运行 jar
java -jar target/ai-agent-backend-0.0.1-SNAPSHOT.jar

# 方式二：开发模式运行
mvn spring-boot:run
```

启动成功后，主应用会自动拉起 `ai-agent-amap-mcp-server`（stdio），并在启动时完成知识库向量化写入（首次启动需要等待 Embedding 分批写入完成）。

### 5. 验证

| 检查项 | 地址 |
| --- | --- |
| 健康检查 | `http://localhost:8123/api/health` → 返回 `ok` |
| Knife4j 接口文档 | `http://localhost:8123/api/doc.html` |
| Swagger UI | `http://localhost:8123/api/swagger-ui.html` |

## 配置说明

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| `server.port` | 主应用端口 | `8123` |
| `server.servlet.context-path` | 全局接口前缀 | `/api` |
| `spring.ai.dashscope.api-key` | 阿里云百炼 API Key（环境变量 `DASHSCOPE_API_KEY`） | 无 |
| `spring.ai.dashscope.chat.options.model` | 对话模型 | `qwen3.8-max` |
| `spring.ai.vectorstore.pgvector.*` | 向量库配置（维度 1536、HNSW、余弦距离） | - |
| `spring.datasource.url` | PostgreSQL 连接地址 | `jdbc:postgresql://localhost:5432/agent_db` |
| `amap.web.key` | 高德 Web 服务 Key（环境变量 `AMAP_WEB_KEY`） | 无 |
| `pexels.api-key` | Pexels API Key（环境变量 `PEXELS_API_KEY`） | 无 |
| `spring.ai.mcp.client.*` | MCP 客户端（stdio 配置取自 `mcp-servers.json`，请求超时 60s） | - |

## API 概览

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 对话 | POST | `/api/ai/manus/chat` | Manus 流式会话对话（SSE，推荐） |
| 对话 | GET | `/api/ai/manus/chat?message=&conversationId=` | 流式对话（兼容旧客户端） |
| 对话 | GET | `/api/ai/manus/chatByRun?message=` | 非流式对话 |
| 会话 | POST/GET | `/api/conversations` | 创建 / 分页查询会话 |
| 会话 | GET/PATCH/DELETE | `/api/conversations/{id}` | 会话详情 / 更新 / 删除 |
| 会话 | GET | `/api/conversations/{id}/messages` | 分页查询会话消息 |
| 计划 | POST/GET | `/api/plans` | 创建 / 分页查询旅行计划（支持关键词） |
| 计划 | GET/PUT/DELETE | `/api/plans/{id}` | 计划详情 / 更新（乐观锁）/ 删除 |
| 计划 | GET | `/api/plans/by-conversation/{conversationId}` | 按会话查询最近计划 |
| 计划 | POST | `/api/plans/{id}/duplicate` | 复制计划 |
| 计划 | POST | `/api/plans/{id}/pdf-exports` | 导出 PDF 旅行攻略 |
| 路线 | POST | `/api/routes/query` | 路线查询（地点名称自动地理编码） |
| 系统 | GET | `/api/health` | 健康检查 |



