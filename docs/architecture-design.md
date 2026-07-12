# 架构设计文档 — AI Expense Tracker

> **唯一架构真相来源**。任何架构变更必须更新本文档。
> 变更历史见 [iteration-log.md](iteration-log.md)。

---

## 1. 总体架构

```
┌──────────────────────────────────────────────────────────┐
│                        Client                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │              Vue 3 + Element Plus                  │  │
│  │  Vite · Pinia · Vue Router · Axios                │  │
│  │  6 个页面：登录/注册 → 仪表盘 → 记账 → 分类 → 统计   │  │
│  └──────────────────────┬─────────────────────────────┘  │
└─────────────────────────┼────────────────────────────────┘
                          │  REST API · JSON · JWT Bearer
┌─────────────────────────┼────────────────────────────────┐
│  ┌──────────────────────▼─────────────────────────────┐  │
│  │           expense-server（启动模块）                 │  │
│  │           Spring Boot 3.x 入口 + 全局配置           │  │
│  └──────────────────────┬─────────────────────────────┘  │
│                         │                                 │
│  ┌──────────────────────┼─────────────────────────────┐  │
│  │          expense-security（安全模块）                │  │
│  │          JwtFilter · SecurityConfig · TokenProvider │  │
│  └──────────────────────┼─────────────────────────────┘  │
│                         │                                 │
│  ┌──────────┬───────────┼───────────┬──────────────────┐  │
│  │  user    │ category  │ transaction│   statistics    │  │
│  │  模块     │  模块      │  模块       │   模块          │  │
│  │ ┌──────┐ │ ┌───────┐ │ ┌───────┐  │ ┌────────────┐  │  │
│  │ │Contr.│ │ │Contr. │ │ │Contr. │  │ │ Controller │  │  │
│  │ │Manag.│ │ │Manag. │ │ │Manag. │  │ │  Manager   │  │  │
│  │ │Serv. │ │ │Serv.  │ │ │Serv.  │  │ │  Service   │  │  │
│  │ │Mapper│ │ │Mapper │ │ │Mapper │  │ │   Mapper   │  │  │
│  │ │Entity│ │ │Entity │ │ │Entity │  │ │   (聚合)    │  │  │
│  │ │ DTO  │ │ │ DTO   │ │ │ DTO   │  │ └────────────┘  │  │
│  │ └──────┘ │ └───────┘ │ └───────┘  │                  │  │
│  └──────────┴───────────┴─────────────┴──────────────────┘  │
│                         │                                 │
│  ┌──────────────────────▼─────────────────────────────┐  │
│  │              expense-common（公共模块）              │  │
│  │      异常 · 工具类 · 常量 · 统一响应 · 基础DTO       │  │
│  └──────────────────────┬─────────────────────────────┘  │
└─────────────────────────┼────────────────────────────────┘
                          │  MyBatis-Plus · JDBC
┌─────────────────────────┼────────────────────────────────┐
│                  ┌──────▼──────┐                          │
│                  │   MySQL 8   │                          │
│                  │ Flyway 迁移  │                          │
│                  └─────────────┘                          │
│                    数据层                                  │
└──────────────────────────────────────────────────────────┘
```

## 2. 多模块 Maven 设计（微服务拆分预备）

### 2.1 模块划分

```
backend/
├── pom.xml                       # 父 POM：统一管理版本 + 模块聚合
├── expense-common/               # 公共模块（被所有模块依赖）
├── expense-security/             # 安全模块（被 expense-server 依赖）
├── expense-user/                 # 用户模块
│   ├── controller/
│   ├── manager/                  # Manager 编排层
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── dto/
├── expense-category/             # 分类模块（同结构）
├── expense-bill/                  # 账单模块（同结构）
├── expense-statistics/           # 统计模块（同结构）
├── expense-ai/                   # AI 智能模块（V2.0，依赖 bill + category）
└── expense-server/               # 启动模块（依赖所有业务模块 + 跨模块 Manager）
```

### 2.2 依赖关系

```
                    expense-server（启动 + 装配）
                   /    |      |        \
                  /     |      |         \
    expense-user  expense-category  expense-bill  expense-statistics  expense-ai
                  \     |      |        /             |
                   \    |      |       /              |
                  expense-security     expense-common (expense-ai also depends on bill + category)
                         \            /
                          \          /
                           expense-common
```

**规则**:
- `expense-common` 不依赖任何业务模块，只含纯工具代码
- `expense-security` 依赖 `expense-common`
- 业务模块（user/category/transaction/statistics）依赖 `expense-common`
- `expense-server` 依赖所有模块，负责组装和启动

### 2.3 V1 单体 → 未来微服务拆分路径

```
V1：Maven 多模块，单体部署                   V4：微服务
┌──────────────────────┐                 ┌──────────┐
│  expense-server      │                 │ Gateway  │
│  ┌─────┐ ┌────────┐  │                 └────┬─────┘
│  │user │ │category│  │       拆         ┌────┼──────────┐
│  │user │ │category│  │   ───────▶      │    │          │
│  │user │ │category│  │     只改        ▼    ▼          ▼
│  └─────┘ └────────┘  │    POM 和    ┌────┐┌────┐┌────┐┌────┐
│  ┌──────┐ ┌────────┐ │    启动类    │user││cat ││txn ││stat│
│  │txn   │ │stat    │ │              └────┘└────┘└────┘└────┘
│  │txn   │ │stat    │ │              各自独立 JAR，独立部署
│  │txn   │ │stat    │ │
│  └──────┘ └────────┘ │
└──────────────────────┘
```

模块内部代码**不需要任何修改**，拆分时只需：
1. 各模块独立 POM（已经是）
2. 各模块加自己的 `XxxApplication.java` 启动类
3. 服务间通信改为 Feign / Dubbo（V4 阶段）

### 2.4 父 POM 依赖管理

| 依赖 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-parent | 3.x | Spring Boot |
| mybatis-plus-spring-boot3-starter | 3.5+ | MyBatis-Plus |
| mysql-connector-j | 8.x | MySQL 驱动 |
| flyway-core + flyway-mysql | 10.x | 数据库迁移 |
| jjwt-api / impl / jackson | 0.12+ | JWT |
| lombok | 1.18+ | 注解简化 |
| spring-boot-starter-test | 3.x | 测试 |
| testcontainers | 1.19+ | 集成测试 |

---

## 3. 分层架构与 Manager 层

### 3.1 五层调用链

```
Request → Controller → Manager → Service → Mapper → DB
            │            │          │
          DTO参数     编排/聚合   原子业务
          JSR-303校验  事务控制   单一职责
```

### 3.2 各层职责对比

| 层 | 职责 | 示例 |
|----|------|------|
| **Controller** | 接收参数、JSR-303 校验、调用 Manager、封装响应 | `UserController.register(@Valid @RequestBody req)` |
| **Manager** | 编排多个 Service、处理复合业务、事务控制 | `UserManager.register()` 调用 `UserService` + `CategoryService` |
| **Service** | 单一原子业务、模块专属逻辑 | `UserService.createUser()` 只负责创建用户记录 |
| **Mapper** | MyBatis-Plus 数据访问 | `UserMapper.insert(user)` |
| **Entity** | 数据库表映射 | `@TableName("user") public class User` |

### 3.3 为什么需要 Manager 层

```java
// ❌ 反模式：没有 Manager，Service 承担编排职责，越界调用其他模块
@Service
public class UserService {
    @Autowired private CategoryService categoryService;

    public User register(RegisterRequest req) {
        User user = this.createUser(req);           // 用户逻辑
        categoryService.initDefaults(user.getId()); // 分类逻辑（越界，Service 之间互相调用）
        return user;
    }
}

// ✅ 正确：Manager 编排，Service 保持原子纯净
// Service 和 Manager 都是具体类，不需要 interface + impl
@Component
public class UserManager {
    @Autowired private UserService userService;
    @Autowired private CategoryService categoryService;

    @Transactional
    public UserVO register(RegisterRequest req) {
        User user = userService.createUser(req);              // 原子操作 1
        categoryService.initDefaultCategories(user.getId());   // 原子操作 2
        return UserAssembler.toVO(user);
    }
}

@Service
public class UserService {
    // 只做用户创建这一件事
    public User createUser(RegisterRequest req) { ... }
}
@Service
public class CategoryService {
    // 只做分类初始化这一件事
    public void initDefaultCategories(Long userId) { ... }
}
```

### 3.4 Manager 的位置策略

| 场景 | Manager 位置 | 说明 |
|------|-------------|------|
| 模块内编排 | 该模块的 `manager/` 包 | 如 UserManager 在 expense-user |
| 跨模块编排 | expense-server 的 `manager/` 包 | 如 DashboardManager 需调 UserService + TransactionService + StatisticsService |

```
expense-user/manager/              ← 模块内编排
    └── UserManager.java           (调 UserService + CategoryService)

expense-server/manager/            ← 跨模块编排
    └── DashboardManager.java      (调 UserService + TransactionService + StatisticsService)
```

**核心原则**：业务模块的 Service 只被 Manager 调用，不直接互相调用。跨模块编排统一走 expense-server 的 Manager。

### 3.5 模块内部目录结构（统一标准）

```
expense-{module}/
└── src/main/java/com/example/expense/{module}/
    ├── controller/                 # REST 接口
    │   └── XxxController.java
    ├── manager/                    # 编排层（具体类，非接口+实现）
    │   └── XxxManager.java
    ├── service/                    # 原子业务（具体类，非接口+实现）
    │   └── XxxService.java
    ├── mapper/                     # MyBatis-Plus Mapper 接口
    │   └── XxxMapper.java
    ├── entity/                     # 数据模型
    │   └── Xxx.java
    └── dto/                        # 数据传输对象
        ├── XxxRequest.java
        ├── XxxResponse.java
        └── XxxVO.java
```

---

## 4. 数据模型

### 4.1 ER 图

```
┌──────────┐       ┌──────────────┐       ┌──────────────┐
│   user   │       │   category   │       │  transaction │
├──────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)  │──┐    │ id (PK)      │       │ id (PK)      │
│ email    │  │    │ user_id (FK) │──┐    │ user_id (FK) │──┐
│ password │  │    │ name         │  │    │ category_id  │  │
│ nickname │  │    │ type         │  │    │   (FK)       │  │
│ status   │  │    │ created_time │  │    │ amount       │  │
│ created  │  │    └──────────────┘  │    │ type         │  │
│ updated  │  │                      │    │ description  │  │
└──────────┘  │                      │    │ trans_date   │  │
              └──────────────────────┘    │ created_time │  │
                  user 1:N category       │ updated_time │  │
                                          └──────────────┘  │
                                              │             │
                              user 1:N ───────┘             │
                              category N:1 ─────────────────┘
```

### 4.2 表结构（MyBatis-Plus Entity）

**user**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | @TableId(type=ASSIGN_ID) |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 邮箱 |
| password | VARCHAR(255) | NOT NULL | BCrypt |
| nickname | VARCHAR(100) | NULLABLE | 昵称 |
| status | TINYINT | DEFAULT 1 | 1=正常 0=禁用 |
| created_time | DATETIME | NOT NULL | @TableField(fill=INSERT) |
| updated_time | DATETIME | NOT NULL | @TableField(fill=INSERT_UPDATE) |

**category**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | |
| user_id | BIGINT | FK, NOT NULL | 所属用户 |
| name | VARCHAR(100) | NOT NULL | 分类名称 |
| type | VARCHAR(20) | NOT NULL | INCOME / EXPENSE |
| created_time | DATETIME | NOT NULL | |

**transaction**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | |
| user_id | BIGINT | FK, NOT NULL | 所属用户 |
| category_id | BIGINT | FK, NOT NULL | 分类 |
| amount | DECIMAL(12,2) | NOT NULL | 金额 |
| type | VARCHAR(20) | NOT NULL | INCOME / EXPENSE |
| description | VARCHAR(500) | NULLABLE | 备注 |
| transaction_date | DATE | NOT NULL | 交易日期 |
| created_time | DATETIME | NOT NULL | |
| updated_time | DATETIME | NOT NULL | |

---

## 5. API 设计

### 5.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

错误码规则：`HTTP状态码 + 两位序号`（如 `40401` = 用户不存在）

### 5.2 用户 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/users/register | 否 | 注册 |
| POST | /api/users/login | 否 | 登录，返回 JWT |

### 5.3 分类 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /api/categories | 是 | 分类列表，?type=INCOME\|EXPENSE |
| POST | /api/categories | 是 | 创建分类 |
| PUT | /api/categories/{id} | 是 | 修改分类 |
| DELETE | /api/categories/{id} | 是 | 删除分类 |

### 5.4 账单 API（核心）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | /api/transactions | 是 | 创建账单 |
| GET | /api/transactions | 是 | 查询，?type=&categoryId=&startDate=&endDate=&page=&size= |
| GET | /api/transactions/{id} | 是 | 查询单条 |
| PUT | /api/transactions/{id} | 是 | 修改账单 |
| DELETE | /api/transactions/{id} | 是 | 删除账单 |

### 5.5 统计 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | /api/statistics/monthly | 是 | 月度汇总，?year=&month= |
| GET | /api/statistics/category | 是 | 分类统计，?year=&month=&type= |

---

## 6. 安全设计

```
POST /api/users/login
        │
        ▼
┌──────────────────┐
│  验证邮箱 + 密码   │
└────────┬─────────┘
         │ 成功
         ▼
┌──────────────────┐
│  生成 JWT Token   │  ← payload: { userId, email, exp(7d) }
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  前端存储 Token   │  ← Pinia + localStorage
│  Axios 拦截器自动 │
│  附加 Header:     │
│  Authorization:   │
│  Bearer <token>   │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ JwtAuthFilter    │  ← OncePerRequestFilter
│ 解析 → 验证 →     │
│ 设置 SecurityContext│
└──────────────────┘
```

### 前端路由守卫

```
未登录 → 只能访问 /login, /register
已登录 → 自动跳转 /dashboard
Token 过期 → 清除状态 → 跳转 /login
```

---

## 7. 前端架构设计

### 7.1 技术选型理由

| 问题 | 方案 | 理由 |
|------|------|------|
| 框架 | Vue 3 Composition API | 企业主流、TypeScript 友好、生态成熟 |
| 构建 | Vite 5 | 秒级 HMR、Vue 官方推荐 |
| UI | Element Plus 2.x | 表单/表格/日期/分页组件全覆盖，国内企业广泛使用 |
| HTTP | Axios | 拦截器支持自动附加 Token |
| 状态 | Pinia | Vue 3 官方推荐，模块化存储 |
| 路由 | Vue Router 4 | 路由守卫实现权限控制 |

### 7.2 前端目录结构

```
frontend/
├── index.html
├── package.json
├── vite.config.ts                   # Vite 配置 + 代理
├── src/
│   ├── main.ts                      # 入口，挂载 App
│   ├── App.vue                      # 根组件
│   ├── api/                         # 后端 API 请求封装
│   │   ├── request.ts               # Axios 实例（baseURL、拦截器）
│   │   ├── user.ts                  # 用户相关 API
│   │   ├── category.ts              # 分类相关 API
│   │   ├── transaction.ts           # 账单相关 API
│   │   └── statistics.ts            # 统计相关 API
│   ├── router/
│   │   └── index.ts                 # 路由表 + 导航守卫
│   ├── stores/
│   │   ├── user.ts                  # Token + 用户信息
│   │   └── app.ts                   # 全局 UI 状态
│   ├── views/
│   │   ├── login/
│   │   │   └── LoginView.vue        # 登录页面
│   │   ├── register/
│   │   │   └── RegisterView.vue     # 注册页面
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue    # 首页仪表盘
│   │   ├── transactions/
│   │   │   ├── TransactionList.vue  # 账单列表
│   │   │   └── TransactionForm.vue  # 新增/编辑弹窗
│   │   ├── categories/
│   │   │   └── CategoryManage.vue   # 分类管理
│   │   └── statistics/
│   │       └── MonthlyStats.vue     # 月度统计
│   ├── components/                  # 公共组件
│   │   ├── AppLayout.vue            # 整体布局（侧边栏+顶栏+内容区）
│   │   └── AmountCard.vue           # 金额卡片
│   ├── utils/
│   │   └── auth.ts                  # Token 读写工具
│   └── assets/                      # 样式/图片
```

### 7.3 页面路由设计

| 路径 | 页面 | 认证 | 说明 |
|------|------|------|------|
| /login | LoginView | 否 | 登录 |
| /register | RegisterView | 否 | 注册 |
| /dashboard | DashboardView | 是 | 首页仪表盘（默认页） |
| /transactions | TransactionList | 是 | 账单管理 |
| /categories | CategoryManage | 是 | 分类管理 |
| /statistics | MonthlyStats | 是 | 月度统计 |

### 7.4 前后端联调方案

**开发环境**：
```
Vite Dev Server (:5173) ──proxy──▶ Spring Boot (:8080)
   /api/* 请求通过 Vite proxy 转发到后端，解决跨域
```

**生产环境**：
```
Nginx (:80) ──▶ /api/* → Spring Boot (:8080)
              ──▶ /*     → 前端静态文件
```

---

## 8. 配置规划

| 配置文件 | 用途 |
|----------|------|
| application.yml | 公共配置 |
| application-dev.yml | 开发环境（本地 MySQL） |
| application-test.yml | 测试环境（TestContainers） |
| application-prod.yml | 生产环境（预留） |
| vite.config.ts | 前端开发代理配置 |

---

## 9. 技术决策记录

| ID | 决策 | 理由 | 日期 |
|----|------|------|------|
| AD-01 | Spring Boot 3.x | 项目指定 | 2026-07-11 |
| AD-02 | MyBatis-Plus 替代 JPA | 国内企业主流、SQL 可控、分页插件成熟 | 2026-07-11 |
| AD-03 | Lombok | 减少 Entity/DTO 样板代码，企业标配 | 2026-07-11 |
| AD-04 | Maven 多模块 | V1 单体部署 + 未来无缝拆微服务 | 2026-07-11 |
| AD-05 | Vue 3 + Element Plus | 国内企业主流前端技术栈 | 2026-07-11 |
| AD-06 | 前后端分离 | 独立开发部署、Vite proxy 联调 | 2026-07-11 |
| AD-07 | JWT 无状态认证 | 简单、无服务端存储、适合前后端分离 | 2026-07-11 |
| AD-08 | Flyway 数据库迁移 | 可追溯、可重复、与 Spring Boot 集成 | 2026-07-11 |
| AD-09 | TestContainers 集成测试 | 真实 MySQL，避免 H2 兼容性问题 | 2026-07-11 |
| AD-10 | Manager 编排层 | Controller 与 Service 之间增加 Manager：Service 专注原子业务，Manager 编排多 Service | 2026-07-11 |
| AD-11 | 去掉 interface + impl | Service 和 Manager 默认为具体类，只有一个实现时无需接口；需要多态时再提取接口 | 2026-07-11 |
| AD-12 | DeepSeek API | V2.0 LLM 选用 DeepSeek（deepseek-v4-pro），OpenAI 兼容 API，RestTemplate 直调 | 2026-07-11 |
| AD-13 | AI 模块只读不写 | expense-ai 不建新表、不写数据，纯计算/分析层，LLM 结果实时返回不持久化 | 2026-07-11 |
| AD-14 | Spring Cloud 微服务 | Spring Cloud 2024.0.0 + Alibaba 2023.0.1.2，拆分为 5 个独立服务 | 2026-07-12 |
| AD-15 | Nacos 注册+配置中心 | 服务发现 + 配置管理，dev/test/prod 三 namespace 隔离 | 2026-07-12 |
| AD-16 | Gateway 统一鉴权 | Gateway 解析 JWT 注入 X-User-Id header，下游服务信任 header，移除原有 JwtAuthFilter | 2026-07-12 |
| AD-17 | XUserFilter 上下文传递 | 读 X-User-Id header → 设 SecurityContextHolder，现有 SecurityUtil 代码零改动 | 2026-07-12 |
| AD-18 | Sentinel 限流 | Gateway 全局 QPS + 服务级热点保护，规则持久化到 Nacos | 2026-07-12 |
| AD-19 | Prometheus + Grafana 监控 | Actuator/Micrometer 暴露指标，Prometheus 采集，Grafana 展示 CPU/内存/QPS/P99 | 2026-07-12 |
| AD-20 | Phase 1 共享数据库 | V4.0 Phase 1 所有服务共用同一 MySQL，Flyway 按表归属分散到各服务；Phase 2 拆库 | 2026-07-12 |
| AD-21 | expense-budget 合并入 bill-service | 预算仅 1 表 + 简单 CRUD，独立部署成本高收益低，并入 bill-service | 2026-07-12 |

---

## 10. V4.0 微服务架构

### 10.1 服务拓扑

```
                    Spring Cloud Gateway (:8080)  ← 统一入口
                          │
      ┌─────────┬─────────┼─────────┬──────────┐
      ▼         ▼         ▼         ▼          ▼
 user-svc  category  bill-svc  statistics   ai-svc
 (:8081)   -svc     (:8083)  -svc         (:8085)
           (:8082)           (:8084)
```

### 10.2 服务清单

| 服务 | 来源模块 | 端口 | 数据库表 | 依赖服务 |
|------|---------|------|---------|---------|
| **gateway** | expense-gateway（新建） | 8080 | — | Nacos |
| **user-service** | expense-user | 8081 | `user` | category-service（Feign） |
| **category-service** | expense-category | 8082 | `category` | —（叶子服务） |
| **bill-service** | expense-bill + expense-budget | 8083 | `bill`, `budget` | category-service（Feign） |
| **statistics-service** | expense-statistics | 8084 | —（聚合查询） | bill-service, category-service |
| **ai-service** | expense-ai | 8085 | —（纯计算） | statistics-service |

### 10.3 模块拆分模型（三模块）

每个对外提供 Feign API 的服务按 **-api / -common / -application** 拆为三个独立 Maven 模块：

```
expense-category-api/           ← 轻量 JAR（Feign 接口 + DTO），对外发布
expense-category-application/   ← Spring Boot 应用（只部署不发布）
(expense-category-common/)      ← 可选：服务内部共享 Entity/Utils
```

**当前项目拆分清单：**

| 服务 | API 模块 | 应用模块 | 说明 |
|------|---------|---------|------|
| category | `expense-category-api` | `expense-category-application` | CategoryClient + CategoryDTO |
| bill | `expense-bill-api` | `expense-bill-application` | BillClient + BillDTO |
| statistics | `expense-statistics-api` | `expense-statistics-application` | StatisticsClient + 3 DTOs |
| user | — | `expense-user`（单模块） | 无 Feign 消费者，暂不拆 |
| ai | — | `expense-ai`（单模块） | 无 Feign 消费者，暂不拆 |

**共享模块：**

| 模块 | 类型 | 说明 |
|------|------|------|
| **expense-common** | 共享 JAR | ApiResponse、SecurityUtil、XUserFilter、UserContextFeignInterceptor |
| **expense-security** | 共享 JAR（精简） | JwtTokenProvider |

### 10.4 上下文传播

```
Client → Gateway                          → 下游服务
         [JwtValidationGlobalFilter]          [XUserFilter]
         1. 提取 Authorization header         1. 读 X-User-Id header
         2. 验证 JWT 签名+过期                 2. 设 SecurityContextHolder
         3. 注入 X-User-Id / X-User-Email      3. SecurityUtil.getCurrentUserId()
         4. 移除 Authorization header             → 现有代码零改动
         5. 转发请求
```

### 10.5 Gateway 路由

| 路由 | 目标服务 | 特殊处理 |
|------|---------|---------|
| `/api/users/**` | user-service | — |
| `/api/categories/**` | category-service | — |
| `/api/bills/**` | bill-service | — |
| `/api/statistics/**` | statistics-service | — |
| `/api/ai/analysis/stream`, `/api/ai/report/stream` | ai-service | SSE 非缓冲透传，120s 超时 |
| `/api/ai/**` | ai-service | — |

- CORS：Gateway 统一配置
- 限流：Sentinel 在 Gateway 层 QPS 控制

### 10.6 监控架构

```
各服务 /actuator/prometheus → Prometheus 采集 → Grafana 展示
    ├── JVM: 堆内存、GC 暂停、线程数
    ├── HTTP: QPS、P99 延迟、错误率
    ├── DB: HikariCP 连接数
    └── CPU: process_cpu_usage
```

### 10.7 部署架构（docker-compose）

```
基础设施：MySQL:8.0, Redis:7, Nacos:2.3.2, Sentinel Dashboard:1.8.8
监控：    Prometheus:3.3, Grafana:11.6
应用：    Gateway, user-service, category-service, bill-service, statistics-service, ai-service
```

### 10.8 实施阶段

| Sprint | 阶段 | 内容 |
|--------|------|------|
| S17 | Docker + 基础设施 | 父 POM 修复、expense-api/gateway 模块、XUserFilter、各服务独立化、docker-compose |
| S18 | Spring Cloud 拆分 | Nacos 注册/配置中心、Feign 替换直接调用、Sentinel 限流规则 |
| S19 | 监控 + 收尾 | Prometheus+Grafana 部署、仪表盘、全链路联调、文档同步 |
