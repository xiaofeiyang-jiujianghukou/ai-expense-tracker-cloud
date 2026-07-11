# V3.0 设计稿 — AI Expense Tracker

> ✅ **V3.0 已验收，本文档冻结。** 后续版本如需改动 V3 设计，仅在末尾加外联引用，不修改正文。

---

## 1. V3.0 设计目标

在 V2.0 AI 能力基础上，引入数据可视化、预算管理和数据导出，提升数据洞察和实用价值。

### 一句话目标

> **ECharts 可视化财报 + 智能预算建议 + CSV/Excel/PDF 三格式数据导出。**

---

## 2. V3.0 新增模块与改造

### 2.1 ECharts 可视化（Sprint 1）

| 组件 | 页面 | 用途 |
|------|------|------|
| 折线图 | TrendAnalysis（新增页） | N 月收支趋势对比 |
| 饼图 | MonthlyStats（改造） | 分类支出占比 |
| 柱图 | TrendAnalysis | 日支出分布（31天） |

**API**：`POST /api/statistics/trend` + `POST /api/statistics/daily`

### 2.2 预算模块（Sprint 2）

新建 `expense-budget` 模块：
```
expense-budget/
├── controller/   → BudgetController（CRUD）
├── service/      → BudgetService + BudgetAdviceService
├── mapper/
├── entity/
└── dto/
```

| 端点 | 用途 |
|------|------|
| `POST /api/budgets/list` | 按月查询预算列表 |
| `POST /api/budgets/save` | 保存/更新预算 |
| `POST /api/budgets/delete` | 删除预算 |
| `POST /api/ai/advice` | AI 预算建议（近3月统计 + 趋势推算 → 结构化 JSON） |

**AI 预算建议逻辑**：近3月各分类 × 月份统计 → 取有效月份均值 → 按月趋势上调 → 推算全月预算。

### 2.3 数据导出（Sprint 3）

| 格式 | 技术方案 | 端点 |
|------|----------|------|
| CSV | Java BufferedWriter + UTF-8 BOM | `POST /api/bills/export-csv` |
| Excel | EasyExcel 4.0.3 + 美化样式 | `POST /api/statistics/export-excel` |
| PDF | 客户端 window.print() + 弹窗 | 前端 MonthlyStats.vue |

**Excel 美化要求**：深蓝表头白字粗体、全单元格细线边框、内容自动换行、CJK 感知自适应列宽带呼吸间距。详见 [development-standards.md §2.7](../development-standards.md)。

### 2.4 枚举标准化

- `BillType` 枚举（INCOME/EXPENSE）替代所有魔法字符串
- `SecurityUtil.getCurrentUserId()` 统一获取当前用户，全部 Controller 移除 Authentication 参数

---

## 3. 前端新增页面

| 页面 | 新增内容 |
|------|----------|
| TrendAnalysis | ECharts 折线图 + 柱图 + 月份选择器 |
| BudgetManage | 预算列表 + AI 建议弹窗 + 进度条 |
| BillList | 「导出 CSV」按钮 |
| MonthlyStats | 「导出 Excel」+「下载 PDF」按钮 |

---

## 4. 依赖变更

| 新增依赖 | 版本 | 用途 |
|----------|------|------|
| easyexcel | 4.0.3 | Excel 导出 |
| commons-compress | 1.25.0 | 修复 TestContainers 版本冲突 |

详见 [development-plan.md](../development-plan.md) V3.0 Sprint 13-16 和 [iteration-log.md #013-#014](../iteration-log.md)。
