<template>
  <AppLayout>
    <div class="dashboard">
      <div class="cards">
        <el-card class="card income"><div class="label">本月收入</div><div class="value">¥{{ stats.income }}</div></el-card>
        <el-card class="card expense"><div class="label">本月支出</div><div class="value">¥{{ stats.expense }}</div></el-card>
        <el-card class="card balance"><div class="label">本月结余</div><div class="value">¥{{ stats.balance }}</div></el-card>
      </div>

      <el-card class="insights">
        <template #header>
          <div class="card-header">
            <span>AI 消费洞察</span>
            <el-button type="primary" size="small" :loading="insightLoading" @click="refreshInsights">重新生成</el-button>
          </div>
        </template>
        <div v-if="insights.length || typingHint" class="insight-list">
          <ul>
            <li v-for="(item, i) in insights" :key="i">{{ item }}</li>
            <li v-if="typingHint" class="typing-line">{{ typingHint }}</li>
          </ul>
        </div>
        <div v-else class="insight-placeholder">
          <el-icon class="is-loading" :size="24"><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32zm0 640a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V736a32 32 0 0 1 32-32zm448-192a32 32 0 0 1-32 32H736a32 32 0 0 1 0-64h192a32 32 0 0 1 32 32zm-640 0a32 32 0 0 1-32 32H96a32 32 0 0 1 0-64h192a32 32 0 0 1 32 32z"/></svg></el-icon>
          <span>{{ insightLoading || insightsLoading ? 'AI 正在分析您的消费数据…' : '点击「重新生成」获取 AI 消费洞察' }}</span>
        </div>
      </el-card>

      <el-card class="recent">
        <template #header>
          <div class="card-header">
            <span>最近账单</span>
            <el-button type="primary" size="small" @click="router.push('/bills')">新增</el-button>
          </div>
        </template>
        <el-table :data="recentBills" stripe size="small" v-loading="loading">
          <el-table-column prop="billDate" label="日期" width="110" />
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">
              <span :class="row.type === 'INCOME' ? 'income-text' : 'expense-text'">
                {{ row.type === 'INCOME' ? '+' : '-' }}¥{{ row.amount }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../../components/AppLayout.vue'
import { listBills, type BillVO } from '../../api/bill'
import { getMonthlyStats } from '../../api/statistics'
import { getAnalysis } from '../../api/ai'
import { getToken } from '../../utils/auth'

const router = useRouter()
const loading = ref(false)
const recentBills = ref<BillVO[]>([])
const stats = ref({ income: 0, expense: 0, balance: 0 })
const insights = ref<string[]>([])
const insightLoading = ref(false)
const insightsLoading = ref(true)  // initial page-load loading state
const typingHint = ref('')         // in-progress last line during streaming

onMounted(async () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1

  try {
    const res = await listBills({ page: 1, size: 8 })
    recentBills.value = res.data.records
  } catch { /* bill list failed, keep empty */ }

  try {
    const res = await getMonthlyStats(year, month)
    stats.value = res.data
  } catch { /* stats failed, keep zeros */ }

  // Default: load from cache
  loadInsights(year, month)
})

async function loadInsights(year: number, month: number) {
  insightsLoading.value = true
  try {
    const res = await getAnalysis({ year, month })
    insights.value = res.data.insights
  } catch { /* AI failed, non-blocking */ }
  finally { insightsLoading.value = false }
}

async function refreshInsights() {
  insightLoading.value = true
  insights.value = []
  typingHint.value = ''
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  try {
    const token = getToken()
    const resp = await fetch('/api/ai/analysis/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({ year, month, forceRefresh: true })
    })
    const reader = resp.body?.getReader()
    if (!reader) throw new Error('No body')
    const decoder = new TextDecoder()
    let sseBuffer = ''
    let textAcc = ''  // accumulated raw insight text
    let prevWasData = false
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        const final = textAcc.trim()
        if (final) insights.value.push(final)
        textAcc = ''
        break
      }
      sseBuffer += decoder.decode(value, { stream: true })
      const lines = sseBuffer.split('\n')
      sseBuffer = lines.pop() || ''
      for (const rawLine of lines) {
        if (rawLine.startsWith('event:line') || rawLine.startsWith('event:chunk')) { prevWasData = false; continue }
        if (rawLine.startsWith('data:')) {
          if (prevWasData) textAcc += '\n'
          textAcc += rawLine.substring(5)
          prevWasData = true
          const parts = textAcc.split('\n')
          textAcc = parts.pop() || ''
          for (const p of parts) {
            const trimmed = p.trim()
            if (trimmed) insights.value.push(trimmed)
          }
          typingHint.value = textAcc.trim()
          continue
        }
        prevWasData = false
        if (rawLine.startsWith('event:done')) {
          const final = textAcc.trim()
          if (final) insights.value.push(final)
          textAcc = ''
          typingHint.value = ''
          insightLoading.value = false
          return
        }
      }
    }
  } catch { /* AI failed, non-blocking */ }
  finally { insightLoading.value = false; typingHint.value = '' }
}
</script>

<style scoped>
.cards { display: flex; gap: 16px; margin-bottom: 20px; }
.card { flex: 1; text-align: center; }
.card .label { color: #999; font-size: 14px; }
.card .value { font-size: 28px; font-weight: bold; margin-top: 8px; }
.card.income .value { color: #67c23a; }
.card.expense .value { color: #f56c6c; }
.card.balance .value { color: #409eff; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.income-text { color: #67c23a; }
.expense-text { color: #f56c6c; }
.insights { margin-bottom: 16px; }
.insights ul { margin: 0; padding-left: 20px; }
.insights li { margin-bottom: 6px; color: #606266; line-height: 1.6; }
.insight-placeholder { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 24px 0; color: #909399; font-size: 14px; }
.typing-line { color: #409eff; }
</style>
