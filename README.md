# 智能运维（AIOps）平台

本项目实现了一个“**智能运维（AIOps）平台**”，集合了**AI 智能体大脑**、**Java SpringBoot 后端**以及**Vue 前端**，形成了从告警接收、日志分析、自动运维到业务可视化的全链路解决方案。

---

## 1. 项目概述

- **AI 大脑**：使用 LangChain + OpenAI/阿里DashScope 接口，对运维告警进行自动分析、根因定位和修复建议。
- **Java 后端**：基于 Spring Boot 3.4、Kubernetes、Redis、Kafka 实现告警接收、工具调用、状态管理以及 WebSocket 实时推送。
- **前端 UI**：Vue 3 + Element‑Plus 构建，提供实时仪表盘、告警列表、任务管理、工具调用等交互页面。
- **自动巡检**：Python agent 在后台定时执行主动健康检查并把结果回传给 Java 后端，可通过 UI 触发手动巡检。

> 通过将 AI 作为核心决策引擎，系统可以在告警触发后**自动**完成多轮推理，从查询 Kubernetes 状态 → 日志分析 → 执行重启/扩容/回滚 → 结果验证，最终给出一份完整可执行的运维报告。

---

## 2. 架构图

```mermaid
flowchart LR
    subgraph Frontend
        A[Vue 前端] -- 通过 REST / WebSocket 与]
    end
    subgraph Python Agent
        B[aiops‑agent] -- 调用 Java Back‑End](Java Backend)
    end
    subgraph Java Back‑End
        C[ops_agent]
    end
    A -- GET/POST API --> C
    B -- HTTP 调用 API --> C
    C -- WebSocket 推送 --> A
```

---

## 3. 技术栈

| 子系统 | 语言 / 框架 | 关键依赖 | 说明 |
|--------|-------------|----------|------|
| **AI 大脑** | Python | `fastapi`, `uvicorn`, `langchain`, `dashscope` | 负责 ReAct 推理与向量检索 |
| **后端服务** | Java (Spring Boot 3.4) | `spring-boot-starter-webflux`, `kubernetes-client`, `jsch`, `resilience4j` | 处理告警、工具调用、WebSocket 等 |
| **前端页面** | Vue 3 (Vite) | `vue-router`, `pinia`, `element-plus` | 提供仪表盘、实时监控、工具页 |
| **数据库** | MySQL | `spring-boot-starter-data-jpa` | 存储告警、用户等核心数据 |
| **缓存 / 消息** | Redis, Kafka | `spring-boot-starter-data-redis-reactive`, `spring-kafka` | 缓存、异步任务、事件总线 |
| **向量检索** | Chroma | `chromadb` | 用于历史故障手册检索 |

---

## 4. 运行环境

> 代码仓库已在 **Windows** 下进行开发，Docker 与 kind 集群已保存在 D:\ 桌面。若要在 Linux / macOS 上运行，请自行调整 `application.yml` 与 `env` 文件中的路径。

| 环境 | 配置 |
|------|------|
| JDK | 21 |
| Python | 3.12+ |
| Node.js | 20+ |
| Maven | 3.9+ |
| Docker | 20+ |
| kind | 0.20+ |

---

## 5. 快速启动（本地）

```bash
# 1️⃣ 启动 MySQL（如果未跑）
docker run -d --name=mysql --rm -p3306:3306 -e MYSQL_ROOT_PASSWORD=your_password mysql:8

# 2️⃣ 秘钥 & 环境变量
cp ops_agent/.env.example ops_agent/.env  # 修改 MYSQL_*、OPSAGENT_INTERNAL_TOKEN 等
cp aiops-agent/.env.example aiops-agent/.env

# 3️⃣ 启动 Java 后端
cd ops_agent
./mvnw spring-boot:run  # 端口 8081

# 4️⃣ 启动 Python AI 大脑
cd aiops-agent
uvicorn main:app --reload --port 5000

# 5️⃣ 启动前端
cd ops-agent-front
npm install
npm run dev
```

> 以上步骤会在本地 `localhost:3000` 启动前端页面，前端默认调用 `http://localhost:8081` 与 `http://localhost:5000`。若在 Docker/Kind 中，请将对应端口映射或在 `application.yml` 中配置 `opsagent.internal.token` 与 `opsagent.agent.url`。<br>
> 对于 CI 或生产环境，可直接使用 `docker compose up -d` 启动依赖服务（MySQL、Redis）。

---

## 6. API 参考（Java 后端）

> 所有 REST 接口均位于 `/api` 前缀。示例请看 `ops_agent/src/main/java/com/opsagent/controller`。关键接口如下：

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/alert` | POST | 接收外部告警（如 K8s Event） |
| `/api/task` | GET / POST | 查看/创建人工巡检任务 |
| `/api/tool` | GET / POST | 列表/执行 Tool |
| `/api/agent/callback/*` | 各种 | AI 大脑回调接口 |
| `/ws/agent` | WebSocket | 实时推送步骤/结果 |

> AI 大脑与后端的通信使用 `X-Internal-Token` 进行身份校验，令牌请在 `ops_agent/application.yml` 与 `aiops-agent/.env` 中保持一致。

---

## 7. 业务说明

1. **告警接收**：Java 后端通过 `/api/alert` 接收告警，验证签名后转发给 Python AI 大脑。
2. **ReAct 推理**：Python 大脑在 ReAct 循环中可调用以下工具：
   - `search_knowledge_base`
   - `get_service_status`
   - `analyze_logs`
   - `execute_repair_action`（restart/clear_cache）
   - `scale_service`
   - `rollback_version`
   - `request_human_approval`
3. **自动巡检**：Python 进程每晚 08:00（或手动触发）执行全服务健康检查，结果通过 `/api/agent/callback/complete` 回传给 Java 后端，Java 再投递至对应 WebSocket。
4. **前端展示**：实时监控页面使用 WebSocket 订阅 `taskId`，展示各步骤、日志与执行结果；工具页面支持手动调用，带审批前置流程。

---

## 8. 贡献指南

1. Fork 本仓库。
2. 在 `feature/xxx` 分支上完成任务。
3. 单元测试通过后提交 PR。
4. PR 标题请按 `feat: …`、`fix: …` 规范。
5. 用 `miopenAI` 或 `cladden` 运行 lint 检查。

---

## 9. 许可证

Apache‑2.0
