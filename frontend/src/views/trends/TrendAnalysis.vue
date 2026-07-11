<template>
  <AppLayout>
    <div class="trends-page">
      <div class="toolbar">
        <span class="label">月份范围</span>
        <el-slider v-model="monthRange" :min="1" :max="12" :step="1" show-stops
          style="width:260px" @change="fetchTrend" />
        <span class="range-hint">近 {{ monthRange }} 个月</span>
      </div>

      <el-card class="chart-card">
        <template #header><span>收支趋势</span></template>
        <div v-loading="loading">
          <TrendLineChart v-if="trendPoints.length" :points="trendPoints" />
          <el-empty v-else description="暂无数据" :image-size="80" />
        </div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>分类支出对比（{{ currentMonth }}月）</span></template>
        <div v-loading="loading">
          <CategoryBarChart v-if="categoryData.length" :data="categoryData" />
          <el-empty v-else description="暂无数据" :image-size="80" />
        </div>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../../components/AppLayout.vue'
import TrendLineChart from '../../components/charts/TrendLineChart.vue'
import CategoryBarChart from '../../components/charts/CategoryBarChart.vue'
import { getTrend, getMonthlyStats } from '../../api/statistics'

const monthRange = ref(6)
const loading = ref(false)
const trendPoints = ref<any[]>([])
const categoryData = ref<any[]>([])

const currentMonth = computed(() => {
  const now = new Date()
  return now.getMonth() + 1
})

onMounted(() => fetchTrend())

async function fetchTrend() {
  loading.value = true
  try {
    const res = await getTrend(monthRange.value)
    trendPoints.value = res.data.points
  } catch { /* ignore */ }

  // Also fetch current month category breakdown
  const now = new Date()
  try {
    const stats = await getMonthlyStats(now.getFullYear(), now.getMonth() + 1)
    categoryData.value = stats.data.categoryBreakdown || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.label { color: #606266; font-size: 14px; }
.range-hint { color: #909399; font-size: 13px; }
.chart-card { margin-bottom: 16px; }
</style>
