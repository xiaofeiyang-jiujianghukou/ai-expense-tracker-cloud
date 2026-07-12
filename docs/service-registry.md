# AI Expense Tracker Cloud — 服务注册表

> 最后更新: 2026-07-12

## 域名 & 服务

| 域名 | 服务 | 端口 | 账号 |
|------|------|------|------|
| `www.expense.com` | 前端 (Vue 3) + API Gateway | 80 → 5173 / 8080 | JWT 登录 |
| `nacos.expense.com` | Nacos 注册/配置中心 | 8848 | `nacos` |
| `sentinel.expense.com` | Sentinel Dashboard 流量控制 | 8858 | `nacos` |
| `skywalking.expense.com` | SkyWalking 链路追踪 | OAP:11800/12800 | `admin` |
| `grafana.expense.com` | Grafana 监控面板 | 3000 | `admin` |
| `prometheus.expense.com` | Prometheus 指标采集 | 9090 | —（无认证） |
| `arthas.expense.com` | Arthas 实时诊断 | — | 待部署 |

## 微服务（内部，不对外暴露域名）

| 服务 | 端口 | 说明 |
|------|------|------|
| user-service | 8081 | 用户、认证、JWT |
| category-service | 8082 | 分类管理 |
| bill-service | 8083 | 账单管理 |
| statistics-service | 8084 | 月度统计 |
| ai-service | 8085 | AI 智能分类/报告 |
| expense-gateway | 8080 | API 网关（统一入口） |

## 基础设施

| 容器 | 端口 | 账号 |
|------|------|------|
| MySQL 8.0 | 3306 | `root` |
| Redis 7 | 6379 | `default`（无用户名） |
| Nacos 2.3.2 | 8848 | `nacos` |
| Sentinel Dashboard 1.8.8 | 8858 | `nacos` |
| SkyWalking OAP 10.1 | 11800 / 12800 | `admin` |
| SkyWalking UI 10.1 | 8080 (内部) | `admin` |
| Prometheus 3.3 | 9090 | — |
| Grafana 11.6 | 3000 | `admin` |
| Nginx (反向代理) | 80 | — |

## Nacos 配置

| 项目 | 值 |
|------|-----|
| 命名空间 | `expense-dev` |
| 分组 | `expense-cloud` |
| 认证 | 已启用 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_HOST` | MySQL 主机 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `EXPENSE_DB_USERNAME` | MySQL 用户名 | `root` |
| `EXPENSE_DB_PASSWORD` | MySQL 密码 | — |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | — |
| `NACOS_SERVER` | Nacos 地址 | `127.0.0.1:8848` |
| `NACOS_NAMESPACE` | Nacos 命名空间 | — |
| `JWT_SECRET` | JWT 签名密钥 | — |
| `LLM_API_KEY` | DeepSeek API Key | — |
| `LLM_BASE_URL` | LLM API 地址 | `https://api.deepseek.com` |
| `LLM_MODEL` | LLM 模型 | `deepseek-v4-pro` |

## 快速启动

```bash
# 安装所有镜像 + 启动
docker compose up -d

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f <service>

# 停止
docker compose down
```
