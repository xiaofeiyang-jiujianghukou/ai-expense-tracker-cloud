# 开发设计规范 — AI Expense Tracker

> **持续演进**：每个版本都可以向本文档追加新规范，不做版本冻结。
> 版本专属的设计目标、效果、思路参见 `docs/design/` 下的独立版本文档（v1/v2/v3-design-doc.md），验收后冻结。

---

# 一、架构设计原则

> 以下原则源自 V1.0 设计，适用于所有后续版本。新增模块/功能时必须遵循。

## 1.1 核心理念：企业级标准 + 简单优先 + 为扩展预留

| 维度 | 原则 | 说明 |
|------|------|------|
| **分层** | Controller → Manager → Service → Mapper | Manager 编排多 Service，Service 保持原子 |
| **ORM** | MyBatis-Plus | SQL 完全可控，禁止 JPA |
| **模块** | Maven 多模块，单体部署 | 模块间不直接依赖，微服务化只拆 POM + 启动类 |
| **前端** | Vue 3 + Element Plus | Composition API，组件化 |
| **认证** | JWT 无状态 | 可扩展角色/权限体系 |
| **命名** | 避开 Java/Spring 关键字 | 账单用 Bill/BillType，不用 Transaction（与 @Transactional 冲突） |

## 1.2 后端分层与模块结构

**每个业务模块的统一目录结构**：
```
├── controller/    → @RestController, @Validated（只做参数接收和委托，严禁业务逻辑）
├── manager/       → 编排多个 Service（具体类，需要时才创建）
├── service/       → 单一原子业务（具体类）
├── mapper/        → extends BaseMapper<T>（MyBatis-Plus）
├── entity/        → @Data, @TableName
└── dto/           → @Data, request + response
```

**调用链路**：
```
单 Service 场景：  Controller → Service → Mapper → DB
多 Service 编排：  Controller → Manager → Service → Mapper → DB
```

**Manager 规则**：
- ✅ 编排 2+ 个 Service 时才需要 Manager
- ❌ 单 Service 调用时 Controller 直调 Service，不需要 Manager
- ❌ 禁止写只做透传的 Manager（Controller → Manager → Service）
- 命名：`{Domain}Manager`

**Service/Manager 均为具体类**，不强行定义 interface + impl。需多态时再提取接口。

**写操作返回值**：
- insert/update/delete 返回 void，Controller 返回 `ApiResponse<Void>`
- 特例：注册/登录需返回 ID/Token；乐观锁需返回 affected rows

**HTTP 方法限制**：
- 仅使用 `@GetMapping`（单参）+ `@PostMapping`（多参/含 Body）
- 禁止 `@PutMapping` / `@DeleteMapping`

**Controller 职责边界（强制执行）**：
- ✅ 参数接收、JSR-303 校验、调用 Service/Manager、返回结果
- ❌ 任何业务逻辑（文件生成/拼接、数据转换、算法计算、第三方库调用）

## 1.3 多模块依赖设计

```
expense-server                         ← 启动模块 + 跨模块 Manager
    ↓ 依赖
expense-user / expense-category / expense-bill / expense-statistics / expense-ai / expense-budget
    ↓ 依赖
expense-security                       ← JWT + Security Config
    ↓ 依赖
expense-common                         ← 公共工具、异常、枚举
    ↓ 依赖
Spring Boot / MyBatis-Plus / Lombok / MySQL Driver / ...
```

**关键决策**：业务模块之间**不直接相互依赖**。跨模块编排通过 expense-server 的 Manager 实现。拆分微服务时，Manager 对 Service 的本地调用改为 Feign 远程调用，业务代码不变。

## 1.4 前端设计原则

```
页面层级：
  AppLayout（侧边栏导航 + 顶部栏）
    └── 各 View 页面

交互原则：
  - 每个操作有反馈：成功 Toast / 失败提示
  - 删除操作有二次确认
  - 表格数据为空时有空状态占位
  - 请求中按钮 Loading 防重复提交
```

---

# 二、编码规范

## 2.1 后端命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| Entity | 表名驼峰 + @TableName | `User`, `Bill`, `Category` |
| Mapper | Entity名 + Mapper | `UserMapper extends BaseMapper<User>` |
| Service | 模块 + Service | `UserService`（具体类） |
| Manager | 模块 + Manager | `UserManager`（具体类） |
| Controller | 模块 + Controller | `UserController` |
| DTO | 用途 + Request/Response/VO | `RegisterRequest`, `LoginResponse` |
| Maven 模块 | expense-xxx | `expense-user`, `expense-common` |

## 2.2 前端命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | XxxView.vue | `LoginView.vue`, `DashboardView.vue` |
| 业务组件 | 功能名.vue | `BillForm.vue`, `AmountCard.vue` |
| API 模块 | 资源名.ts | `user.ts`, `bill.ts` |
| Store | 资源名.ts | `user.ts`, `app.ts` |
| 路由路径 | kebab-case | `/bills`, `/monthly-stats` |

## 2.3 异常处理规范

```
throw new BusinessException(ErrorCode.USER_NOT_FOUND)
    → GlobalExceptionHandler 拦截
    → 返回 { "code": 40401, "message": "用户不存在", "data": null }
```

| 场景 | HTTP 状态码 | 错误码 |
|------|------------|--------|
| 资源不存在 | 404 | 40401~40499 |
| 参数校验失败 | 400 | 40001~40099 |
| 无权访问 | 403 | 40301~40399 |
| 未认证 | 401 | 40101~40199 |
| 服务器错误 | 500 | 50001~50099 |

## 2.4 参数校验规范

**后端（JSR-303）**：
```java
// Controller
@PostMapping("/register")
public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) { ... }

// DTO
@Data
public class RegisterRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6, max = 32) private String password;
    @Size(max = 50) private String nickname;
}

// Service — 业务校验
if (userMapper.selectCount(
    new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
}
```

**前端（Element Plus）**：
```typescript
const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ]
}
```

## 2.5 数据库建表规范

**所有表和字段必须添加 COMMENT**，这是强制规范，不是可选项。

```sql
-- ✅ 正确
CREATE TABLE `user` (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    email   VARCHAR(255) NOT NULL           COMMENT '邮箱',
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

| 规范项 | 要求 |
|--------|------|
| 表注释 | `COMMENT='表用途说明'` |
| 字段注释 | 每个字段加 `COMMENT`，枚举字段注明可选值 |
| 索引命名 | `uk_xxx`（唯一）、`idx_xxx`（普通） |
| 字符集 | 统一 `utf8mb4` + `utf8mb4_unicode_ci` |
| 引擎 | 统一 `InnoDB` |
| 主键 | `BIGINT NOT NULL AUTO_INCREMENT`，不允许业务主键 |
| 时间戳 | `created_time` 用 `DEFAULT CURRENT_TIMESTAMP`，`updated_time` 用 `ON UPDATE CURRENT_TIMESTAMP`，代码不注入时间 |

## 2.6 配置管理规范

**敏感信息永远不硬编码，走环境变量。**

```
项目级配置（硬编码）：数据库名、JWT 秘钥、JWT 过期时间
部署级配置（环境变量）：数据库地址/端口/用户名/密码、第三方 Key/Secret
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/ai_expense_tracker?...
    username: ${EXPENSE_DB_USERNAME}   # ← 强制环境变量，无默认值
    password: ${EXPENSE_DB_PASSWORD}   # ← 强制环境变量，无默认值
```

### 环境变量命名策略

| 类别 | 命名 | 示例 |
|------|------|------|
| 全局共享 | 通用名，无前缀 | `DB_HOST`、`LLM_API_KEY`、`LLM_MODEL` |
| 项目专属 | `EXPENSE_` 前缀 | `EXPENSE_DB_USERNAME`、`EXPENSE_DB_PASSWORD` |

**设置规则**：
1. 先检查系统中是否已存在同名变量
2. 同名同值 → 直接复用
3. 同名异值 → 创建 `EXPENSE_` 前缀专属变量
4. 绝不复用同名变量承载不同值

## 2.7 文件导出规范

| 场景 | 推荐 | 禁止 |
|------|------|------|
| Excel 导出 | **EasyExcel**（`com.alibaba:easyexcel`） | ❌ Apache POI |
| CSV 导出 | Java 标准库（`BufferedWriter`） | — |
| PDF 导出 | 客户端 `window.print()` | — |

**选择 EasyExcel 的理由**：POI 代码量 5-10 倍、易 OOM；EasyExcel 是阿里官方封装，API 简洁。

### 输出美化要求（强制执行）

| 要求 | 实现方式 |
|------|----------|
| 表头深色背景 + 白色粗体 | `FillPatternType.SOLID_FOREGROUND` + `DARK_BLUE` |
| 表头居中 | `HorizontalAlignment.CENTER` |
| 内容自动换行 | `setWrapped(true)` |
| 全单元格细线边框 | `setBorder*(BorderStyle.THIN)` |
| 列宽自适应 + 呼吸间距 | 自定义 `AbstractColumnWidthStyleStrategy`（CJK×2，min 14-16 字符，+3-4 padding，max 38） |
| 中文字体 | `Microsoft YaHei`，表头 12pt / 内容 11pt |
| 多 Sheet | **单一 `ExcelWriter` + 多个 `WriteSheet` + `finish()`** |

> ⚠️ **严禁对同一个 OutputStream 多次调用 `EasyExcel.write()`** — 会写入两个独立 ZIP，文件损坏。

**CSV 导出要求**：必须写入 UTF-8 BOM（`﻿`），否则 Excel 打开中文乱码。

---

# 三、流程规范

## 3.1 Git 提交规范

```
feat(module): what was done
fix(module): what was fixed
refactor(module): what was refactored
test(module): what was tested
docs: documentation
style: UI/layout changes

示例：
  feat(user): add register and login API
  feat(frontend): add login page and auth flow
  fix(bill): fix date range query boundary
```

## 3.2 测试规范

```
后端测试目录：
src/test/java/com/example/expense/
├── user/
│   ├── service/UserServiceTest.java
│   ├── controller/UserControllerTest.java
│   └── UserIntegrationTest.java
└── ...

测试命名：should_Result_When_Condition
  should_ReturnUser_When_ValidCredentials
  should_ThrowException_When_EmailAlreadyExists
```

## 3.3 Maven 编译环境规范

- 父 POM 强制 `project.build.sourceEncoding=UTF-8`
- `.mvn/maven.config` 包含 `-Dfile.encoding=UTF-8`
- **编译输出乱码 → 立刻修复**
- **新依赖查找优先去阿里云镜像仓库搜索最新版本**
- **遇到问题不许退缩**，降级版本必须先获批准

## 3.4 验收流程规范

**核心原则**：
1. 联调验收必须在浏览器中操作每个页面/按钮/交互，**仅 API 模拟 ≠ 联调通过**
2. **增量验证**：修复 BUG 后只验证修复项及其影响范围
3. **方式如实标注**：PowerShell/curl =「API 模拟」，浏览器 =「浏览器」
4. **验收报告含「预期」和「实测」两列，实测填具体数值**

```
验收流程：
编译通过 → API 模拟测试 → 浏览器验收 → 增量回归 → 人工验收
```

**API 测试前置规则**：调用任何 API 前，先查 DTO 确认字段名（`grep "class XxxRequest"`）。

验收报告格式：
```
| # | 测试项 | 方式 | 预期 | 实测 |
|---|--------|------|------|------|
| 1 | POST /api/ai/categorize | API模拟 | code=200 | code=200, 餐饮, conf=1.0 |
```

**浏览器验收**：DevTools 确认零错误，禁止实测列只写"通过"。

## 3.5 联调测试规范

13 项清单：注册→登录→CRUD→统计→Token 过期→未登录拦截→登录回跳。

| 状态码 | 前端处理 |
|--------|----------|
| 200 + code=200 | 正常展示 |
| 200 + code≠200 | `ElMessage.error` 展示 message |
| 401 / 403 | 清 token → 跳 `/login` |
| 500 | `ElMessage.error` 展示 |

前端体验要求：操作有反馈、删除有确认、Loading 防重复、空状态占位、未登录跳转登录页。

---

# 四、文档规范

## 4.1 文档同步规范

**每次实现阶段完成后，同步所有受影响文档。** 项目结构变更时 `grep` 全局搜索旧名称 → 逐文件替换 → 确认零遗漏。

需同步的文件清单：CLAUDE.md / README.md / architecture-design.md / design docs / development-plan.md。

## 4.2 设计文档版本化管理规范

> 新增于 V3.0（2026-07-12）

### 核心原则

1. **每个大版本保留独立设计文档**（`v1/v2/v3-design-doc.md`），放在 `docs/design/` 下
2. **已验收版本的文档冻结**，后续不再修改正文
3. **共享规范独立成文**（本文档），各版本均可追加，持续演进
4. **如果新版改动到旧版设计**，在旧版文档末尾加外联引用，不修改正文：

```markdown
> ⚡ V3.0 已修改此设计：导出功能新增 Excel/PDF，详见 [v3-design-doc.md](v3-design-doc.md)
```

### 文件结构

```
docs/
├── design/
│   ├── v1-design-doc.md        ← 已验收，冻结
│   ├── v2-design-doc.md        ← 已验收，冻结
│   └── v3-design-doc.md        ← 已验收，冻结
├── development-standards.md    ← 共享规范（持续演进）
├── project-requirements.md
├── architecture-design.md
├── development-plan.md
└── iteration-log.md
```

| 禁止 | 正确做法 |
|------|----------|
| 一个设计文档跨版本持续膨胀 | 每版本独立文档，验收后冻结 |
| 在新版本中修改旧版设计文档正文 | 在旧版末尾加外联引用 |
| 不区分版本专属和跨版本共享 | 共享规范集中在 `development-standards.md` |

---

# 五、设计反模式（避坑指南）

| 反模式 | 正确做法 |
|--------|----------|
| Controller 写业务逻辑 | 全放 Manager + Service |
| Entity 直接返给前端 | 用 DTO/VO 隔离 |
| Service 跨模块调用 | Manager 编排 |
| 强行 interface + impl | 默认为具体类，需多态时再提取接口 |
| 前端直接操作 localStorage | 封装在 utils/auth.ts |
| 硬编码 API 地址 | .env + Vite proxy |
| 跨域靠后端临时配置 | 开发用 proxy，生产用 Nginx |
| 业务模块间直接依赖 | 通过 server 层编排 |
| 没有空状态/加载态 | Loading + Empty 组件 |
| 建表不加 COMMENT | 表注释 + 字段注释，强制规范 |
| 敏感信息硬编码 | 走环境变量，无默认值 |
| 写操作返回对象 | 返回 ApiResponse\<Void\> |
| 使用 @PutMapping/@DeleteMapping | 仅 GetMapping + PostMapping |
| 全限定类名代替 import | 用 import |
| 代码改了文档没同步 | 走文档同步检查清单 |
| 遇到问题就退缩放弃 | 追根究底解决，降级需审批 |
| 编译输出有乱码不修 | 强制 UTF-8 |
| 用 curl 测代理就宣称联调完成 | 必须浏览器逐项操作 |
| 魔法值散落各处 | 定义枚举（BillType） |
| 命名与 Java/Spring 生态冲突 | Bill/BillType 而非 Transaction |
| API 模拟标成浏览器测试 | 如实标注方式 |
| 实测列只写"通过" | 填具体数值 |
| 裸用 POI 写 Excel | 用 EasyExcel + 样式策略 |
| Excel 导出无样式 | 深蓝表头 + 边框 + 自动换行 + 自适应列宽 |
| 对同一流两次 EasyExcel.write() | 单一 ExcelWriter + 多 WriteSheet + finish() |
| 一个设计文档跨版本持续膨胀 | 每版本独立文档，验收后冻结 |
| 在新版本中修改旧版设计文档正文 | 在旧版末尾加外联引用 |
