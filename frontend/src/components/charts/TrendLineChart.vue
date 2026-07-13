<template>
  <VChart :option="option" autoresize style="height:300px" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface TrendPoint {
  year: number; month: number; income: number; expense: number; balance: number
}

const props = defineProps<{ points: TrendPoint[] }>()

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入', '支出', '结余'], top: 0, right: 0 },
  grid: { left: 50, right: 20, top: 35, bottom: 20 },
  xAxis: {
    type: 'category',
    data: props.points.map(p => `${p.month}月`)
  },
  yAxis: { type: 'value' },
  series: [
    { name: '收入', type: 'line', data: props.points.map(p => p.income), smooth: true, color: '#67c23a' },
    { name: '支出', type: 'line', data: props.points.map(p => p.expense), smooth: true, color: '#f56c6c' },
    { name: '结余', type: 'line', data: props.points.map(p => p.balance), smooth: true, color: '#409eff' }
  ]
}))
</script>
