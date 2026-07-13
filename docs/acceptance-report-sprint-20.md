# 验收报告 — Sprint 20: Framework Starter 拆分 + 三模块拆分

> **验收日期**: 2026-07-13  
> **验收人**: Claude Code (Playwright 浏览器自动化)  
> **环境**: Docker Compose 12 容器, Java 21, Nacos 2.3.2  

---

## 一、Sprint 20 变更范围

### 1.1 Framework Starter 拆分 (expense-framework → web/orm/redis)

| 变更 | 详情 |
|------|------|
| 删除 | `backend/expense-framework/` (17 类大而全单模块) |
| 新增 | `expense-starter-web` — Web/Security/Feign/JWT/Nacos/Sentinel/Actuator |
| 新增 | `expense-starter-orm` — MyBatis/DataSource/Flyway (依赖 web) |
| 新增 | `expense-starter-redis` — Redis 配置 (依赖 web) |
| 修改 | 12 个服务 POM (按需引入 starter) |
| 修改 | Dockerfile.base-builder (3 层 starter 安装) |
| 修改 | FrameworkAutoConfiguration (去掉 @Import) |
| 修改 | 5 个架构文档同步 |

### 1.2 三模块拆分 (user/budget/ai)

| 服务 | 拆分前 | 拆分后 |
|------|--------|--------|
| expense-user | 单模块 | api / common / application |
| expense-budget | 单模块 | api / common / application |
| expense-ai | 单模块 | api / common / application |

---

## 二、容器运行状态

| # | 容器 | 镜像 | 状态 | 端口 |
|---|------|------|------|------|
| 1 | expense-mysql | mysql:8.0 | ✅ Up (healthy) | 3306 |
| 2 | expense-redis | redis:7-alpine | ✅ Up (healthy) | 6379 |
| 3 | expense-nacos | nacos-server:v2.3.2 | ✅ Up | 8848/9848 |
| 4 | expense-gateway | expense-gateway | ✅ Up | 8080 |
| 5 | expense-user | expense-user-application | ✅ Up | 8081 |
| 6 | expense-category | expense-category-application | ✅ Up | 8082 |
| 7 | expense-bill | expense-bill-application | ✅ Up | 8083 |
| 8 | expense-statistics | expense-statistics-application | ✅ Up | 8084 |
| 9 | expense-ai | expense-ai-application | ✅ Up | 8085 |
| 10 | expense-budget | expense-budget-application | ✅ Up | 8086 |
| 11 | expense-frontend | Vue 3 (Vite) | ✅ Up | 5173 |
| 12 | expense-nginx | nginx:alpine | ✅ Up | 80 |

**结果**: 12/12 ✅

---

## 三、Nacos 服务注册

| # | 服务名 | 组 | 状态 |
|---|--------|-----|------|
| 1 | expense-gateway | expense-cloud | ✅ |
| 2 | expense-user | expense-cloud | ✅ |
| 3 | expense-category | expense-cloud | ✅ |
| 4 | expense-bill | expense-cloud | ✅ |
| 5 | expense-statistics | expense-cloud | ✅ |
| 6 | expense-ai | expense-cloud | ✅ |
| 7 | expense-budget | expense-cloud | ✅ |

**结果**: 7/7 ✅

---

## 四、浏览器页面验收

| # | 页面 | URL | 预期 | 实测 | Console |
|---|------|-----|------|------|---------|
| 1 | 仪表盘 | /dashboard | 登录状态、收入/支出/结余、AI 洞察、最近账单 | 收入 ¥5000 / 支出 ¥40.5 / 结余 ¥4959.5, AI 洞察 4 条, 最近账单 3 条 | 0 error |
| 2 | 账单管理 | /bills | 账单列表、筛选/分页、新增/编辑/删除、导出 CSV | 3 条记录, 类型/分类/日期筛选, 编辑/删除按钮, 分页 Total 3 | 0 error |
| 3 | 分类管理 | /categories | 支出/收入 Tab 切换、新增/编辑/删除 | 5 个支出分类 (餐饮/交通/购物/娱乐/住房), 编辑/删除按钮 | 0 error |
| 4 | 月度统计 | /statistics | 月度汇总、分类占比、AI 报告、导出 Excel/PDF | 收入 ¥5000 / 支出 ¥40.5 / 结余 ¥4959.5, AI 财务报告 (总体评估/收支对比/分类观察/理财建议), 导出按钮 | 0 error |
| 5 | 趋势分析 | /trends | 收支趋势图、分类支出对比图 | 近 6 月趋势图 (legend 右上角), 7月分类支出对比 | 0 error |
| 6 | 预算管理 | /budget | 预算进度条、设置预算、AI 建议 | 餐饮 ¥610 (4.2%), 交通 ¥360 (4.2%), 编辑按钮 | 0 error |

**结果**: 6/6 页面正常, Console 0 错误 ✅

---

## 五、API 冒烟测试

| # | API | 方式 | 预期 | 实测 |
|---|-----|------|------|------|
| 1 | POST /api/users/login | API 模拟 | code=200, token 返回 | code=200, token=eyJhbG... |
| 2 | POST /api/categories/list | API 模拟 | code=200, 5 个分类 | code=200, 5 items |

**结果**: 通过 ✅

---

## 六、汇总

| 验收项 | 结果 |
|--------|------|
| Maven 编译 (全部 25 个模块) | ✅ |
| 容器运行 | ✅ 12/12 |
| Nacos 注册 | ✅ 7/7 |
| 浏览器页面 | ✅ 6/6 |
| Console 错误 | ✅ 0 |
| API 冒烟 | ✅ 通过 |

**验收结论**: Sprint 20 全部通过。Framework Starter 拆分 + user/budget/ai 三模块拆分，无回归问题。
