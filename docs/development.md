# 开发环境搭建

## 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21 |
| Python | 3.12+ |
| Node.js | 20+ |
| Maven | 3.9+ |
| Docker | 20+ |

## 启动步骤

### 1. 启动依赖服务

```bash
docker compose up -d
```

### 2. 配置环境变量

```bash
cp ops_agent/.env.example ops_agent/.env
cp aiops-agent/.env.example aiops-agent/.env
```

修改 `.env` 中的数据库密码、令牌等。

### 3. 启动 Java 后端

```bash
cd ops_agent
./mvnw spring-boot:run
```

端口：`8081`

### 4. 启动 Python AI 大脑

```bash
cd aiops-agent
pip install -r requirements.txt
uvicorn main:app --reload --port 5000
```

端口：`5000`

### 5. 启动前端

```bash
cd ops-agent-front
npm install
npm run dev
```

访问：`http://localhost:3000`

## 目录结构

```text
AIops/
├── aiops-agent/       # Python AI 大脑
├── ops_agent/         # Java 后端
├── ops-agent-front/   # Vue 3 前端
├── docs/              # 项目文档
├── .github/workflows/ # CI 配置
├── docker-compose.yml
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```