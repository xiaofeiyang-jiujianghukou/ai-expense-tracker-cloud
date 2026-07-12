# 架构设计文档 — AI Expense Tracker Cloud

> **唯一架构真相来源**。V4.0 微服务版，任何架构变更必须更新本文档。
> V1~V3 单体版已冻结，历史设计见 [design/v1-design-doc.md](design/v1-design-doc.md) ~ [design/v4-design-doc.md](design/v4-design-doc.md)。
> 变更历史见 [iteration-log.md](iteration-log.md)。

---

## 1. 总体架构（V4.0 微服务）

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                Vue 3 + Element Plus                       │  │
│  │        Vite · Pinia · Vue Router · Axios · Marked         │  │
│  │   6 页面：仪表盘 / 账单 / 分类 / 统计 / 趋势 / 预算         │  │
│  └───────────────────────┬───────────────────────────────────┘  │
└──────────────────────────┼──────────────────────────────────────┘
                           │  HTTP · REST · JSON
┌──────────────────────────┼──────────────────────────────────────┐
│                   ┌──────▼──────┐                               │
│                   │ expense-nginx│  (:80) 反向代理               │
│                   │ expense.com  │                               │
│                   └──────┬──────┘                               │
│                          │                                      │
│  ┌───────────────────────▼──────────────────────────────────┐   │
│  │            expense-gateway (:8080)                        │   │
│  │    Spring Cloud Gateway · JWT 鉴权 · CORS · Sentinel     │   │
│  │    路由配置: Nacos expense-gateway.yaml (动态刷新)        │   │
│  └───────┬───────┬───────┬───────┬───────┬───────┬──────────┘   │
│          │       │       │       │       │       │              │
│  ┌───────▼┐ ┌───▼──┐ ┌─▼───┐ ┌─▼───┐ ┌─▼───┐ ┌─▼──────────┐   │
│  │expense │ │ exp. │ │exp. │ │exp. │ │exp. │ │expense-ai  │   │
│  │ -user  │ │ cat. │ │bill │ │bud. │ │stat.│ │(:8085)     │   │
│  │(:8081) │ │(:8082│ │(:83)│ │(:86)│ │(:84)│ │SSE + Redis │   │
│  └───┬────┘ └──────┘ └──┬──┘ └──┬──┘ └──┬──┘ └──────┬─────┘   │
│      │                  │       │       │           │          │
│      └──────────────────┴───────┴───────┴───────────┘          │
│                         │  Feign (lb://)                        │
│  ┌──────────────────────┼──────────────────────────────────┐   │
│  │              共享基础设施                                │   │
│  │  ┌────────┐ ┌───────┐ ┌───────┐                         │   │
│  │  │ MySQL  │ │ Redis │ │ Nacos │  (注册中心 + 配置中心)    │   │
│  │  │ :3306  │ │ :6379 │ │:8848  │                         │   │
│  │  └────────┘ └───────┘ └───────┘                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                     Docker Compose 容器编排                      │
└─────────────────────────────────────────────────────────────────┘
```

### 1.1 容器清单（12 容器）

| 容器 | 端口 | 类型 | 说明 |
|------|------|------|------|
| **expense-nginx** | 80 | 反向代理 | 域名路由，上游动态解析 |
| **expense-frontend** | 5173 | 前端 | Vue 3 开发服务器 |
| **expense-gateway** | 8080 | 网关 | JWT 鉴权 + CORS + Nacos 动态路由 |
| **expense-user** | 8081 | 微服务 | 注册/登录，JWT 生成 |
| **expense-category** | 8082 | 微服务 | 分类 CRUD（叶子服务） |
| **expense-bill** | 8083 | 微服务 | 账单 CRUD + CSV 导出 |
| **expense-budget** | 8086 | 微服务 | 预算管理 |
| **expense-statistics** | 8084 | 微服务 | 月度统计 / 趋势 / Excel 导出 |
| **expense-ai** | 8085 | 微服务 | AI 分类建议 / 消费洞察 / 报告（SSE 流式） |
| **expense-mysql** | 3306 | 数据库 | MySQL 8.0 |
| **expense-redis** | 6379 | 缓存 | AI 分析缓存 |
| **expense-nacos** | 8848/9848 | 注册配置中心 | 服务发现 + 动态路由配置 |

---

## 2. 模块结构

```
backend/
├── pom.xml                          # Maven 父 POM（版本管理 + 模块聚合）
├── Dockerfile.base-builder          # 共享基础镜像（只含 expense-framework）
├── expense-framework/               # 共享框架
│   └── config/
│       ├── ApiResponseDecoder.java  #   Feign 通用解码器（解包 ApiResponse.data）
│       ├── JwtTokenProvider.java    #   JWT 生成+校验（@ConditionalOnProperty）
│       ├── MyBatisPlusConfig.java   #   MyBatis-Plus 分页插件（@ConditionalOnClass）
│       ├── UserContextFeignInterceptor.java  #   Feign X-User-Id 传播
│       └── FeignErrorDecoder.java   #   Feign 错误解码
│
├── expense-gateway/                 # API 网关（单模块）
├── expense-user/                    # 用户服务（单模块，待拆三模块）
├── expense-budget/                  # 预算服务（单模块，待拆三模块）
├── expense-ai/                      # AI 服务（单模块，待拆三模块）
│
├── expense-category/                # 分类服务（三模块）
│   ├── expense-category-api/        #   Feign 接口 + DTO
│   ├── expense-category-common/     #   内部共享 DTO
│   └── expense-category-application/ #  Controller + Service + Mapper
│
├── expense-bill/                    # 账单服务（三模块）
│   ├── expense-bill-api/
│   ├── expense-bill-common/
│   └── expense-bill-application/
│
└── expense-statistics/              # 统计服务（三模块）
    ├── expense-statistics-api/
    ├── expense-statistics-common/
    └── expense-statistics-application/
```

### 2.1 模块依赖原则

- `expense-framework` 是所有服务的共享基础，包含通用配置和工具
- 微服务间通过 Feign (`lb://expense-xxx`) 通信，禁止应用模块间直接 Maven 依赖
- 对外提供 Feign API 的服务按三模块拆分（api / common / application）
- 单模块服务暂不拆分（无 Feign 消费者，待后续统一）

---

## 3. 认证架构

```
Client → Nginx → Gateway [JwtValidationGlobalFilter]
                      │
                      1. 提取 Authorization: Bearer <token>
                      2. 验证 JWT 签名 + 过期
                      3. 注入 X-User-Id + X-User-Email header
                      4. 移除 Authorization header（不透传给下游）
                      │
                      ▼
                  下游服务 [XUserFilter]
                      1. 读 X-User-Id header
                      2. 设置 SecurityContextHolder
                      3. SecurityUtil.getCurrentUserId() → 现有代码零改动
                      │
                      ▼
                  Feign 调用 [UserContextFeignInterceptor]
                      1. 读当前 RequestContext 中的 X-User-Id
                      2. 复制到 Feign Request header
```

**关键组件**：

| 组件 | 位置 | 职责 |
|------|------|------|
| `JwtValidationGlobalFilter` | expense-gateway | Gateway GlobalFilter，解析 JWT → 注入 header |
| `XUserFilter` | expense-framework | 下游 OncePerRequestFilter |
| `JwtTokenProvider` | expense-framework | JWT 生成 + 校验（仅 user/gateway 使用，`@ConditionalOnProperty("jwt.secret")`） |
| `UserContextFeignInterceptor` | expense-framework | Feign RequestInterceptor |

### 3.1 SSE 线程上下文传播

SSE 流式端点使用 `ttlExecutor` 在后台线程处理。必须显式传播 `SecurityContext` + `RequestContextHolder`，否则 Feign 调用丢失用户上下文：

```java
SecurityContext ctx = SecurityContextHolder.getContext();
RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
ttlExecutor.execute(() -> {
    SecurityContextHolder.setContext(ctx);
    RequestContextHolder.setRequestAttributes(attrs);
    try { /* Feign 调用 */ }
    finally { RequestContextHolder.resetRequestAttributes(); }
});
```

---

## 4. Gateway 动态路由

路由配置存储在 Nacos `expense-gateway.yaml`（`expense-cloud` group），Gateway 通过 `spring.config.import` 加载并自动刷新。

| 路由 | 目标 | 特殊处理 |
|------|------|---------|
| `/api/users/**` | lb://expense-user | — |
| `/api/categories/**` | lb://expense-category | — |
| `/api/bills/**` | lb://expense-bill | — |
| `/api/budgets/**` | lb://expense-budget | — |
| `/api/statistics/**` | lb://expense-statistics | — |
| `/api/ai/analysis/stream` | lb://expense-ai | SSE 非缓冲透传，120s 超时 |
| `/api/ai/report/stream` | lb://expense-ai | 同上 |
| `/api/ai/**` | lb://expense-ai | — |

新服务上线只需在 Nacos 控制台更新 `expense-gateway.yaml`，Gateway 自动热刷新，不停机。

---

## 5. 数据模型

### 5.1 ER 图

```
┌──────────┐       ┌──────────────┐       ┌──────────────┐
│   user   │       │   category   │       │    bill      │
├──────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)  │──┐    │ id (PK)      │       │ id (PK)      │
│ email    │  │    │ user_id (FK) │──┐    │ user_id (FK) │──┐
│ password │  │    │ name         │  │    │ category_id  │  │
│ nickname │  │    │ type         │  │    │   (FK)       │  │
│ status   │  │    │ created_time │  │    │ amount       │  │
│ created  │  │    └──────────────┘  │    │ type         │  │
│ updated  │  │                      │    │ description  │  │
└──────────┘  │                      │    │ bill_date    │  │
              └──────────────────────┘    │ created_time │  │
                  user 1:N category       │ updated_time │  │
                                          └──────────────┘  │
                                              │             │
                              user 1:N ───────┘             │
                              category N:1 ─────────────────┘

┌──────────────┐
│   budget     │
├──────────────┤
│ id (PK)      │
│ user_id (FK) │
│ category_id  │
│ year, month  │
│ amount       │
│ created_time │
└──────────────┘
```

### 5.2 表分布（Phase 1 共享 MySQL）

| 表 | 所属服务 | Flyway 迁移 |
|----|---------|-------------|
| `user` | expense-user | V1 |
| `category` | expense-category | V2 |
| `bill` | expense-bill | V3~V5 |
| `budget` | expense-budget | V6 |

---

## 6. API 设计

### 6.1 统一响应

```json
{ "code": 200, "message": "success", "data": {} }
```

错误码规则：`HTTP状态码 + 两位序号`

### 6.2 用户 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/users/register | 否 | 注册（自动创建默认分类） |
| POST | /api/users/login | 否 | 登录，返回 JWT（7 天过期） |

### 6.3 分类 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/categories/list | 是 | 用户分类列表 |
| POST | /api/categories/save | 是 | 创建/更新分类 |
| POST | /api/categories/delete | 是 | 删除分类 |

### 6.4 账单 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/bills | 是 | 创建账单 |
| POST | /api/bills/list | 是 | 筛选分页查询 |
| POST | /api/bills/update | 是 | 修改账单 |
| POST | /api/bills/delete | 是 | 删除账单 |
| POST | /api/bills/export-csv | 是 | 导出 CSV |

### 6.5 统计 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/statistics/monthly | 是 | 月度汇总（收入/支出/结余/分类明细） |
| POST | /api/statistics/trend | 是 | N 月收支趋势 |
| POST | /api/statistics/daily | 是 | 日支出分布（31 天） |
| POST | /api/statistics/export-excel | 是 | 导出 Excel（2 Sheet） |

### 6.6 预算 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/budgets/list | 是 | 按月查询预算列表 |
| POST | /api/budgets/save | 是 | 保存/更新预算 |
| POST | /api/budgets/delete | 是 | 删除预算 |

### 6.7 AI API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/ai/categorize | 是 | AI 分类建议（描述+金额→分类+置信度） |
| POST | /api/ai/analysis | 是 | 消费洞察（同步） |
| POST | /api/ai/analysis/stream | 是 | 消费洞察（SSE 流式） |
| POST | /api/ai/report | 是 | 财务报告（同步） |
| POST | /api/ai/report/stream | 是 | 财务报告（SSE 流式） |
| POST | /api/ai/budget-advice | 是 | AI 预算建议 |
| POST | /api/ai/anomaly | 是 | 异常检测 |

### 6.8 API 规范

- URL 前缀 `/api/`，只使用 `@GetMapping` / `@PostMapping`
- 多参数场景一律 `@PostMapping` + `@RequestBody`
- 写操作返回 `ApiResponse<Void>`
- 前端导出使用 `responseType: 'blob'`，后端不设 `Content-Disposition: attachment`（避免 blob 下载冲突）

---

## 7. 前端架构

### 7.1 技术栈

| 技术 | 用途 |
|------|------|
| Vue 3 (Composition API) | 框架 |
| Vite 5 | 构建 |
| Element Plus 2.x | UI 组件 |
| Axios | HTTP（拦截器 + Token 刷新） |
| Pinia | 状态管理 |
| Vue Router 4 | 路由 + 守卫 |
| ECharts 5 | 图表（折线/饼图/柱图） |
| Marked | Markdown 渲染（AI 报告） |

### 7.2 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| /login | 登录 | 未登录重定向到此 |
| /register | 注册 | — |
| /dashboard | 仪表盘 | AI 消费洞察 + 最近账单 |
| /bills | 账单管理 | 筛选/分页/CRUD/导出 CSV |
| /categories | 分类管理 | CRUD |
| /statistics | 月度统计 | ECharts 饼图 + AI 报告 + 导出 Excel |
| /trends | 趋势分析 | ECharts 折线图 + 柱图 |
| /budget | 预算管理 | 设置预算 + AI 建议 |

### 7.3 前后端联调

| 环境 | 方案 |
|------|------|
| 开发 | Nginx :80 → frontend :5173 / Gateway :8080 |
| 部署 | Docker Compose，所有容器同一网络 `expense-net` |

---

## 8. 容器化与构建

### 8.1 基础镜像（`Dockerfile.base-builder`）

- **只包含 `expense-framework`**，不含任何微服务
- 通过 `mvn install -f expense-framework/pom.xml` 直接构建
- 服务构建时 `FROM expense-base-builder`，继承 framework JAR + 依赖缓存

### 8.2 服务 Dockerfile

- 每个服务只 COPY 自己 + 编译依赖的兄弟模块源码和 POM
- 使用 `mvn install -f <dep>/pom.xml` 逐个安装编译依赖
- 使用 `mvn package -f <self>/pom.xml` 打包自身
- 禁止 `-pl -am`（触发 Maven reactor 验证所有模块）

### 8.3 构建流程

```powershell
# 1. 基础镜像
docker build -t expense-base-builder:latest -f backend/Dockerfile.base-builder backend/
# 2. 按依赖顺序逐个构建
docker compose build expense-gateway expense-category expense-user \
                       expense-bill expense-budget expense-statistics expense-ai
# 3. 部署
docker compose up -d
```

---

## 9. 部署架构（docker-compose）

```
依赖链:
  mysql (healthcheck) ──▶ nacos ──▶ gateway ──▶ nginx
     │                      │
     ├── user-service       ├── category-service
     ├── bill-service       ├── budget-service
     ├── statistics-service ├── ai-service (also needs redis)
     │                      │
     └── redis ◀────────────┘
```

所有容器统一网络 `expense-net`，资源配置 `deploy.resources.limits`（memory + cpus），JVM 容器 `-Xmx` < 容器 memory limit。

### 9.1 域名

| 域名 | 指向 | hosts |
|------|------|-------|
| www.expense.com | nginx → gateway → 各服务 | `127.0.0.1 www.expense.com` |
| nacos.expense.com | nacos:8848 | `127.0.0.1 nacos.expense.com` |

### 9.2 Nacos

- URL: `http://nacos.expense.com`（nacos / 环境变量）
- 服务注册: `expense-cloud` group，public namespace
- 路由配置: `expense-gateway.yaml`（`expense-cloud` group）
- 首次启动自动初始化: `nacos-init.sh` 修改密码 + 上传路由

---

## 10. 技术决策

| ID | 决策 | 选择 | 理由 |
|----|------|------|------|
| AD-01 | 后端框架 | Spring Boot 3.4 + Spring Cloud 2024.0.0 | 企业主流 |
| AD-02 | ORM | MyBatis-Plus 3.5.9 | 国内主流、分页插件成熟 |
| AD-03 | 注册/配置中心 | Nacos 2.3.2 | 注册+配置一体 |
| AD-04 | 网关 | Spring Cloud Gateway | 响应式、Java 原生集成 |
| AD-05 | 服务间通信 | Feign + Nacos lb:// | 声明式、自动负载均衡 |
| AD-06 | 认证 | Gateway JWT → X-User-Id | 安全隔离、下游零改动 |
| AD-07 | 限流 | Sentinel（Gateway 层） | Alibaba 生态 |
| AD-08 | 前端 | Vue 3 + Element Plus | 国内主流 |
| AD-09 | LLM | DeepSeek v4-pro（AgentScope） | OpenAI 兼容 |
| AD-10 | Phase 1 数据库 | 共享 MySQL | 降低初期复杂度 |
| AD-11 | 容器镜像源 | 阿里云 ACR 个人版 | 国内可达 |
| AD-12 | 构建优化 | 基础镜像 + mvn -f 按需构建 | 免去 reactor 全量构建 |
| AD-13 | Feign 解码 | ApiResponseDecoder 自定义 | 兼容 ApiResponse 包装和裸数据 |
| AD-14 | 分页 | MyBatis-Plus + jsqlparser 3.5.9 | 3.5.9 起 PaginationInnerInterceptor 独立模块 |

---

> 📎 关联文档:
> - [v4-design-doc.md](design/v4-design-doc.md) — V4.0 完整设计决策和踩坑记录
> - [project-requirements.md](project-requirements.md) — 需求文档
> - [development-plan.md](development-plan.md) — Sprint 计划
> - [iteration-log.md](iteration-log.md) — 变更历史
> - [development-standards.md](development-standards.md) — 开发规范
