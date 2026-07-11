<template>
  <VChart :option="option" autoresize style="height:280px" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart as Pie } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([Pie, TooltipComponent, LegendComponent, CanvasRenderer])

interface CategoryItem {
  categoryName: string; amount: number; percentage: number
}

const props = defineProps<{ data: CategoryItem[] }>()

const option = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
  legend: { orient: 'vertical', right: 0, top: 'center' },
  series: [{
    type: 'pie',
    radius: ['45%', '75%'],
    center: ['40%', '50%'],
    data: props.data.map(d => ({ name: d.categoryName, value: d.amount })),
    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
  }]
}))
</script>
