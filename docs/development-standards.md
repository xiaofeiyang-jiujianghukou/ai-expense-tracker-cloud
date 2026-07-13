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

## 1.5 微服务模块拆分规范（V4.0+）

> **优先级：高**。此规范是 V4.0 微服务架构的核心设计原则，适用于所有微服务间调用场景。

### 三模块拆分模型

每个需要对外提供 API 的微服务，必须拆分为三个独立 Maven 模块：

```
expense-category-api/                ← API 模块（轻量 JAR，对外发布）
│   └── pom.xml                       依赖：仅 expense-common + openfeign
│
expense-category-application/        ← 应用模块（Spring Boot 可执行 JAR，只部署不发布）
│   └── pom.xml                       依赖：-api + -common + 全部运行时依赖
│
(expense-category-common/)           ← 业务公共模块（可选，服务内部共享的 Entity/Utils）
```

| 模块 | 后缀 | 职责 | 发布 | 包大小 |
|------|------|------|------|--------|
| **API 模块** | `-api` | Feign 接口 + 对外 DTO | ✅ 发布到 Maven 仓库 | 几 KB（纯接口+POJO） |
| **公共模块** | `-common` | 服务内部共享的 Entity/Utils/常量（可选） | ✅ 发布（如其他服务需要） | 小 |
| **应用模块** | `-application` | Spring Boot 启动类 + Controller/Service/Mapper + 配置 | ❌ 只部署，不发布 | 几十 MB（含全部运行时依赖） |

### 为什么拆？

| 问题 | 不拆的后果 | 拆分后 |
|------|-----------|--------|
| **消费者引入过多依赖** | 依赖完整服务 JAR → 拉入 Spring Boot/MyBatis/MySQL 驱动等几十 MB 不必要依赖 | 消费者只依赖 `-api`，仅拉入 Feign 接口 + POJO |
| **版本耦合** | 服务内部重构（不改 API），消费者被迫升级 | 内部变更只发 `-application`，消费者无感知 |
| **编译速度** | 大 JAR 传递依赖拖慢消费方编译 | `-api` 几乎无传递依赖，编译秒级 |
| **全局 API 模块** | 所有 Feign 接口集中一个模块，改一个影响全部 | 各服务独立 `-api`，边界清晰 |

### 反模式（禁止）

```
❌ expense-api/  ← 全局共享 API 模块，所有 Feign 接口 + DTO 集中于此
   ├── CategoryClient.java
   ├── BillClient.java
   └── StatisticsClient.java

❌ expense-category/  ← 单模块服务，API 和实现混在一起
   └── src/.../category/api/client/CategoryClient.java
   └── src/.../category/service/CategoryService.java
```

### 正确做法（三模块：api / common / application）

```
✅ expense-category-api/           ← Feign 接口 + Feign DTO（轻量，< 10KB）
│   └── src/.../category/api/
│       ├── client/CategoryClient.java
│       └── dto/CategoryDTO.java
│
✅ expense-category-common/        ← 共享运行时类（VO、Entity 等被其他模块引用的类）
│   └── src/.../category/dto/
│       ├── CategoryVO.java
│       └── CategoryRequest.java
│
✅ expense-category-application/   ← Spring Boot 应用（只部署不发布为依赖）
│   └── Spring Boot + controller + service + mapper
│   └── 依赖 -api + -common
```

> **强制规则：**
> 1. **每个微服务必须按 api / common / application 三模块拆分**，即使当前暂无消费者。预留结构方便未来复用。
> 2. **任何 `-application` 模块不得被其他模块作为 dependency 引用。** 需要 Feign 调用就引 `-api`，需要共享类就引 `-common`。这从根本上杜绝 fat JAR 嵌套的类加载问题。

### 依赖规则

| 场景 | 依赖 | 说明 |
|------|------|------|
| **Feign 调用** | `expense-{service}-api` | 只引 Feign 接口 + Feign DTO，几 KB |
| **引用共享类** | `expense-{service}-common` | VO、Entity 等，标准 JAR 无嵌套问题 |
| **服务自身** | `-api` + `-common` + full runtime | `-application` 依赖前两个 |
| **禁止** | ❌ 依赖其他服务的 `-application` | 会导致 fat JAR 嵌套，内部类 NoClassDefFoundError |

### 包路径约定

| 内容 | 包路径 | 所在模块 |
|------|--------|---------|
| Feign 接口 | `com.example.expense.{service}.api.client` | `-api` |
| Feign DTO | `com.example.expense.{service}.api.dto` | `-api` |
| 共享 VO / 请求 DTO | `com.example.expense.{service}.dto` | `-common` |
| 应用实现 | `com.example.expense.{service}.controller/service/...` | `-application` |

### @FeignClient 注解规范

**必须包含四个属性：**

```java
@FeignClient(
        name = "category-service",           // Nacos 服务名
        contextId = "category-service-api",  // 唯一上下文 ID（避免 Bean 名冲突）
        path = "/api/categories",            // 基础路径
        url = "${category-service-api.url:}"   // 调试直连 URL（属性名=contextId.url，空=走 Nacos）
)
```

| 属性 | 必填 | 说明 |
|------|------|------|
| `name` | ✅ | Nacos 注册的服务名 |
| `contextId` | ✅ | 唯一标识，命名规范 `{服务名}-api` |
| `path` | ✅ | 匹配 Controller 的 `@RequestMapping` |
| `url` | ✅ | `${服务名.url:}` — **空默认值 = 走 Nacos 服务发现**；本地调试时设为 `http://localhost:808x` 绕过 Nacos 直连 |

**调试用法：** 消费方 application.yml 中设置 `{contextId}.url` 即可绕过 Nacos：
```yaml
# 调试时直连 category-service，无需启动 Nacos
category-service-api.url: http://localhost:8082
```

**多 FeignClient 场景：** 同一服务可以有多个不同 contextId 的 FeignClient，各自独立调试：
```yaml
category-service-api.url: http://localhost:8082        # 主接口
category-service-admin-api.url: http://localhost:9082  # 管理接口
```

### 反例代码

```java
// ❌ 全局共享 — 无服务归属
package com.example.expense.api.client;
```

```java
// ✅ 服务专属 API 模块
package com.example.expense.category.api.client;
```

## 1.6 微服务基础框架 Starter 规范（V4.0+）

> **优先级：高**。基础设施已拆分为 3 个独立 starter — `expense-starter-web`（Web/Security/Feign/JWT/Nacos/Sentinel）、`expense-starter-orm`（MyBatis/DataSource/Flyway，含 web）、`expense-starter-redis`（Redis，含 web）。各服务按需引入。**新增任何公共组件时必须遵守本节规范。**
>
> ⚡ **Sprint 20 变更**：原 `expense-framework` 单模块已拆为 3 starter，下文代码示例中的 `expense-framework` 对应现在的 `expense-starter-web` 或 `expense-starter-orm`（视组件所属而定）。详见 `docs/design/v4-starter-split-plan.md`。

### 1.6.1 组件分类：必要组件 vs 非必要组件

引入新组件时，第一步是判断它属于哪一类：

| 分类 | 定义 | 判断标准 | 加载方式 |
|------|------|---------|---------|
| **必要组件** | 每个微服务都必须使用的 | 网关、user、category、bill、statistics、ai 六个服务**全部需要** | 始终加载，无需条件注解 |
| **非必要组件** | 仅部分服务需要 | 至少存在一个服务**不需要** | `@ConditionalOnClass`，服务 POM opt-in |

**判定流程：**
```
新组件 → 遍历所有微服务 → 全部需要？
  ├── 是 → 必要组件 → 始终加载
  └── 否 → 非必要组件 → @ConditionalOnClass opt-in
```

### 1.6.2 必要组件实现规范

必要组件**不**加 `@ConditionalOnClass`，框架 POM 中**不**标记 `optional`。

**属性文件：** 放在 `framework-defaults.properties`（全局加载，`FrameworkAutoConfiguration` 上通过 `@PropertySource` 引入）。

**Auto-configuration 类：** 不加条件注解，直接通过 `@Import` 或 `@Bean` 注册。

**示例：**
```java
// ✅ 必要组件 — 所有服务都需要 Actuator，无需条件
// framework-defaults.properties:
management.endpoints.web.exposure.include=health,info,prometheus,metrics
```

```
当前必要组件：
├── Web + MVC           (spring-boot-starter-web，非 optional)
├── Security            (XUserFilter + SecurityFilterChain)
├── Feign 拦截器         (UserContextFeignInterceptor，全局 Bean 静默注入，开发者无需感知)
├── Nacos 注册/配置      (spring-cloud-starter-alibaba-nacos-*)
├── Actuator + Prometheus (始终暴露 /actuator/prometheus)
└── 全局异常处理          (GlobalExceptionHandler + ApiResponse)
```

### 1.6.3 非必要组件实现规范

非必要组件必须满足 **"引了就说明需要，没引就是不需要"** 原则——服务不引入该依赖时，启动日志零报错零警告，连一行配置都不需要写。

**实现三步：**

**① 框架 POM 中标记 `optional`：**
```xml
<!-- expense-framework/pom.xml -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <optional>true</optional>   <!-- ← 关键：服务不声明就不会引入 -->
</dependency>
```

**② 独立属性文件（非 `framework-defaults.properties`）：**
```
framework-mybatis-defaults.properties   ← MyBatis 专用
framework-redis-defaults.properties     ← Redis 专用
```
每个非必要组件一个独立 properties 文件，**绝不**放入全局 `framework-defaults.properties`。

**③ Auto-configuration 类加 `@ConditionalOnClass` + `@PropertySource`：**
```java
// ✅ 正确：仅在 MyBatis-Plus 在 classpath 时才激活
@Configuration
@ConditionalOnClass(MybatisPlusAutoConfiguration.class)
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@PropertySource("classpath:framework-mybatis-defaults.properties")
public class FrameworkMyBatisAutoConfiguration {
}
```

```
当前非必要组件：
├── MyBatis-Plus   ← @ConditionalOnClass(MybatisPlusAutoConfiguration)
│                    属性文件: framework-mybatis-defaults.properties
├── MySQL DataSource ← @ConditionalOnClass(HikariDataSource)
│                     从环境变量 DB_HOST/DB_PORT/EXPENSE_DB_USERNAME/EXPENSE_DB_PASSWORD
├── Flyway          ← 无框架配置（Spring Boot 自带的 FlywayAutoConfiguration 处理）
│                    服务 POM 声明 flyway-core 即自动启用
└── Redis           ← @ConditionalOnClass(RedisTemplate)
                     属性文件: framework-redis-defaults.properties
```

### 1.6.4 反模式与正确做法对照

| 场景 | ❌ 反模式 | ✅ 正确 |
|------|---------|--------|
| **全局属性文件** | 把 MyBatis/Flyway/Redis 默认值放进 `framework-defaults.properties` | 非必要组件独立 properties 文件，`@ConditionalOnClass` 隔离加载 |
| **手动开关** | 服务写 `spring.flyway.enabled: false` 来关闭不需要的组件 | 服务不引入 flyway 依赖，压根不加载 |
| **无差别依赖** | 框架 POM 中所有依赖无 `optional` 标记 | 非必要组件标记 `<optional>true</optional>` |
| **全局 API 模块** | 建一个 `expense-api/` 放所有 Feign 接口 | 每个服务独立 `-api` 模块（见 §1.5） |
| **缺少条件注解** | 非必要 AutoConfiguration 不加 `@ConditionalOnClass` | 必加，确保 classpath 无依赖时不报错 |

### 1.6.5 新增组件 checklist

向 `expense-framework` 添加新组件时，逐项确认：

```
☐ 1. 判定分类：遍历全部微服务，确认是必要还是非必要
☐ 2. [必要] 依赖不放 optional / [非必要] 依赖标记 <optional>true</optional>
☐ 3. [必要] 默认值放入 framework-defaults.properties
     [非必要] 新建独立 framework-{name}-defaults.properties
☐ 4. 创建 Framework{Name}AutoConfiguration 类
☐ 5. [非必要] 加 @ConditionalOnClass({核心类}.class)
☐ 6. [非必要] 加 @PropertySource("classpath:framework-{name}-defaults.properties")
☐ 7. 在 FrameworkAutoConfiguration 的 @Import 中注册
☐ 8. 更新本文档 §1.6 的组件清单
☐ 9. 编译验证：无此依赖的服务启动零报错
```

### 1.6.6 属性文件一览

```
expense-framework/src/main/resources/
├── framework-defaults.properties          ← 必要组件（仅 Actuator）
├── framework-mybatis-defaults.properties  ← 非必要（@ConditionalOnClass 隔离加载）
└── framework-redis-defaults.properties    ← 非必要（@ConditionalOnClass 隔离加载）
```

### 1.6.7 Auto-configuration 类清单

| 类 | 条件 | 分类 | 职责 |
|----|------|------|------|
| `FrameworkAutoConfiguration` | 始终 | 入口 | `@ComponentScan` + `@PropertySource` 全局属性 + `@Import` 全部子配置 |
| `FrameworkSecurityAutoConfiguration` | 始终 | 必要 | `SecurityFilterChain` + `XUserFilter` |
| `FrameworkWebAutoConfiguration` | 始终 | 必要 | `GlobalExceptionHandler` |
| `FrameworkFeignAutoConfiguration` | 始终 | 必要 | `UserContextFeignInterceptor` |
| `FrameworkMyBatisAutoConfiguration` | `@ConditionalOnClass` | 非必要 | MyBatis-Plus 默认值 |
| `FrameworkDataSourceAutoConfiguration` | `@ConditionalOnClass` | 非必要 | MySQL DataSource（环境变量） |
| `FrameworkRedisAutoConfiguration` | `@ConditionalOnClass` | 非必要 | Redis 连接（环境变量） |

### 1.6.8 服务 POM 与 application.yml 规范

**服务 POM — 一个框架依赖搞定的写法：**
```xml
<!-- ✅ 必要组件：expense-framework 一个依赖全部覆盖 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>expense-framework</artifactId>
</dependency>

<!-- ✅ 非必要组件：服务显式声明需要的 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>  <!-- 需要 DB -->
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>                <!-- 需要 DB -->
</dependency>

<!-- ✅ 服务特有依赖 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>                        <!-- 仅 statistics + bill -->
</dependency>
```

**服务 application.yml — 最小化写法：**
```yaml
# ✅ 仅保留服务特有配置，框架已覆盖通用部分
server:
  port: 8082
spring:
  application:
    name: category-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: expense-cloud
# 不需要写：datasource / flyway / mybatis-plus / management — 框架 + opt-in 已覆盖
```

### 1.6.9 Jackson ObjectMapper 规范

> **优先级：高**。禁止手动 `new ObjectMapper()`，统一注入 Spring Boot 管理的实例。

Spring Boot 的 `JacksonAutoConfiguration` 已自动配置好：
- `JavaTimeModule` — `LocalDateTime`、`LocalDate` 序列化
- `spring.jackson.date-format` / `spring.jackson.time-zone` 配置绑定
- classpath 上的 `Module` Bean 自动发现

**反模式（禁止）：**

```java
// ❌ 覆盖了 Spring Boot 的完整配置，丢失 JavaTimeModule 等
@Bean
public ObjectMapper objectMapper() { return new ObjectMapper(); }

// ❌ 每次 new 浪费内存，不受全局配置管控
private final ObjectMapper objectMapper = new ObjectMapper();
```

**正确做法：直接注入，按需用 Customizer 定制**

```java
// ✅ 注入 Spring Boot 已配置好的 ObjectMapper
@Service
@RequiredArgsConstructor
public class AiCategoryService {
    private final ObjectMapper objectMapper;
}

// ✅ 如需定制，用 Customizer 而非覆盖整个 Bean
@Bean
public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return builder -> builder
        .timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
}
```

**`Jackson2ObjectMapperBuilderCustomizer` 工作流：**

```
JacksonAutoConfiguration → 创建 Builder → 收集 Customizer Bean
  → 逐个 customize() → build() → 最终 ObjectMapper
  → @Autowired 拿到的就是这个完整配置版
```

声明 Customizer 后无需额外操作，所有 `ObjectMapper` 注入点自动生效。

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
