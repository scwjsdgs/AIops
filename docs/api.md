# API 参考

所有 REST 接口均位于 `/api` 前缀。

## 告警

### POST /api/alert

接收外部告警。

**请求体：**

```json
{
  "service": "order-service",
  "level": "critical",
  "message": "Pod CrashLoopBackOff",
  "namespace": "default"
}
```

**响应：**

```json
{
  "taskId": "task-001",
  "status": "accepted"
}
```

## 任务

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/task` | GET | 查看任务列表 |
| `/api/task` | POST | 创建人工巡检任务 |

## 工具

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/tool` | GET | 工具列表 |
| `/api/tool` | POST | 执行工具 |

## AI 回调

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/agent/callback/complete` | POST | AI 大脑回传执行结果 |

## WebSocket

| 路径 | 说明 |
|------|------|
| `/ws/agent` | 实时推送步骤与结果 |

## 鉴权

AI 大脑与 Java 后端之间使用 `X-Internal-Token` 请求头做身份校验。
令牌需在 `ops_agent/application.yml` 与 `aiops-agent/.env` 中保持一致。