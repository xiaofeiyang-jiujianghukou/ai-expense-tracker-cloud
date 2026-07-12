# V4.0 微服务基础组件对接经验

> 纯经验记录，不列入开发规范。记录踩过的坑和解决方式，供后续项目参考。
> 时间: 2026-07-12

---

## 1. Nacos 2.3.2

### 坑1: 默认密码不能通过环境变量设置

Nacos 官方**不支持** `NACOS_ADMIN_PASSWORD` 之类的环境变量直接预设管理员密码。这是安全设计。

**解决方式**：
- Docker 启动后默认用户 `nacos/nacos` 自动创建
- 通过自定义 entrypoint 挂载 init 脚本自动改密：
```yaml
entrypoint:
  - sh
  - -c
  - "nohup /home/nacos/init.sh & exec bin/docker-startup.sh"
```
- init 脚本用容器自带的 `curl` 调 API 改密码
- 不用额外容器/镜像

### 坑2: JWT 密钥格式要求

`NACOS_AUTH_TOKEN` 必须是 **256-bit (32字节) base64 编码**，否则启动报 `IllegalArgumentException`。

**生成方式**：
```powershell
$bytes = New-Object byte[] 32
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### 坑3: 容器重建后用户丢失

Nacos standalone 模式用内嵌 Derby 存储用户。删除 volume 后用户数据丢失，需重新初始化。

**教训**：Nacos volume 数据与 MySQL volume 一样重要，不要随意 `docker compose down -v`。

### 坑4: 命名空间

- 默认命名空间是 `public`，namespaceId 为空字符串 `""`
- 自定义命名空间用 `customNamespaceId` 创建（如 `expense-dev`）
- 创建后所有服务统一用同一个 namespace
- 在 docker-compose 中 `NACOS_NAMESPACE: ""` 可能导致 YAML 解析问题，用具体的 namespace ID

---

## 2. Nginx 反向代理

### 坑1: server_name 不匹配 → 回退到第一个 server block

当请求的 Host 头匹配不到任何 `server_name` 时，Nginx 会用**第一个 server block**（`www.expense.com`）。导致 `arthas.expense.com` 跳转到前端页面。

**解决方式**：
- 确保每个子域名都有对应的 `server` block
- 可以用 `default_server` 明确默认行为
- 诊断：`curl -H "Host: xxx.expense.com" http://127.0.0.1/`

### 坑2: 代理重启后 DNS 缓存

Nginx 在 `proxy_pass` 中使用域名时，启动时解析一次并缓存。容器重建后 IP 变了，Nginx 仍连旧 IP → 502。

**解决方式**：
- 方法1: 重启 Nginx (`docker restart expense-nginx`)
- 方法2: 用 `resolver` 指令 + `set $backend` 变量强制动态解析
- **本项目的教训**：修改容器后务必 `docker exec expense-nginx nginx -s reload`

### 坑3: Nacos Web UI 路径

Nacos 的 Web 界面在 `/nacos/` 路径下，不是 `/`。直接代理到 `nacos:8848` 访问根路径返回 404。

**解决方式**：Nginx 加 redirect：
```nginx
location = / {
    return 302 /nacos/;
}
```

---

## 3. SkyWalking 10.1.0

### 坑1: OAP 内存需求远超预期

OAP 默认 JVM 参数 `-Xms2G`，容器限制 512M 直接 OOMKilled。

**解决方式**：
- 通过 `JAVA_OPTS: "-Xms256m -Xmx768m"` 覆盖
- 容器限制至少 1G
- **教训**：JVM 容器启动前务必检查默认堆大小 vs 容器限制，否则容器静默被杀

### 坑2: OAP 启动极慢（5-10 分钟）

SkyWalking 10.x 首次启动要创建大量 H2 表（上百个），即使给了 768M 堆也要 5 分钟以上。

**解决方式**：耐心等待，通过健康检查日志确认就绪：
```
Health status: HealthStatus(score=0, details=)
```
`score=0` 不等于没启动，只是没有 Agent 上报数据。关键是 GraphQL 端点能否响应。

### 坑3: UI 必须在 OAP 就绪后启动

UI 启动时会立即尝试连接 OAP。如果 OAP 还没就绪，连接池被 `Connection Refused` 污染，之后即使 OAP 好了也恢复不了。

**解决方式**：
- 先启动 OAP，等 GraphQL 端点返回正常响应（即使是 400 业务错误也算通）
- 再启动/重启 UI
- 或者 UI 加 `restart: always` + 健康检查依赖

### 坑4: SkyWalking 10.x Booster UI 架构

和 9.x 的经典 UI 不同，10.x Booster UI 是 Spring Boot/Armeria 应用，内部代理 `/graphql` 到 OAP。浏览器 JS 调 `http://ui:8080/graphql` → UI 后端代理到 `http://oap:12800/graphql`。

**诊断命令**：
```bash
# 从 UI 容器直连 OAP
wget --post-data='{"query":"..."}' --header='Content-Type: application/json' http://skywalking-oap:12800/graphql
```

### 坑5: 健康分 score=0 不代表挂了

OAP 的 health checker 总是 `score=0` 直到有数据上报。这是正常的。真正的问题是 GraphQL 能否响应。

---

## 4. Arthas 4.0.5

### 坑1: Tunnel Server 没有官方 Docker 镜像

Arthas Tunnel Server 只有 Maven/GitHub 发布的 fat jar，没有 Docker 镜像。

**解决方式**：自己包 Dockerfile，4 行搞定：
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY arthas-tunnel-server.jar /app/app.jar
EXPOSE 7777 8080
ENTRYPOINT ["java", "-jar", "-Darthas.enable-detail-pages=true", "/app/app.jar"]
```

### 坑2: Nginx 502 (Connection Refused)

Arthas 容器重建后 IP 变了，Nginx DNS 缓存指向旧 IP → 502。

**解决方式**：重建 Arthas 后重启 Nginx。

### 坑3: Tunnel Server Web 控制台默认无认证

Tunnel Server 的 Web 页面默认开放，没有登录保护。虽然有 Spring Security 但只保护 `/actuator` 端点，不保护主页面。

**解决方式**：在 Nginx 层加 basic auth：
```nginx
auth_basic "Arthas Tunnel";
auth_basic_user_file /etc/nginx/.htpasswd;
```

`SPRING_SECURITY_USER_NAME` 和 `SPRING_SECURITY_USER_PASSWORD` 环境变量只影响 `/actuator/arthas` 的 API 认证，不影响 Web 页面。

### 坑4: Agent 需要 arthas-spring-boot-starter

Web Console 只是一个壳，需要各微服务引入 `arthas-spring-boot-starter` + 配置 `arthas.tunnel-server` 地址，Agent 才会注册到 Tunnel。

**注意**：starter 和 tunnel-server 版本要保持一致（都用 4.0.5）。

---

## 5. Docker Compose 通用

### 坑1: ENTRYPOINT vs CMD 覆盖

Docker Compose 的 `command:` 只是传给 ENTRYPOINT 的参数。Nacos 镜像的 ENTRYPOINT 是 `docker-startup.sh`，`command:` 的内容被当参数传入但被忽略。

**解决方式**：用 `entrypoint:` 覆盖整个入口。

### 坑2: deploy.resources 只对 Swarm 生效？

`deploy.resources.limits` 在 `docker compose`（非 Swarm 模式）中**生效**，但 `deploy.resources.reservations` 不生效。

### 坑3: 端口冲突排查

多个容器争同一宿主机端口（8719 问题）。**原则**：基础设施组件（Dashboard）用默认端口不动，微服务绕开。

### 坑4: YAML 重复 key 静默覆盖

docker-compose.yml 中出现重复的 `depends_on:`，不报错但只有最后一个生效。用 `docker compose config` 验证。

---

## 6. JVM 容器最小内存经验

| 应用类型 | 推荐堆 | 推荐容器限制 | 说明 |
|----------|--------|-------------|------|
| Spring Boot MVC (简单 CRUD) | 128-256M | 256-320M | 当前 256M 偏紧（87-98%） |
| Spring Cloud Gateway (Netty) | 64-128M | 256M | 反应式，内存效率高 |
| Nacos 2.x | 128-384M | 512M | JVM + gRPC |
| SkyWalking OAP | 256-768M | 1G+ | 建议开发环境跳过 |
| MySQL 8.0 | N/A | 512M | 数据量大会增长 |
| Redis 7 Alpine | N/A | 128M | 加 `--maxmemory 100mb` |

**铁律**：`-Xmx` 必须 **小于** 容器 memory limit，留出 Metaspace + 堆外内存的空间（约 20-30%）。

---

## 7. 私有镜像仓库对接

### 规则

`crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/<name>:<tag>`

### 命名转换

| Docker Hub 原名 | 私有仓库名 |
|-----------------|-----------|
| `mysql:8.0` | `.../mysql:8.0` |
| `redis:7-alpine` | `.../redis:7-alpine` |
| `nacos/nacos-server:v2.3.2` | `.../nacos-server:v2.3.2` |
| `bladex/sentinel-dashboard:1.8.8` | `.../sentinel-dashboard:1.8.8` |
| `prom/prometheus:v3.3.0` | `.../prometheus:v3.3.0` |
| `grafana/grafana:11.6.0` | `.../grafana:11.6.0` |
| `eclipse-temurin:21-jdk-alpine` | `.../eclipse-temurin:21-jdk-alpine` |
| `eclipse-temurin:21-jre-alpine` | `.../eclipse-temurin:21-jre-alpine` |
| `maven:3.9-eclipse-temurin-21-alpine` | `.../maven:3.9-eclipse-temurin-21-alpine` |

**规则**：`org/name:tag` → 去掉 org 前缀，只保留 name:tag。

---

## 8. 快速排障清单

```bash
# 端口冲突
Get-NetTCPConnection -LocalPort <port>

# 容器无法启动
docker logs <container> --tail 50

# Nacos 在线服务数
curl http://127.0.0.1:8848/nacos/v1/ns/service/list?groupName=expense-cloud

# Nginx DNS 缓存 → 重启
docker restart expense-nginx

# JVM 是否 OOM
docker inspect <container> --format '{{.State.OOMKilled}}'

# Docker Compose 配置是否有语法错误
docker compose config --quiet
```
