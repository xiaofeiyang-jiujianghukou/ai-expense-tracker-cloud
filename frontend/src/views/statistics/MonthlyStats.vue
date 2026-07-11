<template>
  <AppLayout>
    <div class="stats-page">
      <div class="toolbar">
        <el-date-picker v-model="selectedMonth" type="month" format="YYYY年M月" value-format="YYYY-MM"
          placeholder="选择月份" @change="fetchStats" />
        <el-button type="primary" :loading="reportLoading" @click="fetchReport">生成 AI 报告</el-button>
        <el-button @click="exportExcelFile">导出 Excel</el-button>
      </div>

      <div class="cards">
        <el-card class="card income"><div class="label">收入</div><div class="value">¥{{ stats.income }}</div></el-card>
        <el-card class="card expense"><div class="label">支出</div><div class="value">¥{{ stats.expense }}</div></el-card>
        <el-card class="card balance"><div class="label">结余</div><div class="value">¥{{ stats.balance }}</div></el-card>
      </div>

      <el-card v-if="stats.categoryBreakdown?.length" class="breakdown">
        <template #header><span>支出分类占比</span></template>
        <PieChart :data="stats.categoryBreakdown" />
      </el-card>

      <el-card class="report-card">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
            <span>AI 财务报告</span>
            <el-button v-if="report" size="small" @click="exportPdf">下载 PDF</el-button>
          </div>
        </template>
        <div v-if="report" class="report-content" v-html="renderedReport"></div>
        <div v-else class="report-placeholder">
          <el-icon class="is-loading" :size="24"><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32zm0 640a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V736a32 32 0 0 1 32-32zm448-192a32 32 0 0 1-32 32H736a32 32 0 0 1 0-64h192a32 32 0 0 1 32 32zm-640 0a32 32 0 0 1-32 32H96a32 32 0 0 1 0-64h192a32 32 0 0 1 32 32z"/></svg></el-icon>
          <span>{{ reportLoading || reportInitialLoading ? 'AI 正在生成财务报告…' : '点击「生成 AI 报告」获取月度财务分析' }}</span>
        </div>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import PieChart from '../../components/charts/PieChart.vue'
import { getMonthlyStats, exportExcel } from '../../api/statistics'
import { getReport } from '../../api/ai'
import { ElMessage } from 'element-plus'
import { getToken } from '../../utils/auth'
import { marked } from 'marked'

const now = new Date()
const selectedMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const stats = reactive({ income: 0, expense: 0, balance: 0, categoryBreakdown: [] as any[] })
const report = ref('')
const reportLoading = ref(false)
const reportInitialLoading = ref(true)
const renderedReport = ref('')

// Watch + manual assignment ensures every streaming chunk triggers a fresh markdown → HTML render.
// Using computed() can cause stale rendering during SSE because Vue may cache the result
// when the accumulated text hasn't produced a structurally different HTML output yet.
watch(report, (val) => {
  if (!val) { renderedReport.value = ''; return }
  renderedReport.value = marked(val) as string
})

onMounted(() => { fetchStats(); loadCachedReport() })
async function fetchStats() {
  const [y, m] = selectedMonth.value.split('-')
  try {
    const res = await getMonthlyStats(Number(y), Number(m))
    Object.assign(stats, res.data)
  } catch { /* ignore */ }
}

// Load cached report on page enter (no force refresh)
async function loadCachedReport() {
  reportInitialLoading.value = true
  const [y, m] = selectedMonth.value.split('-')
  try {
    const res = await getReport({ year: Number(y), month: Number(m) })
    report.value = res.data.report
  } catch { /* no cache or error, wait for user to click generate */ }
  finally { reportInitialLoading.value = false }
}

// Force regenerate via SSE streaming
async function fetchReport() {
  reportLoading.value = true
  report.value = ''
  const [y, m] = selectedMonth.value.split('-')
  try {
    const token = getToken()
    const resp = await fetch('/api/ai/report/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ year: Number(y), month: Number(m), forceRefresh: true })
    })
    const reader = resp.body?.getReader()
    if (!reader) throw new Error('No response body')
    const decoder = new TextDecoder()
    let buffer = ''
    let prevWasData = false
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('event:chunk')) continue
        if (line.startsWith('data:')) {
          // Spring SseEmitter splits multi-line data into separate data: lines.
          // We must re-insert newlines between consecutive data: lines, otherwise
          // markdown like "## Header\n\nParagraph" becomes "## HeaderParagraph"
          // and the parser sees no structure.
          if (prevWasData) report.value += '\n'
          report.value += line.substring(5)
          prevWasData = true
          continue
        }
        prevWasData = false
        if (line.startsWith('event:done')) {
          reportLoading.value = false
          return
        }
      }
    }
  } catch { /* AI failed, non-blocking */ }
  finally { reportLoading.value = false }
}

async function exportExcelFile() {
  if (stats.categoryBreakdown.length === 0) {
    ElMessage.warning('暂无数据可导出'); return
  }
  try {
    const [y, m] = selectedMonth.value.split('-')
    const blob = await exportExcel(Number(y), Number(m))
    const url = URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url; a.download = `月报_${y}年${m}月.xlsx`; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel 导出成功')
  } catch { ElMessage.warning('导出失败') }
}

function exportPdf() {
  if (!report.value) return
  const win = window.open('', '_blank', 'width=800,height=600')!
  win.document.write(`
    <!DOCTYPE html><html><head><meta charset="utf-8"><title>AI 财务报告</title>
    <style>
      body { font-family: "Microsoft YaHei", sans-serif; max-width: 700px; margin: 0 auto; padding: 0 20px 40px; line-height: 1.8; color: #333; }
      .toolbar { position: sticky; top: 0; background: #fff; padding: 12px 0; border-bottom: 1px solid #eee; margin-bottom: 20px; display: flex; gap: 8px; align-items: center; z-index: 10; }
      .btn { padding: 8px 20px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
      .btn-primary { background: #409eff; color: #fff; }
      .btn-primary:hover { background: #337ecc; }
      .btn-default { background: #f5f7fa; color: #606266; border: 1px solid #dcdfe6; }
      .btn-default:hover { color: #409eff; border-color: #c6e2ff; background: #ecf5ff; }
      .hint { font-size: 13px; color: #909399; }
      h2 { font-size: 18px; margin: 16px 0 8px; padding-bottom: 6px; border-bottom: 1px solid #eee; }
      h3 { font-size: 16px; margin: 14px 0 6px; }
      strong { color: #333; }
      ul, ol { padding-left: 20px; margin: 8px 0; }
      li { margin-bottom: 4px; }
      table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 14px; }
      th { background: #f5f7fa; text-align: left; padding: 8px 12px; border: 1px solid #ddd; font-weight: 600; }
      td { padding: 8px 12px; border: 1px solid #ddd; }
      tr:nth-child(even) { background: #fafafa; }
      blockquote { margin: 12px 0; padding: 8px 16px; border-left: 4px solid #409eff; background: #ecf5ff; color: #606266; }
      hr { border: none; border-top: 1px solid #eee; margin: 16px 0; }
      code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; }
      p { margin: 8px 0; }
      @media print {
        .toolbar { display: none; }
        body { margin: 0.8cm; padding: 0; }
        @page { margin: 0.8cm; }
      }
    </style></head><body>
      <div class="toolbar">
        <button class="btn btn-primary" onclick="window.print()">🖨️ 打印 / 保存 PDF</button>
        <span class="hint">点击后在打印对话框选「另存为 PDF」→ 更多设置中取消勾选「页眉和页脚」</span>
      </div>
      ${renderedReport.value}
    </body></html>
  `)
  win.document.close()
}
</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.cards { display: flex; gap: 16px; margin-bottom: 20px; }
.card { flex: 1; text-align: center; }
.card .label { color: #999; font-size: 14px; }
.card .value { font-size: 28px; font-weight: bold; margin-top: 8px; }
.card.income .value { color: #67c23a; }
.card.expense .value { color: #f56c6c; }
.card.balance .value { color: #409eff; }
.report-card { margin-top: 20px; }
.report-placeholder { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 24px 0; color: #909399; font-size: 14px; }
.report-content { line-height: 1.8; color: #303133; }
.report-content :deep(h2) { font-size: 18px; margin: 16px 0 8px; padding-bottom: 6px; border-bottom: 1px solid #ebeef5; }
.report-content :deep(h3) { font-size: 16px; margin: 14px 0 6px; }
.report-content :deep(strong) { color: #303133; }
.report-content :deep(ul), .report-content :deep(ol) { padding-left: 20px; margin: 8px 0; }
.report-content :deep(li) { margin-bottom: 4px; }
.report-content :deep(table) { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 14px; }
.report-content :deep(th) { background: #f5f7fa; text-align: left; padding: 8px 12px; border: 1px solid #ebeef5; font-weight: 600; }
.report-content :deep(td) { padding: 8px 12px; border: 1px solid #ebeef5; }
.report-content :deep(tr:nth-child(even)) { background: #fafafa; }
.report-content :deep(blockquote) { margin: 12px 0; padding: 8px 16px; border-left: 4px solid #409eff; background: #ecf5ff; color: #606266; border-radius: 0 4px 4px 0; }
.report-content :deep(hr) { border: none; border-top: 1px solid #ebeef5; margin: 16px 0; }
.report-content :deep(code) { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.report-content :deep(p) { margin: 8px 0; }
</style>
