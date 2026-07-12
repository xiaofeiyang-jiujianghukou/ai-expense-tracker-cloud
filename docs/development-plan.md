# 开发计划 — AI Expense Tracker

> 按 Sprint 迭代执行。每个 Sprint 完成后更新状态。
> 关联文档: [project-requirements.md](project-requirements.md) | [architecture-design.md](architecture-design.md)

---

## Sprint 0：项目初始化

**状态**: ✅ 已完成

| 任务 | 描述 | 状态 |
|------|------|------|
| T001 | 创建项目目录结构 | ✅ |
| T002 | 编写 docs/project-design.md（原始设计） | ✅ |
| T003 | 创建 CLAUDE.md | ✅ |
| T004 | 创建 docs/project-requirements.md | ✅ |
| T005 | 创建 docs/architecture-design.md | ✅ |
| T006 | 创建 docs/development-plan.md | ✅ |
| T007 | 创建 docs/iteration-log.md | ✅ |
| T008 | 创建 docs/design/v1-design-doc.md | ✅ |
| T009 | 创建 .gitignore + 初始化 Git 仓库 | ✅ |
| T010 | 关联远程 + 首次推送 | ✅ |

---

## Sprint 1：项目骨架（后端）

**状态**: ✅ 已完成

**目标**: Maven 多模块父 POM + expense-common + expense-server 可启动 + 数据库连通

| 任务 | 描述 | 关联 |
|------|------|------|
| T101 | 创建 Maven 多模块父 POM（版本管理 + 模块聚合） | NFR-11 |
| T102 | 创建 expense-common 模块（ApiResponse、BusinessException、ErrorCode、工具类） | NFR-04,08 |
| T103 | 创建 expense-security 模块（JwtTokenProvider、JwtAuthFilter、SecurityConfig） | SEC-01 |
| T104 | 创建 expense-server 启动模块（application.yml、入口类） | — |
| T105 | 配置多环境（dev/test）数据库连接 + Flyway | NFR-05 |
| T106 | 全局异常处理器 | NFR-08 |
| T107 | 基础包结构（user/category/transaction/statistics 空壳） | — |
| T108 | 验证项目可启动、数据库可连接、Flyway 可执行 | — |

**产出物**:
- 7 个 Maven 模块：common、security、user、category、transaction、statistics、server
- `mvn spring-boot:run` 启动成功
- 统一响应 + 全局异常 + JWT 基础设施就位

---

## Sprint 2：用户模块（后端）

**状态**: ✅ 已完成

**目标**: 注册 + 登录 + JWT 认证链路打通

| 任务 | 描述 | 关联 |
|------|------|------|
| T201 | User Entity + Mapper | USR-01 |
| T202 | UserService（注册 + BCrypt 加密） | USR-01,03 |
| T203 | UserController（register + login 接口） | USR-01,02 |
| T204 | JwtTokenProvider 实现（生成/验证 Token、7天过期） | SEC-01,02 |
| T205 | SecurityConfig 配置（放行 /register /login，拦截其他） | SEC-01 |
| T206 | Flyway V1 迁移脚本 | NFR-05 |
| T207 | UserService 单元测试 | NFR-01 |
| T208 | UserController MockMvc 测试 | NFR-02 |
| T209 | 用户集成测试（TestContainers） | NFR-03 |

**产出物**:
- `POST /api/users/register` 注册成功
- `POST /api/users/login` 返回 JWT
- 所有后续 API 需 Token

---

## Sprint 3：分类模块（后端）

**状态**: ✅ 已完成

**目标**: 分类 CRUD + 新用户注册自动创建默认分类

| 任务 | 描述 | 关联 |
|------|------|------|
| T301 | Category Entity + Mapper | CAT-01 |
| T302 | CategoryService（CRUD + 默认分类初始化） | CAT-01,02,03 |
| T303 | CategoryController | CAT-01,02 |
| T304 | 注册时自动创建默认分类（在 UserService 中调用） | CAT-03 |
| T305 | Flyway V2 迁移脚本 | NFR-05 |
| T306 | CategoryService 单元测试 | NFR-01 |
| T307 | CategoryController MockMvc 测试 | NFR-02 |
| T308 | 分类集成测试 | NFR-03 |

**产出物**:
- 分类 CRUD API 完成
- 新用户自动拥有 8 个默认分类

---

## Sprint 4：账单模块（后端核心）

**状态**: ✅ 已完成

**目标**: 账单 CRUD + 多条件筛选分页 + 归属校验

| 任务 | 描述 | 关联 |
|------|------|------|
| T401 | Transaction Entity + Mapper（自定义查询 + 分页） | TXN-01,02 |
| T402 | TransactionService（CRUD + 筛选 + 归属校验） | TXN-01~05 |
| T403 | TransactionController | TXN-01~04 |
| T404 | Flyway V3 迁移脚本 | NFR-05 |
| T405 | TransactionService 单元测试 | NFR-01 |
| T406 | TransactionController MockMvc 测试 | NFR-02 |
| T407 | 账单集成测试 | NFR-03 |

**产出物**:
- 完整账单 CRUD API
- 支持 type / categoryId / startDate / endDate 筛选 + 分页

---

## Sprint 5：统计模块（后端）

**状态**: ✅ 已完成

**目标**: 月度汇总 + 分类统计

| 任务 | 描述 | 关联 |
|------|------|------|
| T501 | StatisticsService（月度汇总 + 分类支出统计） | STAT-01,02 |
| T502 | StatisticsController | STAT-01,02 |
| T503 | StatisticsService 单元测试 | NFR-01 |
| T504 | StatisticsController MockMvc 测试 | NFR-02 |
| T505 | 统计集成测试 | NFR-03 |

**产出物**:
- 月度收入/支出/结余查询
- 分类支出金额汇总

---

## Sprint 6：前端项目骨架

**状态**: ✅ 已完成

**目标**: Vue 3 项目初始化 + 路由 + 布局 + Axios 封装

| 任务 | 描述 | 关联 |
|------|------|------|
| T601 | `npm create vue@latest` 创建 Vite + Vue 3 项目 | NFR-12 |
| T602 | 安装依赖：Element Plus、Axios、Pinia、Vue Router | NFR-12 |
| T603 | Axios 实例封装（baseURL、请求/响应拦截器、Token 自动附加） | NFR-12,14 |
| T604 | Pinia user store（Token 存储、登录/登出） | SEC-01 |
| T605 | Vue Router 配置（路由表 + 导航守卫） | FE-01 |
| T606 | AppLayout 布局组件（侧边栏导航 + 顶部栏 + 内容区） | — |
| T607 | Vite proxy 配置（/api → localhost:8080） | NFR-14 |
| T608 | 登录页 + 注册页（纯前端，调用后端 API） | FE-01,02 |

**产出物**:
- 前端项目可启动，登录/注册可调通后端
- 路由守卫生效，未登录自动跳转

---

## Sprint 7：前端核心页面

**状态**: ✅ 已完成

**目标**: 仪表盘 + 账单管理 + 分类管理三个页面

| 任务 | 描述 | 关联 |
|------|------|------|
| T701 | DashboardView：3 个金额卡片 + 最近 10 条账单列表 | FE-03 |
| T702 | TransactionList：筛选栏 + 表格 + 分页 + 新增/编辑弹窗 | FE-04 |
| T703 | CategoryManage：收入/支出标签页切换 + 列表 + 新增/编辑行 | FE-05 |
| T704 | 删除二次确认 + 操作 Toast 反馈 + Loading 状态 | NFR-13 |

**产出物**:
- 完整的账单管理闭环（查/增/改/删）
- 分类管理可用
- 仪表盘首页可见本月概览

---

## Sprint 8：前端统计页 + 整体联调

**状态**: ✅ 已完成

**目标**: 月度统计页面 + 前后端全流程打通

| 任务 | 描述 | 关联 |
|------|------|------|
| T801 | MonthlyStats：月份选择 + 汇总卡片 + 分类明细列表 | FE-06 |
| T802 | 前后端全流程联调测试（注册→登录→记账→查看统计） | — |
| T803 | 边界情况处理（空数据、网络异常、Token 过期） | NFR-13 |

**产出物**:
- 所有页面可用
- 全流程走通

---

## Sprint 9：完善与 Review

**状态**: ✅ 已完成

**目标**: 代码质量 + 文档 + 安全检查

| 任务 | 描述 | 状态 |
|------|------|------|
| T901 | 后端代码 Review（规范、分层层级、异常处理） | ✅ |
| T902 | 前端代码 Review（组件结构、API 封装） | ✅ |
| T903 | 安全审查（BCrypt 加密、JWT 过期、归属校验） | ✅ |
| T904 | 性能审查（数据库索引、MyBatis-Plus 分页） | ✅ |
| T905 | README.md 完善（启动说明、技术栈、项目结构） | ✅ |
| T906 | API 文档整理（README.md API 概览） | ✅ |

---

## 工时汇总

| Sprint | 内容 | 预估工时 |
|--------|------|----------|
| Sprint 0 | 项目初始化 | 已完成 |
| Sprint 1 | 后端骨架 | 8h |
| Sprint 2 | 用户模块 | 9h |
| Sprint 3 | 分类模块 | 7h |
| Sprint 4 | 账单模块 | 9h |
| Sprint 5 | 统计模块 | 4h |
| Sprint 6 | 前端骨架 | 8h |
| Sprint 7 | 前端核心页面 | 12h |
| Sprint 8 | 前端统计+联调 | 6h |
| Sprint 9 | 完善 Review | 8h |
| **V2.0 合计** | | **~20h** |
| Sprint 10 | AI 基础设施 + 自动分类（后端） | 6h |
| Sprint 11 | 自动分类前端 + 消费洞察 | 7h |
| Sprint 12 | 财务报告 + 联调 | 7h |
| **总计** | | **~91h** |

---

## 执行策略

```
后端优先:
  Sprint 0 → Sprint 1 → Sprint 2 → Sprint 3 → Sprint 4 → Sprint 5
                                                              ↓
前端对接:                                              Sprint 6 → Sprint 7 → Sprint 8
                                                              ↓
收尾:                                                    Sprint 9
```

后端先用 Postman 自测，API 稳定后再启动前端开发，减少前后端返工。

---

## Sprint 10：AI 基础设施 + 自动分类（后端）

**状态**: ✅ 已完成

**目标**: 创建 expense-ai 模块，对接 DeepSeek API，实现账单自动分类

| 任务 | 描述 | 关联 |
|------|------|------|
| T1001 | 创建 expense-ai Maven 模块（POM + 目录结构 + 父 POM 注册 + server 依赖） | AI-01 |
| T1002 | ErrorCode 新增 AI 错误码（50002-50004） | AI-01 |
| T1003 | AiConfig 配置类（@ConfigurationProperties + RestTemplate Bean） | AI-01 |
| T1004 | LlmClient（RestTemplate 调 DeepSeek Chat Completions API） | AI-01 |
| T1005 | AiCategoryService（读分类 → 构造 prompt → 调 LLM → 解析） | AI-01 |
| T1006 | AiController（/api/ai/categorize 端点） | AI-01 |
| T1007 | application.yml 新增 ai.llm 配置段（base-url、api-key、model） | AI-01 |

**产出物**: `POST /api/ai/categorize` 输入描述+金额 → 返回建议分类+置信度

---

## Sprint 11：自动分类前端 + 消费洞察

**状态**: ✅ 已完成

**目标**: 前端集成 AI 分类建议 + Dashboard AI 消费洞察

| 任务 | 描述 | 关联 |
|------|------|------|
| T1101 | AiAnalysisService（查月度数据 → 构造分析 prompt → 调 LLM） | AI-02 |
| T1102 | AiController 新增 /api/ai/analysis 端点 | AI-02 |
| T1103 | 前端 api/ai.ts API 封装 | AI-01,02 |
| T1104 | BillList 新增/编辑弹窗集成 AI 分类建议（防抖 800ms） | AI-01 |
| T1105 | Dashboard 新增「AI 消费洞察」卡片 | AI-02 |

**产出物**: 记账时 AI 自动建议分类 + 首页 AI 消费洞察

---

## Sprint 12：财务报告 + 联调

**状态**: ✅ 已完成

**目标**: AI 财务报告 + 文档同步

| 任务 | 描述 | 关联 |
|------|------|------|
| T1201 | AiReportService（生成月度财务报告，含上月对比） | AI-03 |
| T1202 | AiController 新增 /api/ai/report 端点 | AI-03 |
| T1203 | MonthlyStats 页面集成「生成 AI 报告」按钮 | AI-03 |
| T1204 | 文档同步（requirements / architecture / development-plan / README / CLAUDE.md） | — |

**产出物**: 统计页一键生成 AI 财务报告

---

## V3.0：可视化增强 + AI 预算 + 数据导出

### Sprint 13：可视化升级 + 趋势分析

**状态**: ✅ 已完成

**目标**: ECharts 图表替换 + 趋势 API + 枚举标准化

| 任务 | 描述 | 状态 |
|------|------|------|
| T1301 | Trend API（N 月收支趋势） | ✅ |
| T1302 | Daily API（31 天日分布） | ✅ |
| T1303 | ECharts 三组件（折线/饼图/柱图）集成 | ✅ |
| T1304 | TrendAnalysis 页面 + 统计页饼图改造 | ✅ |
| T1305 | BillType 枚举（替代 INCOME/EXPENSE 魔法值） | ✅ |
| T1306 | transaction→bill 全量重命名（Entity/字段/前端/文档） | ✅ |

**产出物**: 趋势分析页 + ECharts 可视化 + 枚举标准化

### Sprint 14：预算模块 + AI 预算建议

**状态**: ✅ 已完成

**目标**: 预算 CRUD + AI 智能预算建议 + 异常检测

| 任务 | 描述 | 状态 |
|------|------|------|
| T1401 | expense-budget 模块（JPA Entity + Mapper + V6 Flyway） | ✅ |
| T1402 | Budget CRUD API（增删改查 + 按月查询） | ✅ |
| T1403 | BudgetAdviceService（近3月分类统计 + 趋势推算 + LLM 结构化 JSON） | ✅ |
| T1404 | AI 异常检测（/api/ai/anomaly：超预算预警） | ✅ |
| T1405 | 前端 BudgetManage.vue（预算列表 + AI 建议弹窗） | ✅ |
| T1406 | SecurityUtil 统一获取用户ID（全部 Controller 重构） | ✅ |

**产出物**: 预算管理页 + AI 预算建议 + 异常检测

### Sprint 15：数据导出

**状态**: ✅ 已完成（浏览器验收通过）

**目标**: CSV / Excel / PDF 三格式数据导出

| 任务 | 描述 | 状态 |
|------|------|------|
| T1501 | CSV 导出（POST /api/bills/export-csv → BillService） | ✅ |
| T1502 | Excel 导出（POST /api/statistics/export-excel → EasyExcel） | ✅ |
| T1503 | PDF 导出（客户端 window.print() 方案） | ✅ |
| T1504 | EasyExcel 4.0.3 依赖 + commons-compress 版本冲突修复 | ✅ |
| T1505 | 前端导出按钮 + blob 下载适配（request.ts responseType） | ✅ |
| T1506 | 浏览器验收：CSV 中文不乱码 / Excel 双 Sheet / PDF 排版 | ✅ |

**产出物**: 三格式导出，浏览器验收全部通过

### Sprint 16：文档收尾

**状态**: ✅ 已完成

**目标**: V3.0 全量文档同步

| 任务 | 描述 | 状态 |
|------|------|------|
| T1601 | development-plan.md 新增 V3.0 Sprint 13-16 | ✅ |
| T1602 | iteration-log.md 追加 #014 | ✅ |
| T1603 | CLAUDE.md 同步更新 | ✅ |
| T1604 | troubleshooting.md 新增 JDK 21 编译 + commons-compress 冲突 | ✅ |
| T1605 | memory 文件状态同步 | ✅ |

**产出物**: 全部文档与代码同步

---

## 工时汇总（更新）

| Sprint | 内容 | 状态 |
|--------|------|------|
| Sprint 0-9 | V1.0 MVP + V2.0 AI 模块 | ✅ |
| Sprint 10-12 | AI 基础设施 + 自动分类 + 财务报告 | ✅ |
| Sprint 13 | 可视化升级 + 趋势分析 | ✅ |
| Sprint 14 | 预算模块 + AI 预算建议 | ✅ |
| Sprint 15 | 数据导出（CSV/Excel/PDF） | ✅ |
| Sprint 16 | 文档收尾 | ✅ |
| **总计** | **V3.0 全部完成** | ✅ |
| Sprint 17 | Docker 容器化 + 基础设施（expense-api/gateway/XUserFilter） | 🟡 进行中 |
| Sprint 18 | Spring Cloud 微服务拆分（Nacos/Feign/Sentinel） | ❌ |
| Sprint 19 | 监控 + 收尾（Prometheus/Grafana） | ❌ |
| Sprint 20 | CI/CD（GitHub Actions） | ❌ |
| Sprint 21 | 云部署 | ❌ |
| Sprint 22 | 文档收尾 | ❌ |
| **V4.0 总计** | **微服务工程化改造** | 🟡 |

---

## Sprint 17：Docker 容器化 + V4.0 基础设施

**状态**: 🟡 进行中

**目标**: 修复 POM 不一致、创建 expense-api 和 expense-gateway 模块、XUserFilter 上下文传递、各服务独立化、Docker 化

| 任务 | 描述 | 状态 |
|------|------|------|
| T1701 | 修复父 POM artifactId/version 不一致（expense→expense-cloud, 1.0.0→4.0.0） | ✅ |
| T1702 | 创建 expense-api 模块（Feign 接口 + DTO + RequestInterceptor） | ✅ |
| T1703 | 创建 expense-gateway 模块（Spring Cloud Gateway + JWT 校验 + 路由） | ✅ |
| T1704 | expense-common 新增 XUserFilter（X-User-Id → SecurityContextHolder） | ✅ |
| T1705 | 各服务模块添加启动类 + application.yml + 更新 POM | ✅ |
| T1706 | 编写 docker-compose.yml（11 个容器） | ✅ |
| T1707 | 编写各服务多阶段 Dockerfile（6 个） | ✅ |
| T1708 | 编写 Prometheus + Grafana 配置 | ✅ |
| T1709 | 验证 Maven 编译通过 | ✅ |
| T1710 | 文档同步（architecture/development-plan/iteration-log/CLAUDE.md） | 🟡 |

**产出物**:
- 5 个可独立启动的服务模块 + 1 个 Gateway
- docker-compose.yml 编排 (11 容器: 5 infra + 6 app + 2 monitor)
- 上下文传递链路: Gateway JWT → X-User-Id → XUserFilter → SecurityUtil

---

## Sprint 18：微服务拆分（Spring Cloud）

**状态**: ❌ 未开始

**目标**: Nacos 注册/配置中心接入、Feign 替换跨模块直接调用、Sentinel 限流

| 任务 | 描述 | 状态 |
|------|------|------|
| T1801 | 各服务接入 Nacos 注册中心（启动验证服务注册） | ❌ |
| T1802 | 各服务接入 Nacos 配置中心（迁移 DB/Redis/JWT/LLM 配置） | ❌ |
| T1803 | Feign 替换：user-service → category-service | ❌ |
| T1804 | Feign 替换：bill-service → category-service | ❌ |
| T1805 | Feign 替换：statistics-service → bill-service + category-service | ❌ |
| T1806 | Feign 替换：ai-service → statistics-service | ❌ |
| T1807 | Sentinel 限流规则配置 + Dashboard 集成 | ❌ |
| T1808 | docker-compose 全链路联调 | ❌ |

---

## Sprint 19：监控 + 收尾

**状态**: ❌ 未开始

**目标**: Prometheus + Grafana 部署、仪表盘、全流程验证

| 任务 | 描述 | 状态 |
|------|------|------|
| T1901 | Prometheus 部署 + 采集验证 | ❌ |
| T1902 | Grafana 部署 + Dashboard 导入 | ❌ |
| T1903 | 验证 CPU/内存/QPS/P99/GC 指标 | ❌ |
| T1904 | 全链路浏览器验收（注册→登录→记账→AI→统计） | ❌ |
| T1905 | 文档收尾 | ❌ |
