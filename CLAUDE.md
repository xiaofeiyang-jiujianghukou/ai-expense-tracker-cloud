# CLAUDE.md — AI Expense Tracker Cloud

> 本文档是 Claude Code 的项目工作指南。每次会话开始时，Claude Code 会先阅读本文档以理解项目全貌。
> 工作流：**Analyze（分析） → Design（设计） → Implement（实现） → Review（审查）**

---

## 1. 项目概况

**AI Expense Tracker Cloud**（AI 智能个人财务管理系统 — 微服务版）是基于单体版 `ai-expense-tracker`（V3.0）的分布式微服务改造项目。

- **当前版本**: V4.0
- **核心目标**: 将 V1~V3 单体应用拆分为微服务架构 — Docker 容器化、K8s 编排、CI/CD、云部署
- **项目阶段**: Sprint 17 完成 → Sprint 18（Spring Cloud 微服务拆分）待启动
- **架构模式**: Spring Cloud 微服务 + Docker + K8s（前身：Maven 多模块单体）
- **代码来源**: 完全复制自单体项目 `ai-expense-tracker`（V3.0 最终版），模块间零直接依赖，已具备微服务拆分基础

---

## 2. 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 语言 |
| Spring Boot | 3.x | 框架 |
| Spring MVC | 6.x | Web 层 |
| MyBatis-Plus | 3.5+ | ORM（替代 JPA） |
| Spring Security | 6.x | 安全框架 |
| JWT (jjwt) | 0.12+ | 无状态认证 |
| MySQL | 8.0 | 数据库 |
| Flyway | 10.x | 数据库版本管理 |
| Lombok | 1.18+ | 减少样板代码 |
| AgentScope | 2.0.0-RC5 | AI 智能体框架（V2.0） |
| DeepSeek API | v4-pro | LLM 模型 |
| Maven | 3.x | 构建 & 多模块管理 |
| Spring Cloud | 2024.0.0 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.2 | Nacos + Sentinel |
| Nacos | 2.3.2 | 注册中心 + 配置中心 |
| Sentinel | 1.8.8 | 流量控制 + 熔断降级 |
| Docker | 26+ | 容器化 |
| Prometheus | 3.3 | 指标采集 |
| Grafana | 11.6 | 监控可视化 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 前端框架（Composition API） |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Axios | 1.x | HTTP 客户端 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由 |

### 测试

| 技术 | 用途 |
|------|------|
| JUnit 5 | 单元测试 |
| Mockito | Mock 测试 |
| TestContainers | 集成测试（真实 MySQL） |

---

## 3. 关键文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目需求 | [docs/project-requirements.md](docs/project-requirements.md) | 唯一需求真相来源（含前端需求） |
| 架构设计 | [docs/architecture-design.md](docs/architecture-design.md) | 唯一架构真相来源（V4.0 微服务架构） |
| 开发计划 | [docs/development-plan.md](docs/development-plan.md) | Sprint 级别任务分解 |
| 迭代日志 | [docs/iteration-log.md](docs/iteration-log.md) | 需求/架构变更追踪 |
| 开发规范 | [docs/development-standards.md](docs/development-standards.md) | 架构原则 + 编码规范 + 流程规范 + 文档规范 + 反模式（持续演进） |
| V1 设计稿 | [docs/design/v1-design-doc.md](docs/design/v1-design-doc.md) | V1.0 设计目标、效果、思路（已验收，冻结） |
| V2 设计稿 | [docs/design/v2-design-doc.md](docs/design/v2-design-doc.md) | V2.0 AI 模块设计（已验收，冻结） |
| V3 设计稿 | [docs/design/v3-design-doc.md](docs/design/v3-design-doc.md) | V3.0 可视化/预算/导出设计（已验收，冻结） |
| V4 设计稿 | [docs/design/v4-design-doc.md](docs/design/v4-design-doc.md) | V4.0 微服务工程化改造设计（当前版本，持续更新） |

---

## 4. 项目目录结构

```
ai-expense-tracker-cloud/
├── CLAUDE.md
├── README.md
├── docker-compose.yml                 # V4.0 容器编排（11 个容器）
├── prometheus/
│   └── prometheus.yml                 # Prometheus 采集配置
├── grafana/
│   ├── dashboards/
│   │   ├── dashboard.yml              # Dashboard 注册
│   │   └── expense-cloud-overview.json # 监控仪表盘
│   └── datasources/
│       └── prometheus.yml             # Prometheus 数据源
├── docs/
│   ├── project-design.md              # 原始设计（只读参考）
│   ├── project-requirements.md        # 需求文档（持续更新）
│   ├── architecture-design.md         # 架构设计（持续更新，含 V4.0 §10）
│   ├── development-plan.md            # 开发计划（Sprint 0-19）
│   ├── iteration-log.md               # 迭代日志（#001-#015）
│   └── design/
│       └── v1-design-doc.md           # V1.0 设计稿
│
├── backend/                           # Maven 多模块父项目
│   ├── pom.xml                        #   父 POM（Spring Cloud BOM + Alibaba BOM + 模块聚合）
│   ├── Dockerfile.base-builder        #   共享基础镜像（3 个 starter：web/orm/redis）
│   ├── expense-starter-web/           #   Web Starter（Web/Security/Feign/JWT/Nacos/Sentinel）
│   ├── expense-starter-orm/           #   ORM Starter（MyBatis/DataSource/Flyway，含 web）
│   ├── expense-starter-redis/         #   Redis Starter（Redis 配置，含 web）
│   ├── expense-gateway/               #   API 网关 :8080（单模块，reactive）
│   ├── expense-user/                  #   用户服务 :8081（三模块）
│   │   ├── expense-user-api/          #     Feign 接口 + DTO
│   │   ├── expense-user-common/       #     共享 DTO
│   │   └── expense-user-application/  #     Spring Boot 应用
│   ├── expense-category/              #   分类服务 :8082
│   │   ├── expense-category-api/      #     Feign 接口 + DTO
│   │   ├── expense-category-common/   #     内部共享 DTO
│   │   └── expense-category-application/ #  Spring Boot 应用
│   ├── expense-bill/                  #   账单服务 :8083
│   │   ├── expense-bill-api/
│   │   ├── expense-bill-common/
│   │   └── expense-bill-application/
│   ├── expense-statistics/            #   统计服务 :8084
│   │   ├── expense-statistics-api/
│   │   ├── expense-statistics-common/
│   │   └── expense-statistics-application/
│   ├── expense-budget/                #   预算服务 :8086（三模块）
│   │   ├── expense-budget-api/
│   │   ├── expense-budget-common/
│   │   └── expense-budget-application/
│   ├── expense-ai/                    #   AI 服务 :8085（三模块）
│   │   ├── expense-ai-api/
│   │   ├── expense-ai-common/
│   │   └── expense-ai-application/
│
└── frontend/                          # Vue 3 前端项目（独立）
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/                       # 后端 API 封装
        ├── router/                    # 路由配置 + 守卫
        ├── stores/                    # Pinia 状态（Token、用户信息）
        ├── views/                     # 页面组件
        │   ├── login/                 # 登录/注册
        │   ├── dashboard/             # 首页仪表盘
        │   ├── transactions/          # 账单管理
        │   ├── categories/            # 分类管理
        │   └── statistics/            # 月度统计
        ├── components/                # 公共组件
        ├── utils/                     # 工具函数（Axios 实例、Token 管理）
        └── assets/                    # 静态资源
```

---

## 5. 开发规范

### 5.1 后端分层职责

```
单 Service：   Controller → Service → Mapper → DB
多 Service：   Controller → Manager → Service → Mapper → DB
```

**Manager 不是必须的层。** 只有编排 2+ 个 Service 时才创建 Manager；单 Service 调用时 Controller 直调 Service。禁止写只做透传的 Manager。

| 层 | 职责 | 禁止 |
|----|------|------|
| **Controller** | 参数接收、JSR-303 校验、调用 Service/Manager、返回结果 | **严禁任何业务逻辑**（包括但不限于：文件生成/拼接、数据转换、算法计算、第三方库调用如 EasyExcel/POI） |
| **Manager** | 编排多个 Service、处理复合业务、事务控制（需要时才加） | 单 Service 透传、直接访问 Mapper |
| **Service** | 单一原子业务、模块专属逻辑 | — |
| **Mapper** | 数据访问（MyBatis-Plus BaseMapper） | 写业务逻辑 |
| **Entity** | 数据模型定义 | 写逻辑代码 |
| **DTO** | 接口数据传输对象（Request/Response/VO） | — |

**Controller 反例（严禁）：**

```java
// ❌ Controller 中写业务逻辑 — 严重违规
@PostMapping("/export-excel")
public ResponseEntity<byte[]> exportExcel(@RequestBody Request req) {
    // 这些全部是业务逻辑，必须在 Manager/Service 中实现：
    MonthlyStatsVO stats = manager.getMonthlyStats(...);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    EasyExcel.write(out).sheet("...").head(...).doWrite(...);  // ← 第三方库调用
    // ...
}
```

```java
// ✅ Controller 只做委托
@PostMapping("/export-excel")
public ResponseEntity<byte[]> exportExcel(@RequestBody Request req) {
    Long userId = SecurityUtil.getCurrentUserId();
    byte[] bytes = statisticsManager.exportExcel(userId, req.getYear(), req.getMonth());
    // 只做 HTTP 层面的包装（Content-Type、Content-Disposition header）
    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("..."))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=...")
            .body(bytes);
}
```

**Manager 命名**: `{Domain}Manager`，表明编排的业务领域（如 `UserManager` 编排注册流程，调 UserService + CategoryService + JwtTokenProvider）。

**Manager vs Service 的边界：**

```java
// ❌ 没有 Manager：Service 承担编排，越界调用其他模块
UserService {
    register() {
        createUser();
        categoryService.initDefaults();  // ← 越界了
    }
}

// ✅ 有 Manager：各层职责清晰，类本身就是实现
UserManager {
    @Transactional
    register(request) {
        userService.createUser();              // 原子 1
        categoryService.initDefaultCategories(); // 原子 2
    }
}
UserService {
    createUser() { /* 只做用户创建 */ }
}
CategoryService {
    initDefaultCategories() { /* 只做分类初始化 */ }
}
```

> **关于接口**：Service 和 Manager 默认是具体类，不强行定义 interface + impl。只有当需要多态（如策略模式、多实现切换）时才提取接口，此时接口名描述能力（如 `PaymentStrategy`），实现名描述具体策略（如 `AlipayPayment`、`WechatPayment`）。

### 5.2 前端分层职责

| 层 | 职责 |
|----|------|
| **views/** | 页面布局 + 组件组装，不含复杂业务逻辑 |
| **components/** | 可复用组件，不直接调用 API |
| **api/** | 封装所有后端请求，view 通过 api/ 拿数据 |
| **stores/** | 全局状态（Token、用户信息），不存页面私有状态 |
| **router/** | 页面路由 + 导航守卫（未登录重定向） |

### 5.3 API 设计

- URL 前缀 `/api/`，请求/响应格式 JSON
- JWT Bearer Token 认证，统一响应体 `{ code, message, data }`
- **仅使用 `@GetMapping` 和 `@PostMapping`**，禁止 `@PutMapping` / `@DeleteMapping`
- `@GetMapping` 仅限单参数（路径变量 `/{id}`）；多参数筛选一律 `@PostMapping`
- **`@PostMapping` 必须使用 `@RequestBody`，严禁使用 `@RequestParam`**
- 写操作不返回对象，只返回 `ApiResponse<Void>`
- 禁止全限定类名内联（如 `com.xxx.Foo`），无冲突时一律用 import

### 5.4 数据库建表规范

> 详见 [development-standards.md §2.5](docs/development-standards.md)

- **表和字段必须加 COMMENT**，不允许无注释建表
- 索引命名：`uk_xxx`（唯一）、`idx_xxx`（普通）
- 统一 utf8mb4 + InnoDB

### 5.5 配置管理规范

> 详见 [development-standards.md §2.6](docs/development-standards.md)

- **敏感信息（数据库密码等）走环境变量，无默认值**
- 项目级配置（库名、JWT 秘钥）可硬编码
- `${VAR}` 无默认值 = 强制设置；`${VAR:default}` = 可选
- **环境变量命名分两类**：全局共享（`DB_HOST`、`LLM_API_KEY` 等）和项目专属（`EXPENSE_` 前缀，如 `EXPENSE_DB_PASSWORD`）。详见 [development-standards.md §2.6](docs/development-standards.md)
- **设置前先检查是否存在同名变量**：同名同值不覆盖，同名异值用项目前缀新建

### 5.6 验收与联调规范

> 详见 [development-standards.md §3.4-3.5](docs/development-standards.md)

- **验收流程不可跳过**：编译 → API 模拟测试 → 浏览器验收 → 增量回归 → 人工验收
- 测试方式必须如实标注：PowerShell/curl =「API模拟」、浏览器 =「浏览器」、不可混淆
- 验收报告必须含「预期」和「实测」两列，实测填具体数值，不能只写"通过"
- 增量验证原则：修复 BUG 后只验证修复项及其影响范围
- 联调验收必须在浏览器中操作每个按钮/交互，打开 DevTools 确认零错误

- **每次开发完成后必须进行完整联调测试**，不能仅靠单接口验证
- 13 项联调清单必须全部通过：注册→登录→CRUD→统计→Token 过期
- 前端体验：操作有反馈、删除有确认、Loading 防重复、空状态占位
- 未登录访问任何页面跳转登录页，不显示 403/空白
- 401/403 自动清 Token 跳登录；200+code≠200 用 ElMessage 展示错误

### 5.7 Docker 构建规范

#### 5.7.1 基础镜像原则

- **基础镜像（`Dockerfile.base-builder`）只包含共享 starter**（`expense-starter-web`、`expense-starter-orm`、`expense-starter-redis`），不包含任何微服务相关内容
- 基础镜像按依赖顺序构建：`mvn install -f expense-starter-web/pom.xml` → orm → redis，不依赖 Maven reactor
- 父 POM 通过 `mvn install -N` 安装到本地仓库（子模块需要引用）
- **禁止在基础镜像中 COPY 其他微服务的 POM 或源码**

#### 5.7.2 微服务 Dockerfile 原则

- 每个服务 Dockerfile **只 COPY 自己 + 编译依赖的兄弟模块源码和 POM**
- 使用 `mvn install -f <dep>/pom.xml` 逐个安装编译依赖，再用 `mvn package -f <self>/pom.xml` 打包自身
- **禁止使用 `-pl` 和 `-am`**（会触发 Maven reactor 验证所有模块）
- 构建完成后 `docker compose up -d` 统一部署

#### 5.7.3 构建流程

```powershell
# 1. 构建基础镜像（首次 ~2 min，后续缓存秒过）
docker build -t expense-base-builder:latest -f backend/Dockerfile.base-builder backend/

# 2. 按依赖顺序逐个构建微服务（每个 ~30-60s）
docker compose build expense-gateway     # 只依赖 framework
docker compose build expense-category    # 叶子服务
docker compose build expense-user        # 依赖 category-api
docker compose build expense-bill        # 依赖 category-api
docker compose build expense-statistics  # 依赖 category-api + bill-api
docker compose build expense-budget      # 依赖 category-api + statistics-api
docker compose build expense-ai          # 依赖全部 api

# 3. 全量部署
docker compose up -d
```

#### 5.7.4 构建反馈要求

- **逐个构建**，不得并行
- 每个服务构建完成后即时汇报结果（成功/失败 + 耗时）
- 失败时分析根因，修复后重新构建该服务，不跳过

### 5.8 Git 规范

（内容同前，略）

### 5.9 枚举与常量规范

- **固定值集合（如 INCOME/EXPENSE）必须定义枚举**，禁止魔法字符串散落各层
- **枚举自带关联数据**：如 `BillType` 枚举携带默认分类列表，不另建静态 Map
- **分层边界做转换**：Service 层接受枚举入参，Controller 层做 `String → Enum` 转换（`BillType.valueOf(str)`），DB 层存 `.name()`
- **命名避开 Java/Spring 通用关键字**：账单用 `Bill`/`BillType`/`billDate`，不用 `Transaction`（与 `@Transactional` 冲突）；命名前先确认不会产生歧义
- 前端模板中如需判断类型，优先用枚举 `name()` 值做字符串比较

### 5.10 测试要求

- Service 层单元测试覆盖率 > 80%
- Controller 使用 MockMvc 测试
- 集成测试使用 TestContainers 启动真实 MySQL

---

## 6. Claude Code 工作流程

1. **Analyze** — 阅读相关代码，输出当前实现、问题、修改方案
2. **Design** — 确认文件变化、数据变化、API 变化
3. **Implement** — 修改代码、编写测试、运行测试确认通过
4. **Review** — 检查代码质量、性能、安全、测试完整性

#### 工作习惯规范

- **重复问题必须记录**：同一种错误出现 2 次以上，立即写入 `memory/troubleshooting.md` 并更新 `MEMORY.md` 索引，防止下次重复折腾
- **高频命令缓存**：执行超过 3 次的命令序列（如重启后端）记录到 troubleshooting.md，下次直接复制
- **安全分类器拦截**：PowerShell 命令含敏感信息（API key、密码等）会被拦截。用 `run_in_background: true` 绕过；或分步先在 shell 设 env vars，再单独执行命令
- **命名防冲突**：项目专用名词避开 Java/Spring 通用关键字（如 `Bill`/`BillType` 而非 `Transaction`/`TransactionType`）— 详见 §5.8 枚举规范
- **🚫 应用模块间禁止引用（优先级：最高）**：`*-application` 模块之间不能互相添加 Maven 依赖。服务间通信只能通过 Feign（API 模块）+ 网关，或共享 Common 模块。例如 bill-application 不能依赖 budget，反之亦然。要合并功能就把代码挪到一个模块里，不要把整个应用模块当依赖引入。
- 遇到未知错误先追根究底，不在同一个问题上反复尝试同一种方法
- **🎯 解决问题优先，不降级绕路（优先级：最高）**：面对问题时优先找到正确方案并解决它，而不是找替代路径绕过去。例如 Dockerfile 模块名不匹配 → 修正 Dockerfile 对齐，而非切回 mvn spring-boot:run。只有正确方案确实不可行（如外部依赖缺失）时才考虑迂回方式。**任何降级/绕路决定必须先征求用户同意**，不得自行决定。笨方式（逐个手动启动）是最后手段，能用工具/框架能力批量解决的优先用工具。
- **🚫 禁止自行降级或绕过（优先级：最高）**：遇到编译失败、启动失败、依赖解析失败等问题时，**必须先分析根因并反馈给用户**，由用户决定是否降级、绕过或采用替代方案。严禁自行执行以下操作：
  - 手动复制 JAR 到本地仓库绕过 `mvn install`/`mvn deploy`
  - 跳过失败模块只编译部分模块
  - 注释掉报错代码
  - 硬编码绕过配置检查
  - 任何其他绕过标准构建/部署流程的操作
  - **正确流程**：分析问题 → 提供方案（含利弊） → 用户选择 → 执行
- **容器镜像拉取规范**：启动 Docker 容器前，必须先询问用户是否需要使用私有镜像仓库，避免直接从 Docker Hub 拉取因网络原因失败。用户确认镜像地址后再执行。
- **⚡ 容器最小资源配置（优先级：高）**：所有 Docker 容器必须配置 `deploy.resources.limits`（memory + cpus），遵循最小必要原则。JVM 容器需同时设置 `-Xms` / `-Xmx` 确保堆内存小于容器限制，否则 OOMKilled。新加容器时主动去查官方/社区推荐的最低配置，不拍脑袋。

### 5.11 AI 模块规范

- **LLM 提供商**: DeepSeek（OpenAI 兼容 API），通过 RestTemplate 直调
- **配置**: LLM 相关走全局变量 `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` / `LLM_PROVIDER`，切换模型只需改值
- **模块依赖**: expense-ai 依赖 expense-bill + expense-category（读数据），不建表不写数据
- **AI 调用失败不阻断业务**：所有 AI 功能优雅降级，LLM 不可用时基础记账操作不受影响
- **Prompt 设计原则**: 结构化输出（要求返回 JSON）、限定范围（只从已有分类中选择）、Temperature 0.3

### 5.12 文档同步规范

> 详见 [development-standards.md §4.1](docs/development-standards.md)

- **每次完成一个大的 Plan/Phase 后必须同步更新所有相关文档**，不能只改代码不改文档
- **项目结构变更（模块改名、新增/删除模块、目录调整）必须同步更新以下所有文件中的引用**：
  - `CLAUDE.md` — §4 项目目录结构
  - `README.md` — 项目结构
  - `docs/architecture-design.md` — 模块结构 + 依赖关系图
  - `docs/design/v1-design-doc.md` — 多模块依赖设计
  - `docs/development-plan.md` — 所有涉及该模块名称的地方
- `docs/development-plan.md` 中各 Sprint 的状态标记必须与实际情况一致
- 文档一致性检查清单：改模块名 → `grep` 全局搜索旧名称 → 逐文件替换 → 确认无遗漏

### 5.13 文件导出规范

> 详见 [development-standards.md §2.7](docs/development-standards.md)

- **Excel 导出优先使用 EasyExcel**，禁止直接使用 Apache POI（代码量 5-10 倍、易 OOM）
- **输出必须美化**：表头深蓝背景白字粗体 + 全单元格细线边框 + 内容自动换行 + 列宽自适应（带呼吸间距，最小宽度 14-16 字符）
- **多 Sheet 使用单一 `ExcelWriter`** + 多个 `WriteSheet` + `finish()`，严禁对同一 OutputStream 多次调用 `EasyExcel.write()`（会写入两个 ZIP，文件损坏）
- **CSV 必须写入 UTF-8 BOM**，否则 Excel 打开中文乱码

### 重要规则

- 所有需求变更 → 更新 `docs/project-requirements.md`
- 所有架构变更 → 更新 `docs/architecture-design.md`
- 每次迭代完成 → 追加 `docs/iteration-log.md`
- 每次大 Plan 完成 → 同步所有相关文档的状态和引用（见 §5.9）
- 项目结构变更 → 全局搜索旧名称，逐文档更新（见 §5.9）
- 需求/架构文档保持精简，只保留最新状态
- 历史决策和变更原因记录在迭代日志中
- 遇到问题不许退缩，追根究底解决；如需降级版本必须先获批准
- Maven 优先去阿里云镜像搜索最新版本；编译输出乱码立即修复（见 §4.12）

### 退出会话检查清单（/exit 前强制执行）

每次 `/exit` 前必须逐项确认：

```
☐ 代码已 git commit，提交信息符合规范（type(module): message）
☐ 所有修改过的文档已保存并 git add：
   CLAUDE.md / README.md / project-requirements / architecture-design /
   development-standards / development-plan / iteration-log /
   docs/design/v1~v3-design-doc
☐ 本次迭代已写入 docs/iteration-log.md
☐ docs/development-plan.md 中 Sprint 状态与实际情况一致
☐ 文档交叉引用一致性检查：
   ☐ 新文档（v2/v3-design-doc / development-standards）是否已被其他文档引用
   ☐ 旧引用（v1-design-doc.md §4.x）是否已更新为 development-standards.md 对应章节
   ☐ 架构/需求/README 中的小节编号是否与父章节一致
   ☐ 文件名/路径引用链接是否有效
☐ projects/.../memory/ 下的进展状态文件已更新
☐ projects/.../MEMORY.md 索引已包含本次进展的指针
☐ 下次继续的任务和上下文已明确记录（重启命令、环境变量、当前验收进度）
☐ 本 Session 中新授权的「无需再问」命令已写入全局权限配置（~/.claude/settings.json）
   — 包括但不限于：PowerShell(*) / mcp__* / WebSearch / WebFetch
   — 用泛模式（如 mvn *），不要复制整条具体命令
```

**未完成以上清单不得 /exit。**
