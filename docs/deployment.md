# 部署说明

## 本地开发部署

参考 [development.md](./development.md)。

## Docker Compose 部署

项目根目录提供 `docker-compose.yml`，可一键启动依赖服务：

```bash
docker compose up -d
```

目前该文件用于启动 MySQL 和 Redis。后续会逐步补充 Java 后端、Python AI 大脑和前端的服务定义。

## Kubernetes 部署

项目使用 kind 做本地 K8s 集群验证。

```bash
# 创建集群
kind create cluster --name aiops

# 应用配置
kubectl apply -f deploy/k8s/
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `MYSQL_HOST` | MySQL 主机 |
| `MYSQL_PORT` | MySQL 端口 |
| `MYSQL_DATABASE` | 数据库名 |
| `MYSQL_USER` | 数据库用户 |
| `MYSQL_PASSWORD` | 数据库密码 |
| `OPSAGENT_INTERNAL_TOKEN` | 内部通信令牌 |
| `OPSAGENT_AGENT_URL` | AI 大脑地址 |