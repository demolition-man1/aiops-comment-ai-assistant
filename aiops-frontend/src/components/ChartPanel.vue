<script setup lang="ts">
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  title: string
  option: EChartsOption
  height?: number
}>()

const el = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const render = async () => {
  await nextTick()
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value)
  }
  chart.setOption(props.option, true)
}

const resize = () => chart?.resize()

watch(() => props.option, render, { deep: true })

onMounted(() => {
  void render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <section class="panel chart-panel">
    <div class="panel-title">{{ title }}</div>
    <div ref="el" class="chart-host" :style="{ height: `${height || 280}px` }" />
  </section>
</template>
