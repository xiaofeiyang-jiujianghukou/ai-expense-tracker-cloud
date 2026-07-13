# V4.0 设计稿 — AI Expense Tracker Cloud

> 📐 **V4.0 微服务工程化改造设计稿**（补设计先行原则，基于已落地的真实架构编写）
> 本文档记录 V4.0 的架构设计决策、技术选型理由和落地细节。后续如需改动，仅在末尾加外联引用，不修改正文。

---

## 1. V4.0 设计目标

在 V3.0 功能完备的单体应用基础上，将其拆分为微服务架构并实现容器化部署、动态路由、可观测性。

### 一句话目标

> **Docker Compose 全容器编排 + Nacos 动态路由 + 7 微服务独立部署 + Prometheus/Grafana 监控。**

### 核心改造维度

| 维度 | V3.0（单体） | V4.0（微服务） |
|------|-------------|---------------|
| 部署单元 | 1 个 fat JAR | 7 个独立服务 + 5 个基础设施容器 |
| 服务发现 | 无（单进程内调用） | Nacos 注册中心 |
| 路由 | Spring MVC 直接路由 | Spring Cloud Gateway + Nacos 动态配置 |
| 认证 | JwtAuthFilter 单体拦截 | Gateway 统一鉴权 + X-User-Id 上下文传播 |
| 配置管理 | application.yml 本地 | Nacos 配置中心（gateway-routes 动态刷新） |
| 服务间通信 | Java 方法调用 | Feign + Nacos 服务发现 |
| 监控 | 无 | Prometheus + Grafana（9 面板仪表盘） |
| 资源管控 | 无限制 | 全容器 deploy.resources.limits + JVM 堆调优 |
| 部署方式 | `mvn spring-boot:run` | `docker compose up -d` |

---

## 2. 服务拓扑

### 2.1 12 容器架构

```
                        ┌──────────────────────────┐
                        │     expense-nginx (:80)    │  反向代理入口
                        │     www.expense.com        │
                        └─────────────┬────────────┘
                                      │
                        ┌─────────────▼────────────┐
                        │  expense-gateway (:8080)  │  Spring Cloud Gateway
                        │  JWT 鉴权 · CORS · 限流    │  Nacos 动态路由
                        └─────────────┬────────────┘
                                      │
          ┌───────────────┬───────────┼───────────┬───────────────┐
          │               │           │           │               │
    ┌─────▼─────┐  ┌──────▼──────┐ ┌──▼────┐ ┌───▼────┐  ┌──────▼──────┐
    │user-svc   │  │category-svc │ │bill-  │ │budget- │  │statistics-  │
    │(:8081)    │  │(:8082)      │ │svc    │ │svc     │  │svc (:8084)  │
    └─────┬─────┘  └──────┬──────┘ │(:8083)│ │(:8086) │  └──────┬──────┘
          │               │        └──┬────┘ └───┬────┘         │
          │               │           │          │              │
          └───────────────┴───────────┴──────────┴──────────────┘
                                      │
                          ┌───────────▼───────────┐
                          │    expense-ai (:8085)  │  SSE 流式 · Redis 缓存
                          └───────────────────────┘
```

### 2.2 服务清单

| 服务 | 端口 | 来源模块 | 数据库表 | 说明 |
|------|------|---------|---------|------|
| **nginx** | 80 | — | — | 反向代理，域名路由 |
| **gateway** | 8080 | expense-gateway（新建） | — | JWT 鉴权 + 动态路由 + CORS |
| **user-service** | 8081 | expense-user | `user` | 注册/登录 |
| **category-service** | 8082 | expense-category | `category` | 分类 CRUD（叶子服务） |
| **bill-service** | 8083 | expense-bill | `bill` | 账单 CRUD + 筛选分页 |
| **budget-service** | 8086 | expense-budget（独立） | `budget` | 预算管理（独立部署） |
| **statistics-service** | 8084 | expense-statistics | —（聚合查询） | 月度统计/分类统计 |
| **ai-service** | 8085 | expense-ai | —（纯计算） | AI 分类/洞察/报告 |
| **mysql** | 3306 | — | 全部 | Phase 1 共享数据库 |
| **redis** | 6379 | — | — | AI 缓存（analysis 1h / report 6h） |
| **nacos** | 8848/9848 | — | — | 注册中心 + 配置中心（auth enabled） |

### 2.3 额外基础设施

| 容器 | 端口 | 用途 |
|------|------|------|
| **frontend** | 5173 | Vue 3 开发服务器 |
| **prometheus** | 9090 | 指标采集（/actuator/prometheus） |
| **grafana** | 3000 | 监控仪表盘（9 面板） |

> **注**: Prometheus 和 Grafana 在 docker-compose.yml 早期版本中存在，当前精简为 12 容器运行集。完整清单见 [architecture-design.md §10.7](../architecture-design.md)。

---

## 3. 模块拆分（三模块模式）

### 3.1 拆分原则

所有应用服务统一按三模块拆分（Sprint 20 完成）：

```
expense-{domain}-api/           ← 轻量 JAR（Feign 接口 + DTO），对外发布
expense-{domain}-common/        ← 共享 DTO/VO（被其他服务 import）
expense-{domain}-application/   ← Spring Boot 应用（Controller + Service + Mapper），只部署不发布
```

### 3.2 当前拆分清单

| 服务 | API 模块 | Common 模块 | 应用模块 | Feign 消费者 |
|------|---------|------------|---------|-------------|
| category | `expense-category-api` | `expense-category-common` | `expense-category-application` | bill, statistics, user, budget, ai |
| bill | `expense-bill-api` | `expense-bill-common` | `expense-bill-application` | statistics, ai |
| statistics | `expense-statistics-api` | `expense-statistics-common` | `expense-statistics-application` | budget, ai |
| user | `expense-user-api` | `expense-user-common` | `expense-user-application` | 暂无（预留） |
| budget | `expense-budget-api` | `expense-budget-common` | `expense-budget-application` | 暂无（预留） |
| ai | `expense-ai-api` | `expense-ai-common` | `expense-ai-application` | 暂无（预留） |

> ⚡ **Sprint 20 更新**：user/budget/ai 已完成三模块拆分，API 模块预留 Feign 接口供未来消费者使用。Gateway 保持单模块（reactive 栈）。

### 3.3 共享 Starter（Sprint 20 重构）

> ⚡ **2026-07-13 更新**：原 `expense-common` + `expense-security` 已合并重构为 `expense-framework`，再拆分为 3 个独立 starter。

| Starter | 关系 | 内容 |
|---------|------|------|
| **expense-starter-web** | 基础层 | Web/MVC + Security + Feign + JWT + Nacos + Sentinel + Actuator + ApiResponse + 异常体系 + BillType 枚举 |
| **expense-starter-orm** | 依赖 web | MyBatis-Plus + DataSource（env vars）+ Flyway |
| **expense-starter-redis** | 依赖 web | Redis 连接配置（env vars） |

**各服务引用**：
- 所有应用服务（user/category/bill/budget/statistics/ai）→ `expense-starter-web`（通过 orm 或 redis 间接引入）
- 有 DB 的服务（user/category/bill/budget/statistics）→ `expense-starter-orm`
- AI → `expense-starter-orm` + `expense-starter-redis`
- Gateway → **不引用任何 starter**（reactive 栈，独立声明依赖）

### 3.4 模块依赖原则

```
🚫 应用模块间禁止相互引用（如 bill-application 不能依赖 budget-application）
✅ 服务间通信 → Feign（通过 API 模块）+ 网关
✅ 共享基础设施 → 3 个 starter（web / orm / redis），按需引入
✅ 各 starter 的 AutoConfiguration.imports 独立注册，无需 @Import
```

---

## 4. 认证架构重构

### 4.1 V3 → V4 变化

```
V3.0（单体）:
  Request → JwtAuthFilter（在每个请求线程解析 JWT）→ Controller → SecurityUtil.getCurrentUserId()

V4.0（微服务）:
  Request → Gateway [JwtValidationGlobalFilter]          → 下游服务 [XUserFilter]
            │                                                    │
            1. 提取 Authorization header                         1. 读 X-User-Id header
            2. 验证 JWT 签名 + 过期                               2. 设 SecurityContextHolder
            3. 注入 X-User-Id / X-User-Email                     3. SecurityUtil 代码零改动
            4. 移除 Authorization header（不向下透传）
```

### 4.2 设计理由

- **安全**: JWT 只暴露在 Gateway 层，下游服务不接触原始 Token
- **兼容**: `XUserFilter` 写入 `SecurityContextHolder`，现有 `SecurityUtil.getCurrentUserId()` 无需修改
- **Feign 传播**: `UserContextFeignInterceptor` 自动携带 `X-User-Id` header 到下游调用

### 4.3 关键组件

| 组件 | 位置 | 职责 |
|------|------|------|
| `JwtValidationGlobalFilter` | expense-gateway | Gateway GlobalFilter，解析 JWT → 注入 header |
| `XUserFilter` | expense-starter-web | 下游 OncePerRequestFilter，读 header → 设 SecurityContext |
| `UserContextFeignInterceptor` | expense-starter-web | Feign RequestInterceptor，自动传播 X-User-Id |

---

## 5. Gateway 动态路由

### 5.1 路由表

| 路由路径 | 目标服务 | 特殊处理 |
|---------|---------|---------|
| `/api/users/**` | user-service | — |
| `/api/categories/**` | category-service | — |
| `/api/bills/**` | bill-service | — |
| `/api/budgets/**` | budget-service | — |
| `/api/statistics/**` | statistics-service | — |
| `/api/ai/analysis/stream` | ai-service | SSE 非缓冲透传，120s 超时 |
| `/api/ai/report/stream` | ai-service | SSE 非缓冲透传，120s 超时 |
| `/api/ai/**` | ai-service | — |

### 5.2 Nacos 动态路由

路由配置存储在 Nacos `gateway-routes.yaml`（YAML 格式），Gateway 通过 Spring Cloud Config 自动监听刷新：

```
新服务上线流程:
  1. 部署新服务 → Nacos 自动注册
  2. 在 Nacos 控制台更新 gateway-routes.yaml → 添加路由规则
  3. Gateway 自动刷新路由（无需重启）
```

**优势**: 新服务上线不改代码、不停机、不重新构建 Gateway 镜像。

### 5.3 CORS

Gateway 统一配置跨域，下游服务不再各自处理 CORS。

---

## 6. 容器化与资源管控

### 6.1 Dockerfile 策略

所有 Java 服务采用多阶段构建：

```dockerfile
# Stage 1: Maven 构建
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
COPY . /build
WORKDIR /build
RUN mvn clean package -pl <module> -am -DskipTests

# Stage 2: 运行镜像（精简）
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /build/<module>/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "/app/app.jar"]
```

### 6.2 资源限制表

| 容器 | Memory Limit | CPUs | JVM -Xmx | 说明 |
|------|-------------|------|----------|------|
| nginx | 64M | 0.5 | — | 纯代理，极低 |
| frontend | 512M | 0.5 | — | Vite dev server |
| mysql | 512M | 1.0 | — | 数据库 |
| redis | 128M | 0.5 | — | maxmemory 100mb |
| nacos | 512M | 1.0 | 384m | 注册+配置中心 |
| gateway | 256M | 0.5 | — | 无状态网关 |
| user-service | 400M | 0.5 | 384m | JVM 堆利用率 ~65% |
| category-service | 256M | 0.5 | — | CRUD 叶子服务 |
| bill-service | 256M | 0.5 | — | CRUD 服务 |
| budget-service | 256M | 0.5 | — | CRUD 服务 |
| statistics-service | 256M | 0.5 | — | 聚合查询 |
| ai-service | 256M | 0.5 | — | LLM 调用 + Redis 缓存 |

### 6.3 资源配置原则

- **最小必要**: 每个容器按实际负载配置，不拍脑袋
- **JVM < 容器**: `-Xmx` 必须小于容器 memory limit，否则 OOMKilled
- **JVM 堆利用率目标**: 60-75%（给 Metaspace + 线程栈留余量）
- **总内存**: 12 容器约 3-4 GB（未限制时可达 10+ GB）

---

## 7. 可观测性

### 7.1 监控架构

```
各服务 /actuator/prometheus  ──▶  Prometheus :9090  ──▶  Grafana :3000
     │                              │                        │
     ├─ JVM: 堆内存、GC             │                        ├─ 9 面板仪表盘
     ├─ HTTP: QPS、P99、错误率       ├─ 15s 采集间隔            ├─ 健康状态面板
     ├─ DB: HikariCP 连接数          └─ prometheus.yml         ├─ QPS / P99 / 错误率
     └─ CPU: process_cpu_usage                                ├─ CPU / 内存
                                                              ├─ DB 连接 / GC
                                                              └─ 线程数
```

### 7.2 Grafana 仪表盘（expense-cloud-overview.json）

| 面板 | 指标 | 告警阈值 |
|------|------|---------|
| 服务健康 | `up` | = 0 时告警 |
| QPS | `rate(http_server_requests_count[1m])` | — |
| P99 延迟 | `http_server_requests_seconds` | > 1s 关注 |
| 错误率 | `http_server_requests_5xx` | > 5% 告警 |
| CPU 使用率 | `process_cpu_usage` | > 80% 告警 |
| JVM 堆内存 | `jvm_memory_used_bytes{area="heap"}` | > 80% 告警 |
| DB 连接数 | `hikaricp_connections_active` | > 80% pool 关注 |
| GC 暂停 | `jvm_gc_pause_seconds` | > 100ms 关注 |
| 线程数 | `jvm_threads_live_threads` | — |

---

## 8. 技术决策

| ID | 决策 | 选择 | 理由 |
|----|------|------|------|
| AD-14 | 微服务框架 | Spring Cloud 2024.0.0 + Alibaba 2023.0.1.2 | 企业主流、Spring Boot 3.x 兼容、中文社区活跃 |
| AD-15 | 注册+配置中心 | Nacos 2.3.2 | 注册+配置一体，阿里云出品，比 Eureka+Config 简洁 |
| AD-16 | 网关鉴权 | Gateway 统一 JWT → X-User-Id | 安全隔离（下游不接触 Token），现有代码零改动 |
| AD-17 | 上下文传播 | XUserFilter（OncePerRequestFilter） | 读 header → 设 SecurityContext → SecurityUtil 不改 |
| AD-18 | 限流 | Sentinel 1.8.8（Gateway 层） | 全局 QPS + 服务级热点保护，规则持久化到 Nacos |
| AD-19 | 监控 | Prometheus 3.3 + Grafana 11.6 | 云原生标准，Actuator/Micrometer 原生暴露 |
| AD-20 | Phase 1 数据库 | 共享 MySQL | 快速落地，降低初期复杂度；Phase 2 拆库 |
| AD-21 | budget 独立部署 | expense-budget 单独容器 | 从 bill-service 中拆出，独立注册 Nacos（AD-21 已更新） |
| AD-22 | 容器镜像仓库 | 阿里云 ACR 个人版（杭州） | 国内网络可达，避免 Docker Hub 拉取失败 |
| AD-23 | Nacos 动态路由 | gateway-routes.yaml | 新服务上线不重建 Gateway 镜像、不停机 |
| AD-24 | 资源管控 | 全容器 limits + JVM 堆参数 | 防止单容器 OOM 拖垮宿主机 |
| AD-25 | 统一密码 | xfylovesxy | MySQL/Redis/Nacos/Grafana/Sentinel 统一，开发环境简配 |
| AD-26 | SSE 路由特殊处理 | 非缓冲透传 + 120s 超时 | AI 流式输出不能缓冲，否则前端等完整响应才渲染 |

---

## 9. 部署架构

### 9.1 docker-compose 编排

```
依赖链:
  mysql (healthcheck)  ──▶  nacos  ──▶  gateway
       │                      │
       ├── user-service       ├── category-service
       ├── bill-service       ├── budget-service
       ├── statistics-service ├── ai-service (also needs redis)
       │                      │
       └── redis              └── nginx → frontend
```

### 9.2 启动命令

```powershell
docker compose up -d           # 启动全部 12 容器
docker compose ps              # 确认状态（12 个 Up）
# 等待 ~60s，访问 http://www.expense.com
```

### 9.3 域名

| 域名 | 指向 | hosts 配置 |
|------|------|-----------|
| `www.expense.com` | nginx → gateway → 各服务 | `127.0.0.1 www.expense.com` |
| `nacos.expense.com` | nacos:8848 | `127.0.0.1 nacos.expense.com` |

### 9.4 Nacos 控制台

- URL: `http://nacos.expense.com`
- 账号: `nacos / xfylovesxy`
- 服务列表: 7 个（user/category/bill/budget/statistics/ai/gateway）
- 配置: `expense-cloud` → `gateway-routes.yaml`

---

## 10. 实施阶段

| Sprint | 阶段 | 内容 | 状态 |
|--------|------|------|------|
| S17 | Docker + 基础设施 | 父 POM 修复、Gateway 模块、XUserFilter、各服务独立化、docker-compose、资源限制、Nacos 初始化 | ✅ 完成 |
| S18 | Nacos 集成 + 动态路由 | Nacos 注册/配置中心、Feign 服务发现、Gateway 路由迁移 Nacos、Sentinel 限流、budget 独立部署 | ✅ 完成 |
| S19 | 监控 + 收尾 | Prometheus + Grafana、全接口浏览器验收、AI BUG 修复、前端容器化验证、文档同步 | 🔄 进行中 |

---

## 11. 关键经验

详见 [integration-experience.md](../integration-experience.md)（各组件对接踩坑记录）和 [service-registry.md](../service-registry.md)（域名/账号/服务注册表）。

### 踩坑精选

1. **Nacos auth 必须提前初始化** — 首次启动 Nacos 无用户表，需 entrypoint 挂载 init.sh 延迟创建
2. **SSE 路由必须非缓冲** — Gateway 默认缓冲响应，SSE 流式需要特殊路由配置 `spring.cloud.gateway.routes[x].metadata.response-buffer-size=0`
3. **Nginx resolver 必须配置** — 上游用域名时需 `resolver 127.0.0.11 valid=30s;`，否则 Nginx 启动时 DNS 解析失败直接退出
4. **JVM 堆必须小于容器限制** — `-Xmx384m` 对应 400M 容器 limit，余量给 Metaspace + 线程栈
5. **应用模块间禁止引用** — `*-application` 之间不能加 Maven 依赖，只能通过 Feign 通信

---

> 📎 关联文档:
> - [architecture-design.md §10](../architecture-design.md) — V4.0 微服务架构
> - [development-plan.md](../development-plan.md) — Sprint 17-19
> - [iteration-log.md #015](../iteration-log.md) — V4.0 微服务架构设计 + Sprint 17 基础设施搭建
> - [integration-experience.md](../integration-experience.md) — 各组件对接踩坑记录
