# 迭代日志 — AI Expense Tracker

> 记录项目需求、架构设计、开发计划的每一次变更。
> 关联文档: [project-requirements.md](project-requirements.md) | [architecture-design.md](architecture-design.md) | [development-plan.md](development-plan.md)

---

## 迭代记录

### #016 — 2026-07-13 | Sprint 20: Framework Starter 拆分为 3 独立 Starter

**类型**: 架构重构

**内容**:
- 将 `expense-framework`（17 个类，大而全单模块）拆为 3 个按需引入的 starter：
  - `expense-starter-web`：Web + Security + Feign + JWT + Nacos + Sentinel + Actuator（全部应用服务）
  - `expense-starter-orm`：MyBatis-Plus + DataSource + Flyway（有 DB 的服务，依赖 web）
  - `expense-starter-redis`：Redis 配置（仅 AI，依赖 web）
- 修改 12 个服务 POM：`expense-framework` → 按需 starter，删除手动声明的 DB/Redis 依赖
- FrameworkAutoConfiguration 去掉 `@Import`，各 starter 通过 `AutoConfiguration.imports` 独立注册
- Flyway 属性从 web 移至 orm starter（`framework-mybatis-defaults.properties`）
- DataSource 从编程式创建改为属性注入（`spring.datasource.*`），避免与 Boot 的 DataSourceAutoConfiguration 冲突
- `@AutoConfigureBefore(DataSourceAutoConfiguration.class)` 确保属性优先注入
- 更新 Dockerfile.base-builder：3 层 starter 按依赖顺序安装（web → orm → redis）
- API/Common 模块移除多余 `expense-framework` 依赖（零 import 使用）
- 浏览器验收：全部 6 页面零错误，7/7 Nacos 注册，12/12 容器运行
- 新增 favicon.svg 消除 404 错误

**影响模块**: expense-framework (删除), expense-starter-web/orm/redis (新增), 12 个服务 POM, Dockerfile.base-builder

### #015 — 2026-07-12 | V4.0 微服务架构设计 + Sprint 17 基础设施搭建

**类型**: 架构设计 + 实现

**内容**:
- V4.0 微服务架构设计评审通过：5 个微服务 + Gateway + 2 个共享 JAR（expense-common / expense-api）
- 关键技术选型：Spring Cloud 2024.0.0 + Alibaba 2023.0.1.2、Nacos 2.3.2、Sentinel 1.8.8、Prometheus + Grafana
- 认证方案：网关统一 JWT 鉴权（Gateway 解析 JWT → 注入 X-User-Id → 下游信任 header）
- 上下文传播：XUserFilter（expense-common）读 header → 设 SecurityContextHolder → 现有代码零改动
- 修复父 POM artifactId/version 不一致（expense→expense-cloud, 1.0.0→4.0.0）— 9 个子模块
- 新建 expense-api 模块：3 个 Feign 接口（CategoryClient/BillClient/StatisticsClient）+ 5 个共享 DTO + UserContextFeignInterceptor
- 新建 expense-gateway 模块：Spring Cloud Gateway + JwtValidationGlobalFilter + 路由配置 + CORS + Sentinel
- expense-common 新增 XUserFilter（@Component, OncePerRequestFilter）
- 5 个服务模块独立化：各添加 XxxApplication.java + application.yml + 更新 POM
- 各服务分配 Flyway 迁移脚本（user→V1, category→V2, bill→V3~V6）
- docker-compose.yml：11 个容器（MySQL/Redis/Nacos/Sentinel/Prometheus/Grafana/Gateway/5 services）
- 6 个多阶段 Dockerfile（eclipse-temurin:21-jdk-alpine → jre-alpine）
- Prometheus 采集配置 + Grafana 仪表盘 JSON（9 个面板：健康/QPS/P99/错误率/CPU/内存/DB连接/GC/线程）
- Phase 1 策略：共享 DB + 保留跨模块依赖（Feign 迁移在 Sprint 18）
- expense-budget 合并入 bill-service，expense-server 标记为待移除

**文档版本**:
- architecture-design.md: 新增 §10 V4.0 微服务架构 + AD-14~AD-21
- development-plan.md: 新增 Sprint 17-19
- CLAUDE.md: 同步更新 V4.0
- iteration-log.md: 追加 #015

### #016 — 2026-07-12 | V4.0 设计稿补写

**类型**: 文档

**内容**:
- 补写 `docs/design/v4-design-doc.md`（V4.0 微服务工程化改造设计稿）
- 涵盖：12 容器架构拓扑、三模块拆分模式、认证架构重构、Gateway 动态路由、资源管控、可观测性、全部技术决策、部署架构、踩坑精选
- CLAUDE.md §3 关键文档索引新增 V4 设计稿
- architecture-design.md §10 新增 V4 设计稿引用

**文档版本**:
- v4-design-doc.md: 新建
- CLAUDE.md: §3 索引新增
- architecture-design.md: §10 新增引用

### #017 — 2026-07-13 | 全接口浏览器验收 + 7 项 BUG 修复 + 目录重构

**类型**: 验收 + 修复 + 重构

**验收结果**（7 模块 6 页面全部通过）:
- 用户 / 分类 / 账单 / 预算 / 统计 / AI → API 验收 100% 通过
- 仪表盘 / 账单管理 / 分类管理 / 月度统计 / 趋势分析 / 预算管理 → 浏览器验收通过
- AI 消费洞察 SSE 流式输出正常工作（餐饮占比 63%、交通 37%、结余率 99%）

**BUG 修复**:
1. AI SSE 500 — `AiController` SSE 方法中 `ttlExecutor` 后台线程丢失 `SecurityContext` + `RequestContextHolder`，导致 Feign 调用无法传播 `X-User-Id`
2. 账单 Total 始终 0 — MyBatis-Plus 3.5.9 `PaginationInnerInterceptor` 移至 `mybatis-plus-jsqlparser`，新增 `MyBatisPlusConfig` 到 `expense-framework`
3. CSV 导出收入显示为支出 — `BillService.exportCsv()` 比较 `"收入"` vs 实际值 `"INCOME"`
4. 日期选择器不支持键盘输入 — Element Plus `<el-date-picker>` 缺少 `editable` 属性
5. 删除取消 Unhandled error — `ElMessageBox.confirm` reject 未 `try/catch`
6. nacos-init.sh token 失效 — 改密码后重新获取 token 再上传路由
7. 导出文件 UUID 文件名/打不开 — 后端 `Content-Disposition: attachment` 与前端 blob 下载冲突

**目录重构**:
- `expense-category/expense-bill/expense-statistics` 三服务的 `-api/-common/-application` 子模块收入各自父目录
- `docker-compose.yml` 服务名统一为 `expense-*`（对齐 Nacos 注册名）
- 7 个 Dockerfile + 父 POM + 全部 `pom.xml relativePath` 同步更新

**新增组件**:
- `ApiResponseDecoder` — Feign 通用解码器，自动解包 `ApiResponse.data`，兼容裸数据响应
- `gateway-routes.yaml` — Gateway 路由配置文件，Nacos 重建时自动初始化

**文档版本**:
- CLAUDE.md: §4 目录结构更新, §5.7 构建规范新增, §5.8-§5.13 重编号
- architecture-design.md: **完全重写** — 以 V4.0 微服务为主线，移除 V1~V3 单体旧内容
- development-plan.md: Sprint 17-19 标记完成，Sprint 20 调整为 Starter 拆分
- iteration-log.md: 追加 #017

---

### #011 — 2026-07-11 | V2.0 浏览器验收准备 + MCP 配置 + API 测试规范

**类型**: 规范 + 工具

**内容**:
- V2.0 后端 API 验收全部通过（8/8）：categorize/analysis/report/stream/auth/cache
- 前端代理测试通过（9/9）：通过 Vite proxy 验证所有 API 端点
- `docs/design/v1-design-doc.md` 新增 API 测试前置规则：调用任何 API 前必须先查 DTO 确认字段名
- 配置 Playwright MCP 服务器（`.mcp.json` + `@playwright/mcp@latest`，24 tools）
- `frontend/` 安装 playwright 作为 devDependency
- 项目 `.claude/settings.json` 新增权限缓存（read-only PowerShell 命令 + WebSearch/WebFetch）
- 浏览器 UI 验收清单已确认，待 Playwright MCP 驱动完成

**待完成**:
- 在加载了 Playwright MCP 的新会话中完成 8 项浏览器 UI 验收
- expense-ai 模块零测试类，需补 Controller 集成测试

**文档版本**:
- v1-design-doc.md: 1.6 → 1.7（API 测试 DTO 预查规则）
- iteration-log.md: 追加

---

### #010 — 2026-07-11 | AgentScope 2.0 集成 + Redis 缓存 + TTL + 验收

**类型**: 架构升级 + 验收

**内容**:
- 引入 AgentScope Java 2.0.0-RC5（`agentscope-openai-spring-boot-starter` + `agentscope-harness`）
- 删除手动 `AiConfig`/`RestTemplate`，改用 AgentScope 自动装配的 `Model` Bean + `HarnessAgent`
- Redis 缓存 AI 分析和报告结果（analysis TTL 1h, report TTL 6h），缓存命中延迟 < 50ms
- 阿里 TTL（`transmittable-thread-local`）/SSE 流式报告
- Spring Security 适配 SSE（`requireExplicitSave(false)` + `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL`）
- 环境变量重命名策略：全局共享（`DB_HOST`/`LLM_API_KEY`）vs 项目专属（`EXPENSE_` 前缀）
- Maven 编译规范：强制 UTF-8、个人仓库优先、不退缩原则
- 验收流程规范写入 v1-design-doc.md §4.10
- 编译输出乱码修复：父 POM `project.build.sourceEncoding=UTF-8` + `.mvn/maven.config`

**文档版本**:
- v1-design-doc.md: 1.5 → 1.6
- CLAUDE.md: 同步更新
- iteration-log.md: 追加

---

### #009 — 2026-07-11 | V2.0 AI 智能模块完成

**类型**: 实现

**内容**:
- 新建 expense-ai Maven 模块（依赖 expense-bill + expense-category，不建表不写数据）
- 对接 DeepSeek API（deepseek-v4-pro，OpenAI 兼容格式，RestTemplate 直调）
- AI 自动分类（/api/ai/categorize）：填写描述后 AI 从已有分类中建议最匹配的分类
- AI 消费洞察（/api/ai/analysis）：本月收支+分类数据 → 3-5 条个性化洞察
- AI 财务报告（/api/ai/report）：月度财务摘要报告，支持与上月对比
- 前端集成：BillList 防抖 800ms 自动触发 AI 分类建议、Dashboard AI 洞察卡片、MonthlyStats 一键生成报告
- LLM 配置完全走环境变量（DEEPSEEK_API_KEY + AI_LLM_BASE_URL + AI_LLM_MODEL），支持随时切换模型
- AI 调用失败优雅降级，不阻断基础记账功能
- 全量文档同步：requirements 新增 AI-01~03，architecture 新增 expense-ai + AD-12/13，development-plan 新增 Sprint 10-12，README/CLAUDE.md 同步更新

**文档版本**:
- project-requirements.md: 1.3 → 1.4
- architecture-design.md: 2.2 → 2.3
- development-plan.md: 2.0 → 2.1
- CLAUDE.md: 同步更新
- README.md: 同步更新

---

### #008 — 2026-07-11 | 文档同步修复 + 文档同步规范

**类型**: 规范 + 修复

**变更原因**: 发现多个文档与代码实际状态不一致：development-plan 中 Sprint 1-5 仍标"待开始"、模块名 expense-transaction 已改为 expense-bill 但文档未同步、API 路径未同步。根本原因是缺少文档同步强制规范。

**内容**:
- development-plan.md: Sprint 1-5 状态从 ⬜ → ✅ 已完成
- README.md: expense-transaction → expense-bill + API 路径 /api/transactions → /api/bills
- CLAUDE.md: expense-transaction → expense-bill
- architecture-design.md: expense-transaction → expense-bill（模块结构 + 依赖关系图）
- v1-design-doc.md: expense-transaction → expense-bill + 新增 §4.11 文档同步规范
- CLAUDE.md: 新增 §5.8 文档同步规范 + §6 重要规则中补充文档同步条目
- 设计反模式新增：代码改了文档没同步

**文档版本**:
- v1-design-doc.md: 1.4 → 1.5
- CLAUDE.md: 同步更新
- development-plan.md: 状态更新

---

### #007 — 2026-07-11 | 前端全部完成 + 接口验证 + 代码 Review

**类型**: 实现 + Review

**内容**:
- Sprint 6: Vue 3 前端骨架（Vite + Element Plus + Router + Pinia + Axios）
- Sprint 7: 仪表盘、账单管理、分类管理页面
- Sprint 8: 月度统计页面
- Sprint 9: 代码规范检查、安全审查、README
- 后端 13 个接口全部验证通过
- 修复: TransactionManager Bean 名冲突、Flyway checksum 不匹配、createdTime null
- 新增规范: 仅 GetMapping(单参) + PostMapping、写操作返回 void
- 新增规范: 禁止全限定类名内联、时间戳由 DB 管理

**文档版本**:
- development-plan.md: 全部 Sprint 标记完成
- README.md: 新建

---

### #006 — 2026-07-11 | 开发规范沉淀：建表注释 + 配置管理

**类型**: 规范

**变更原因**: Sprint 1-4 开发过程中发现缺失强制规范：建表无注释、敏感信息硬编码风险。

**内容**:
- v1-design-doc.md 新增 §4.8 数据库建表规范（表/字段 COMMENT 强制、索引命名、字符集引擎统一）
- v1-design-doc.md 新增 §4.9 配置管理规范（项目级 vs 部署级分层、敏感信息走环境变量无默认值）
- CLAUDE.md 新增 §5.4、§5.5 引用规范
- project-requirements.md 新增 NFR-15、NFR-16
- 反模式新增：建表不加 COMMENT、敏感信息硬编码

**文档版本**:
- v1-design-doc.md: 1.3 → 1.4
- project-requirements.md: 1.3 → 1.4
- CLAUDE.md: 同步更新

---

### #005 — 2026-07-11 | 去掉 interface + impl 鸡肋拆分

**类型**: 设计规范

**变更原因**: Service 和 Manager 默认只有一个实现，强行定义 interface + impl 增加无意义的样板代码。只在需要多态（策略模式、多实现）时才提取接口。

**内容**:
- Service 和 Manager 目录去掉 `impl/` 子目录，类即为具体实现
- 命名：`UserService`（不是 `UserService` + `UserServiceImpl`），`UserManager`（不是 `UserManager` + `UserManagerImpl`）
- 代码示例中 `@Manager` 改为 `@Component`（Spring 无 @Manager 注解）
- 新增反模式："强行 interface + impl — 只有一个实现时纯属鸡肋"
- 更新 CLAUDE.md、architecture-design.md、v1-design-doc.md 的所有相关描述

**文档版本**:
- architecture-design.md: 2.1 → 2.2（去掉 impl/ 目录结构 + AD-11）
- v1-design-doc.md: 1.2 → 1.3（命名规范精简）
- CLAUDE.md: 同步更新

---

### #004 — 2026-07-11 | 引入 Manager 编排层

**类型**: 架构设计

**变更原因**: Controller 和 Service 之间缺少编排层，Service 承担编排职责容易越界调用、职责不清。增加 Manager 层使分层更符合国内企业级标准。

**内容**:
- **新增 Manager 层**：位于 Controller 和 Service 之间
  - Service：专注单一原子业务，不跨模块调用
  - Manager：编排多个 Service，处理复合业务逻辑，控制事务
  - Controller：只做参数校验和路由，委托 Manager 处理
- **Manager 位置策略**：模块内编排放模块 `manager/`，跨模块编排放 expense-server 的 `manager/`
- **模块目录结构更新**：每个业务模块新增 `manager/` 及 `manager/impl/`
- **更新文档**：
  - CLAUDE.md：重写分层职责表，新增 Manager vs Service 边界示例
  - architecture-design.md：新增第 3 节"分层架构与 Manager 层"，含代码示例和位置策略
  - v1-design-doc.md：更新调用链路、模块结构、命名规范、反模式
  - project-requirements.md：NFR-06 分层描述更新

**文档版本**:
- project-requirements.md: 1.2 → 1.3（NFR-06 更新）
- architecture-design.md: 2.0 → 2.1（新增 Manager 层章节 + AD-10）
- v1-design-doc.md: 1.1 → 1.2（Manager 层设计）
- development-plan.md: 2.0（未变更）

---

### #003 — 2026-07-11 | 架构重大补充：前端 + 企业级技术栈 + 多模块设计

**类型**: 架构设计

**变更原因**: 架构遗漏前端交互层，技术栈未对齐国内企业标准，缺少微服务拆分预备设计。

**内容**:
- **新增前端架构**：Vue 3 + Vite + Element Plus + Axios + Pinia + Vue Router
  - 6 个页面：登录、注册、仪表盘、账单管理、分类管理、月度统计
  - 前后端分离，开发环境 Vite proxy 联调，生产 Nginx
  - 前端路由守卫 + Axios 拦截器自动附加 Token
- **ORM 替换**：Spring Data JPA → MyBatis-Plus 3.5+
- **新增 Lombok**：减少 Entity/DTO 样板代码
- **Maven 多模块设计**：
  - backend/ 下 7 个子模块：common、security、user、category、transaction、statistics、server
  - 业务模块间禁止直接依赖，通过 server 层编排
  - V4 微服务拆分时只改 POM 和启动类，业务代码不变
- **项目结构调整**：design/ 移至 docs/design/
- **全面更新文档**：
  - CLAUDE.md：新增前端技术栈、多模块目录结构、前端分层规范
  - project-requirements.md：新增 FE-01~06 前端需求、NFR-11~14
  - architecture-design.md：完全重写，新增前端架构、多模块设计、微服务拆分路径
  - v1-design-doc.md：新增前端页面效果描述、前端设计原则、前端校验规范
  - development-plan.md：扩展为 10 个 Sprint（后端 5 + 前端 4），~71h

**文档版本**:
- project-requirements.md: 1.1 → 1.2（新增 FE 需求 + NFR-11~14）
- architecture-design.md: 1.0 → 2.0（重写：前端架构 + 多模块 + MyBatis-Plus）
- v1-design-doc.md: 1.0 → 1.1（新增前端设计 + 规范更新）
- development-plan.md: 1.0 → 2.0（Sprint 0→5 扩展为 0→9）

---

### #002 — 2026-07-11 | 创建设计稿和开发设计规范

**类型**: 设计

**内容**:
- 创建 `docs/design/` 设计稿目录
- 创建 `docs/design/v1-design-doc.md` V1.0 设计稿（原在 design/，后移至 docs/design/）
- 更新 `project-requirements.md`：新增 NFR-07 ~ NFR-10 设计规范需求
- 更新 `CLAUDE.md`：新增设计稿索引

---

### #001 — 2026-07-11 | 项目初始化

**类型**: 初始化

**内容**:
- 创建项目目录结构
- 编写原始设计文档 `docs/project-design.md`
- 创建 `CLAUDE.md`、需求文档、架构文档、开发计划、迭代日志

---

### #014 — 2026-07-12 | V3.0 收官：导出打磨 + Excel 美化 + 文档架构重组

**类型**: 修复 + 优化 + 文档重构

**内容**:

**数据导出打磨**：
- **修复 Excel 文件损坏**：两次 `EasyExcel.write()` 写同一 OutputStream → 改用单一 `ExcelWriter` + 多 `WriteSheet` + `finish()`
- **Excel 美化输出**：深蓝表头白字粗体 + 全单元格细线边框 + 内容自动换行 + CJK 感知自适应列宽带呼吸间距（`PaddedColumnWidth` 继承 `AbstractColumnWidthStyleStrategy`）
- **EasyExcel 4.0.3 API 确认**：通过 `javap` 反编译确认方法签名，修正 `WriteCellData<?>` vs `CellData` 类型错误
- **PDF 弹窗优化**：删除自动 `window.print()`，改为弹窗内「打印 / 保存 PDF」按钮 + 操作提示，打印时按钮自动隐藏

**其他修复**：
- **Excel commons-compress 版本冲突**：TestContainers 1.24.0 ← 父 POM dependencyManagement 强制 1.25.0
- **troubleshooting.md**：新增 §0 JDK 21 编译 + commons-compress 冲突

**文档架构重组**：
- **v1-design-doc.md 拆分**：§4 全部规范 + §5 反模式 → 独立 `docs/development-standards.md`
- **新增 `docs/design/v2-design-doc.md`** 和 **`docs/design/v3-design-doc.md`**（各自冻结 + 外联引用）
- **v1-design-doc.md 瘦身**：779 行 → 254 行，保留 V1 专属内容，末尾外联 V2/V3 改动
- **development-standards.md 排序**：架构原则 → 编码规范 → 流程规范 → 文档规范 → 反模式
- **新增 §4.2 设计文档版本化管理规范**：版本文档验收后冻结、新版改动只加外联、不修改旧版正文
- **CLAUDE.md**：全部章节引用同步为新编号 + 新增 v2/v3 设计稿索引

**规范新增**：
- §2.7 文件导出规范（EasyExcel 优先 + 美化强制 + CSV BOM）
- §4.2 设计文档版本化管理规范

**文档版本**:
- development-standards.md: 新建（架构原则 + 15 节规范 + 反模式）
- v1-design-doc.md: V1 专属，验收冻结
- v2-design-doc.md: 新建，V2 专属
- v3-design-doc.md: 新建，V3 专属
- development-plan.md: V3.0 Sprint 13-16 全部完成
- iteration-log.md: 追加
- troubleshooting.md: 新增 §0 JDK 21 + commons-compress
- CLAUDE.md: 同步更新

---

### #013 — 2026-07-11 | V3.0 Sprint 1-2: 趋势分析 + ECharts + 预算模块 + SecurityUtil 重构

**类型**: 实现 + 重构

**内容**:
- V3 Sprint 1: 趋势 API（trend/daily）+ ECharts 三组件 + TrendAnalysis 页面
- BillType 枚举替代所有 INCOME/EXPENSE 魔法值
- transaction→bill 全面清理（DB表/列/实体/前端/文档）
- 预算模块（expense-budget + V6 migration + CRUD API）
- AI 预算建议（BudgetAdviceService：近3月分类×月份统计 + 有效月均值 + 趋势 + 推算全月）
- AI 异常检测（/ai/anomaly）
- SecurityUtil 统一获取用户ID，全部 Controller 去掉 Authentication 参数
- 前端 BudgetManage.vue + 路由
- Markdown 渲染修复（SSE 新行丢失 + marked 库）
- 编码修复：logback-spring.xml 强制 UTF-8
- 验收规范：增量回归原则写入设计文档
- 命名规范：避开 Java/Spring 通用关键字

**文档版本**:
- v1-design-doc.md: 新增枚举规范 + 命名冲突 + 增量回归规则
- CLAUDE.md: 新增 §5.8 枚举规范 + §6 工作习惯规范
- iteration-log.md: 追加

---

### #012 — 2026-07-11 | V2.0 浏览器验收完成 + SSE 流式优化 + Markdown 渲染

**类型**: 验收 + 修复 + 优化

**内容**:
- 9 项浏览器 UI 验收全部通过（Playwright MCP 驱动）
- **修复 AI 洞察 SSE 重复**：后端每 chunk 重发全量文本 → 改为只发增量（`sentLength` 追踪），前端累积拆分
- **修复 SSE 换行丢失**：Spring SseEmitter 拆分多行 `data:` 时前后端均未补 `\n`，markdown 解析失效 → 前后端均加 `prevWasData` 补换行
- **修复 AI 分类 24.8s 延迟**：`LlmClient.chat()` 非流式 `.call().block()` → 改用流式内部累积；`CategorizeResponse` 缺 `@NoArgsConstructor` 导致缓存 JSON 反序列化失败
- **新增 categorize Redis 缓存**：TTL 10min，命中 26ms（~300x 加速）
- **新增 Markdown 渲染**：安装 `marked`，`v-html` 替代 `{{ report }}`，完整元素 CSS（table/h2/blockquote/ul/ol）
- **结构化报告 Prompt**：显式要求 `## 总体评估 | ## 收支对比 | table | ## 分类观察 | ## 理财建议`
- **卡片常驻**：仪表盘 AI 洞察 + 统计页 AI 报告卡片始终可见，无数据时显示转圈占位
- **修复流式最后一行延迟**：后端不等 `\n` 直接逐 chunk 转发；前端累积拆分，最后不完整行作为 `typingHint` 实时显示
- **修复 AgentScope 12s 清理延迟**：`.takeUntil(AGENT_END)` 一刀切断内部清理，总耗时 17.3s → 4.8s
- **耗时分析**：通过事件级日志定位到 DeepSeek thinking 2.4s + AgentScope 内部清理 12s

**文档版本**:
- iteration-log.md: 追加

---

## 文档版本追踪

| 文档 | 当前版本 | 最后更新 | 变更次数 |
|------|----------|----------|----------|
| project-requirements.md | 1.4 | 2026-07-11 | 4 |
| architecture-design.md | 2.3 | 2026-07-11 | 4 |
| development-plan.md | 3.0 | 2026-07-12 | 3 |
| development-standards.md | 1.0 | 2026-07-12 | 新建 |
| v1-design-doc.md | 2.0 | 2026-07-12 | 拆分冻结 |
| v2-design-doc.md | 1.0 | 2026-07-12 | 新建 |
| v3-design-doc.md | 1.0 | 2026-07-12 | 新建 |
