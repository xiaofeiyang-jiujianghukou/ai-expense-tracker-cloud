<template>
  <VChart :option="option" autoresize style="height:280px" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface CategoryItem {
  categoryName: string; amount: number
}

const props = defineProps<{ data: CategoryItem[] }>()

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 10, bottom: 30 },
  xAxis: { type: 'category', data: props.data.map(d => d.categoryName) },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar',
    data: props.data.map(d => ({ value: d.amount, itemStyle: { color: '#409eff' } })),
    barMaxWidth: 40
  }]
}))
</script>
