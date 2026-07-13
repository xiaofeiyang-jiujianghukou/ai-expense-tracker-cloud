# Plan: Framework Starter 拆分（懒加载·3 模块）

## Context

`expense-framework` 当前是一个大而全的共享模块（17 个 Java 类 + 全部依赖），所有应用服务被迫依赖它。但实际使用情况是：
- Gateway 完全不依赖 framework（它走 reactive 栈）
- Category/Bill/Budget/Statistics 不需要 Redis/JWT 生成
- AI 不需要 MyBatis
- 只有 User 需要 PasswordEncoder

**目标**：按关注点拆成 3 个懒加载 starter，各服务按需引入。每个 starter 内部的 Bean 通过 `@ConditionalOnClass` / `@ConditionalOnProperty` 条件装配——classpath 上有就激活，没有就跳过。

**关键约束**：Gateway 不参与（reactive 栈与 Spring MVC 不兼容），保持现有独立声明。

---

## 拆分方案

```
expense-starter-web  ← 全部应用服务 (user/category/bill/budget/statistics/ai)
       ↑
expense-starter-orm  ← 有 DB 的服务 (user/category/bill/budget/statistics)
       ↑
expense-starter-redis ← 只有 AI
```

| Starter | 依赖 | 内容 |
|---------|------|------|
| **web** | spring-boot-starter-web, security, openfeign, loadbalancer, actuator, prometheus, nacos-discovery, nacos-config, sentinel, jjwt | ApiResponse, 异常体系, SecurityFilterChain, XUserFilter, SecurityUtil, JwtTokenProvider, Feign 解码器/拦截器, BillType 枚举 |
| **orm** | web + mybatis-plus-boot3-starter, jsqlparser, mysql-connector-j, flyway-core, flyway-mysql | MyBatisPlusConfig, FrameworkDataSourceAutoConfiguration, FrameworkMyBatisAutoConfiguration, Flyway 默认配置 |
| **redis** | web + spring-boot-starter-data-redis | FrameworkRedisAutoConfiguration |

**每个 starter 自带 `AutoConfiguration.imports`，只注册自己模块内的自动配置类。** 废弃 `FrameworkAutoConfiguration` 的 `@Import` 机制——不存在跨模块 import。

---

## 文件变化清单

### 新建模块

#### 1. `backend/expense-starter-web/`
```
expense-starter-web/
├── pom.xml
└── src/main/
    ├── java/com/xiaofeiyang/expense/framework/
    │   ├── ApiResponse.java                                  ← move
    │   ├── autoconfigure/
    │   │   ├── FrameworkAutoConfiguration.java               ← move, 简化(去掉@Import)
    │   │   ├── FrameworkSecurityAutoConfiguration.java        ← move
    │   │   ├── FrameworkFeignAutoConfiguration.java           ← move
    │   │   └── FrameworkWebAutoConfiguration.java             ← move
    │   ├── config/
    │   │   ├── ApiResponseDecoder.java                        ← move
    │   │   ├── UserContextFeignInterceptor.java                ← move
    │   │   ├── FeignErrorDecoder.java                         ← move
    │   │   └── JwtTokenProvider.java                          ← move
    │   ├── enums/
    │   │   └── BillType.java                                  ← move
    │   ├── exception/
    │   │   ├── BusinessException.java                         ← move
    │   │   ├── ErrorCode.java                                 ← move
    │   │   ├── FeignCallException.java                        ← move
    │   │   └── GlobalExceptionHandler.java                    ← move
    │   ├── filter/
    │   │   └── XUserFilter.java                               ← move
    │   └── util/
    │       └── SecurityUtil.java                              ← move
    └── resources/
        ├── framework-defaults.properties                      ← move, 精简
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**AutoConfiguration.imports** (expense-starter-web):
```
com.xiaofeiyang.expense.framework.autoconfigure.FrameworkAutoConfiguration
```

**FrameworkAutoConfiguration** (简化后):
```java
@Configuration
@ComponentScan(basePackages = "com.xiaofeiyang.expense.framework")
@PropertySource("classpath:framework-defaults.properties")
// 不再 @Import — 各 starter 通过自己的 AutoConfiguration.imports 注册
public class FrameworkAutoConfiguration {}
```

**framework-defaults.properties** (精简后，去掉 Flyway 行):
```
spring.cloud.nacos.config.import-check.enabled=false
spring.flyway.out-of-order=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
feign.client.config.default.loggerLevel=full
logging.level.com.xiaofeiyang.expense=DEBUG
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}
management.metrics.export.prometheus.enabled=true
```

#### 2. `backend/expense-starter-orm/`
```
expense-starter-orm/
├── pom.xml
└── src/main/
    ├── java/com/xiaofeiyang/expense/framework/
    │   ├── autoconfigure/
    │   │   ├── FrameworkMyBatisAutoConfiguration.java         ← move
    │   │   └── FrameworkDataSourceAutoConfiguration.java       ← move
    │   └── config/
    │       └── MyBatisPlusConfig.java                         ← move
    └── resources/
        ├── framework-mybatis-defaults.properties               ← move
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**AutoConfiguration.imports** (expense-starter-orm):
```
com.xiaofeiyang.expense.framework.autoconfigure.FrameworkMyBatisAutoConfiguration
com.xiaofeiyang.expense.framework.autoconfigure.FrameworkDataSourceAutoConfiguration
```

#### 3. `backend/expense-starter-redis/`
```
expense-starter-redis/
├── pom.xml
└── src/main/
    ├── java/com/xiaofeiyang/expense/framework/
    │   └── autoconfigure/
    │       └── FrameworkRedisAutoConfiguration.java           ← move
    └── resources/
        ├── framework-redis-defaults.properties                 ← move
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

**AutoConfiguration.imports** (expense-starter-redis):
```
com.xiaofeiyang.expense.framework.autoconfigure.FrameworkRedisAutoConfiguration
```

### 修改文件

#### 4. 父 POM `backend/pom.xml`
- `<modules>`: 移除 `expense-framework`，新增 3 个 starter
- `<dependencyManagement>`: 移除 `expense-framework` 条目，新增 `expense-starter-web/orm/redis` 条目

#### 5. 各服务 POM（6 个）
替换 `expense-framework` 为按需的 starter：

| 服务 | 旧依赖 | 新依赖 |
|------|--------|--------|
| **user** | expense-framework + 手动声明 mybatis/mysql/flyway | expense-starter-orm（内带 web + DB） |
| **category-application** | expense-framework + 手动声明 DB | expense-starter-orm |
| **bill-application** | expense-framework + 手动声明 DB | expense-starter-orm |
| **budget** | expense-framework + 手动声明 DB | expense-starter-orm |
| **statistics-application** | expense-framework（可能间接 DB） | expense-starter-orm |
| **ai** | expense-framework + 手动声明 redis/mybatis/mysql | expense-starter-orm + expense-starter-redis（需要 DB + Redis） |

同时在上述 POM 中删除手动重复声明的依赖（如 mybatis/mysql/flyway/redis 等），这些现在由 starter 统一提供。

#### 6. `backend/Dockerfile.base-builder`
```
# 旧：只安装 expense-framework
# 新：按顺序安装 3 个 starter（orm 依赖 web, redis 依赖 web）
COPY expense-starter-web/pom.xml ./expense-starter-web/
COPY expense-starter-web/src ./expense-starter-web/src/
COPY expense-starter-orm/pom.xml ./expense-starter-orm/
COPY expense-starter-orm/src ./expense-starter-orm/src/
COPY expense-starter-redis/pom.xml ./expense-starter-redis/
COPY expense-starter-redis/src ./expense-starter-redis/src/
RUN mvn install -f pom.xml -N -B -q
RUN mvn install -f expense-starter-web/pom.xml -B -q -DskipTests
RUN mvn install -f expense-starter-orm/pom.xml -B -q -DskipTests
RUN mvn install -f expense-starter-redis/pom.xml -B -q -DskipTests
```

#### 7. 所有 7 个 Dockerfile
不需要结构变化——base-builder 已经包含了所有 3 个 starter。但需要确认 path 引用。

### 删除文件

#### 8. `backend/expense-framework/` 整个目录（所有文件已迁移）

---

## 懒加载设计

每个已存在的 `@ConditionalOnClass` / `@ConditionalOnProperty` 保留：

| Bean | 条件 | 效果 |
|------|------|------|
| MyBatisPlusConfig | `@ConditionalOnClass({MybatisPlusInterceptor, PaginationInnerInterceptor})` | classpath 无 MyBatis 不创建 |
| FrameworkMyBatisAutoConfiguration | `@ConditionalOnClass(MybatisPlusAutoConfiguration)` | 同上 |
| FrameworkDataSourceAutoConfiguration | `@ConditionalOnClass(HikariDataSource.class)` | classpath 无 MySQL 不创建 |
| FrameworkRedisAutoConfiguration | `@ConditionalOnClass(RedisTemplate.class)` | classpath 无 Redis 不创建 |
| JwtTokenProvider | `@ConditionalOnProperty("jwt.secret")` | 没配 jwt.secret 不创建 |
| FrameworkFeignAutoConfiguration | `@ConditionalOnClass(RequestInterceptor.class)` | classpath 无 Feign 不创建 |

**也就是说：jar 在 classpath 上只是一个条件，具体 Bean 是否创建由 @ConditionalOnXxx 决定——"引了就加载，不引就跳过"。**

---

## 构建顺序

```powershell
# 1. 重建基础镜像（包含 3 个 starter）
docker build -t expense-base-builder:latest -f backend/Dockerfile.base-builder backend/

# 2. 按依赖顺序逐个构建服务
docker compose build expense-gateway
docker compose build expense-category
docker compose build expense-user
docker compose build expense-bill
docker compose build expense-budget
docker compose build expense-statistics
docker compose build expense-ai

# 3. 全量部署
docker compose up -d
```

---

## 验证清单

1. `mvn compile -f backend/pom.xml` 全部模块编译通过
2. 7 个 Docker 镜像全部构建成功
3. `docker compose up -d` 12 容器全部 running
4. Nacos 7/7 注册
5. API 冒烟：注册→登录→记账→分类→统计→AI 分类
6. Gateway 确认未引入 Spring MVC（不冲突）
7. AI 服务确认 Redis 连接正常，其他服务确认无 Redis 依赖
