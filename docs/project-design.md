很好，这是我们项目真正进入工程阶段的第一步。

按照我们之前确定的工作流：

```
Analyze（分析）
    ↓
Design（设计）
    ↓
Implement（实现）
```

现在处于 **Design 阶段**。

下面这份文档不是简单的功能列表，而是按照真实企业项目设计方式整理的 **AI Expense Tracker V1.0 项目设计文档**。

后续我们会让 Claude Code 阅读这份文档，并基于它创建代码。

---

# AI Expense Tracker 项目设计文档 V1.0

## 1. 项目概述

### 1.1 项目名称

**AI Expense Tracker**

中文名称：

**AI 智能个人财务管理系统**

---

## 1.2 项目背景

现代用户每天产生大量消费记录，但传统记账工具存在：

* 录入繁琐
* 分类困难
* 缺少消费分析
* 无法提供财务建议

本项目旨在开发一个智能个人财务管理系统，通过：

* 简单记账
* 自动分类
* 数据统计
* AI 辅助分析

帮助用户了解自己的消费习惯，提升财务管理能力。

---

# 2. 项目目标

## V1.0 MVP目标

完成一个稳定的个人记账系统。

用户可以：

* 注册账号
* 登录系统
* 创建消费记录
* 创建收入记录
* 管理消费分类
* 查询历史账单
* 查看财务统计

---

# 3. 技术架构设计

## 3.1 总体架构

```
                 Client

                   |
                   |

              REST API

                   |
                   |

          Spring Boot Backend

                   |
        ---------------------

        Controller Layer

                   |

        Service Layer

                   |

        Repository Layer

                   |

              MySQL

```

---

# 4. 技术栈

## 后端

| 技术              | 版本  |
| --------------- | --- |
| Java            | 21  |
| Spring Boot     | 3.x |
| Spring MVC      | 6.x |
| Spring Data JPA | 3.x |
| Spring Security | 6.x |
| Maven           | 3.x |

---

## 数据库

| 技术      | 用途      |
| ------- | ------- |
| MySQL 8 | 主数据库    |
| Flyway  | 数据库版本管理 |

---

## 测试

| 技术             | 用途     |
| -------------- | ------ |
| JUnit5         | 单元测试   |
| Mockito        | Mock测试 |
| TestContainers | 集成测试   |

---

## 开发工具

| 工具            | 用途     |
| ------------- | ------ |
| IntelliJ IDEA | IDE    |
| Claude Code   | AI开发助手 |
| Git           | 版本控制   |

---

# 5. 系统模块设计

整体模块：

```
ai-expense-tracker

├── 用户模块
├── 分类模块
├── 账单模块
├── 统计模块
└── AI模块（未来）
```

---

# 6. 用户模块 User

## 功能

### 注册

用户通过：

* 邮箱
* 密码

创建账户。

### 登录

返回：

JWT Token

---

## 数据模型

### user

| 字段           | 类型       | 说明   |
| ------------ | -------- | ---- |
| id           | bigint   | 主键   |
| email        | varchar  | 邮箱   |
| password     | varchar  | 密码   |
| nickname     | varchar  | 昵称   |
| status       | tinyint  | 状态   |
| created_time | datetime | 创建时间 |
| updated_time | datetime | 更新时间 |

---

# 7. 分类模块 Category

## 功能

用户管理消费分类。

例如：

收入：

```
工资
奖金
投资
```

支出：

```
餐饮
交通
购物
娱乐
住房
```

---

## 数据模型

### category

| 字段           | 类型       |
| ------------ | -------- |
| id           | bigint   |
| user_id      | bigint   |
| name         | varchar  |
| type         | varchar  |
| created_time | datetime |

type:

```
INCOME

EXPENSE
```

---

# 8. 账单模块 Transaction

核心模块。

## 功能

用户可以：

新增：

* 收入
* 支出

查询：

* 日期范围
* 分类
* 类型

修改：

* 金额
* 描述
* 分类

删除：

* 删除账单

---

## 数据模型

### transaction

| 字段               | 类型       | 说明   |
| ---------------- | -------- | ---- |
| id               | bigint   | 主键   |
| user_id          | bigint   | 用户   |
| category_id      | bigint   | 分类   |
| amount           | decimal  | 金额   |
| type             | varchar  | 类型   |
| description      | varchar  | 备注   |
| transaction_date | date     | 交易日期 |
| created_time     | datetime |      |
| updated_time     | datetime |      |

---

# 9. 统计模块 Statistics

## 功能

提供财务统计。

## V1功能

### 月收入

例如：

```
2026-07

收入:

8000

支出:

3500

结余:

4500

```

---

## API

例如：

```
GET

/api/statistics/monthly

```

返回：

```json
{
 "income":8000,
 "expense":3500,
 "balance":4500
}
```

---

# 10. API设计规范

统一：

RESTful

---

## 用户

### 注册

POST

```
/api/users/register
```

### 登录

POST

```
/api/users/login
```

---

## 分类

GET

```
/api/categories
```

POST

```
/api/categories
```

---

## 账单

新增：

POST

```
/api/transactions
```

查询：

GET

```
/api/transactions
```

修改：

PUT

```
/api/transactions/{id}
```

删除：

DELETE

```
/api/transactions/{id}
```

---

# 11. 项目目录设计

Spring Boot：

```
backend

src/main/java


com.example.expense


├── common

├── config

├── security

├── user

│   ├── controller

│   ├── service

│   ├── repository

│   └── entity


├── category

├── transaction

└── statistics

```

---

# 12. 开发规范

## Controller

职责：

* 参数接收
* 参数校验
* 返回结果

禁止：

* 写业务逻辑

---

## Service

职责：

* 核心业务

---

## Repository

职责：

* 数据访问

---

## Entity

职责：

* 数据模型

---

# 13. 安全设计

V1：

使用 JWT。

流程：

```
登录

↓

验证用户名密码

↓

生成JWT

↓

客户端保存

↓

请求携带Token

↓

Spring Security验证

```

---

# 14. 测试策略

## 单元测试

覆盖：

Service层

目标：

覆盖率 > 80%

---

## Controller测试

使用：

MockMvc

---

## 集成测试

使用：

TestContainers

启动真实MySQL。

---

# 15. Git规范

分支：

```
main

develop

feature/*
bugfix/*
```

---

Commit:

格式：

```
type(module): message
```

例如：

```
feat(transaction): add create transaction api

fix(user): fix login token issue

```

---

# 16. Claude Code 工作方式

所有任务遵循：

## Analyze

阅读代码。

输出：

* 当前实现
* 问题
* 修改方案

---

## Design

确认：

* 文件变化
* 数据变化
* API变化

---

## Implement

执行：

* 修改代码
* 编写测试
* 运行测试

---

## Review

检查：

* 代码质量
* 性能
* 安全
* 测试完整性

---

# 17. 后续版本规划

## V1.0

基础记账

完成：

✅ 用户

✅ 分类

✅ 收支记录

✅ 查询统计

---

## V2.0

智能化

增加：

* AI 自动分类
* 消费分析
* 财务报告

---

## V3.0

数据能力

增加：

* Python 数据分析服务
* 推荐模型
* 可视化

---

## V4.0

工程化

增加：

* Docker
* Kubernetes
* CI/CD
* 云部署

---

# 18. 当前 Sprint 规划

## Sprint 0

项目初始化

状态：

进行中

任务：

* 创建 Git
* 创建 README
* 创建 CLAUDE.md
* 创建 docs/project-design.md

---

## Sprint 1

项目骨架

任务：

* 创建 Spring Boot 3 项目
* Maven配置
* 数据库配置
* 基础包结构

---

## Sprint 2

用户模块

---

# 文档结束

---

这份设计文档就是我们后续让 Claude Code "工作的依据"。

下一步我建议不要马上写代码，而是进入真正的工程流程：

**Sprint 0 - Task 1：把这份设计文档放入项目 `docs/project-design.md`，然后让 Claude Code 阅读它，生成第一版 `CLAUDE.md`。**

这样我们开始训练 Claude Code "理解项目"，而不是让它直接写代码。
