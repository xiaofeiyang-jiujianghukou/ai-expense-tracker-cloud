# V2.0 设计稿 — AI Expense Tracker

> ✅ **V2.0 已验收，本文档冻结。** 后续版本如需改动 V2 设计，仅在末尾加外联引用，不修改正文。

---

## 1. V2.0 设计目标

在 V1.0 手动记账基础上，引入 AI 能力：自动分类建议、消费洞察分析、月度财务报告。

### 一句话目标

> **用户记账时 AI 自动建议分类，首页展示个性化消费洞察，统计页一键生成财务报告。**

---

## 2. V2.0 新增模块

### 2.1 expense-ai 模块

```
expense-ai/
├── config/          → AgentScope 自动装配
├── controller/      → AiController（categorize / analysis / report / anomaly）
├── service/         → AiCategoryService / AiAnalysisService / AiReportService
└── dto/             → CategorizeRequest/Response, AnalysisResponse, ReportResponse
```

**核心设计决策**：
- expense-ai **不建表、不写数据**，只依赖 expense-bill + expense-category 读数据
- 通过 AgentScope 2.0.0-RC5 对接 DeepSeek API，而非手动 RestTemplate
- AI 调用失败优雅降级，不阻断基础记账功能
- Redis 缓存分析/报告结果（analysis TTL 1h, report TTL 6h）

### 2.2 API 端点

| 端点 | 用途 |
|------|------|
| `POST /api/ai/categorize` | 输入描述+金额 → 返回建议分类+置信度 |
| `POST /api/ai/analysis` | 本月收支+分类 → 3-5 条消费洞察 |
| `POST /api/ai/report` | 月度财务摘要 + 上月对比 |
| `POST /api/ai/anomaly` | 超预算检测预警 |

---

## 3. 前端新增

| 页面 | 新增内容 |
|------|----------|
| Dashboard | 「AI 消费洞察」卡片（SSE 流式输出，Markdown 渲染） |
| BillList 弹窗 | 填写描述后 800ms 防抖自动触发 AI 分类建议 |
| MonthlyStats | 「生成 AI 报告」按钮 + 报告渲染卡片 |

### 3.1 Markdown 渲染

安装 `marked` 库，报告中支持 h2/h3/table/blockquote/ul/ol 等元素渲染，CSS 适配深/浅色主题。

### 3.2 SSE 流式输出

- 后端逐 chunk 推送（增量模式，非全量重发）
- 前端累积拆分，实时显示
- 流式输出中断时 gracefully 完成

---

## 4. 技术决策

| 决策 | 选择 | 理由 |
|------|------|------|
| LLM 框架 | AgentScope 2.0.0-RC5 | Spring Boot 原生集成，优于手动 RestTemplate |
| 缓存方案 | Redis（Spring Cache） | 低延迟缓存命中 < 50ms |
| Prompt 策略 | 结构化 JSON 输出 + Temperature 0.3 | 可控可解析，避免幻觉 |

详见 [architecture-design.md](../architecture-design.md) 和 [iteration-log.md #009-#012](../iteration-log.md)。

---

> ⚡ V3.0 改动：新增 expense-budget 模块（AI 预算建议），AI 异常检测移至预算模块。详见 [v3-design-doc.md](v3-design-doc.md)
