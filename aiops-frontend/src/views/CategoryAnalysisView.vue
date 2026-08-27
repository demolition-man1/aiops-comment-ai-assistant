<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { AlertTriangle, Boxes, MessageSquareWarning, RefreshCw, ShoppingBag } from 'lucide-vue-next'

import { reportApi } from '@/api/modules'
import type { CategoryAnalysis } from '@/api/types'
import ChartPanel from '@/components/ChartPanel.vue'
import MetricCard from '@/components/MetricCard.vue'
import { formatPercent, toPercentValue } from '@/utils/metricFormat'

const { t } = useI18n()
const loading = ref(false)
const categories = ref<CategoryAnalysis[]>([])

const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])
const riskTypes = new Set(['none', 'low', 'medium', 'high'])

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('common.unknown')
}

const displayRiskLevel = (value?: string) => {
  const key = value?.trim()
  return key && riskTypes.has(key) ? t(`categories.risk.${key}`) : key || t('common.unknown')
}

const riskTagType = (riskLevel?: string) => {
  if (riskLevel === 'high') {
    return 'danger'
  }
  if (riskLevel === 'medium') {
    return 'warning'
  }
  if (riskLevel === 'low') {
    return 'success'
  }
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    categories.value = await reportApi.categories({ limit: 30 })
  } finally {
    loading.value = false
  }
}

const totalProducts = computed(() => categories.value.reduce((sum, item) => sum + Number(item.productCount || 0), 0))
const totalComments = computed(() => categories.value.reduce((sum, item) => sum + Number(item.commentCount || 0), 0))
const highRiskCount = computed(() => categories.value.filter((item) => item.riskLevel === 'high').length)
const topRiskCategory = computed(() => categories.value[0]?.categoryName || t('common.dash'))

const riskOption = computed<EChartsOption>(() => {
  const rows = categories.value.slice(0, 10)
  return {
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value) => `${Number(value || 0).toFixed(1)}%`
    },
    grid: { left: 118, right: 24, top: 24, bottom: 24 },
    xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    yAxis: {
      type: 'category',
      inverse: true,
      data: rows.map((item) => item.categoryName)
    },
    series: [
      {
        name: t('common.negativeRate'),
        type: 'bar',
        data: rows.map((item) => toPercentValue(item.negativeRate)),
        itemStyle: { color: '#ef4444' }
      }
    ]
  }
})

const volumeOption = computed<EChartsOption>(() => {
  const rows = [...categories.value]
    .sort((left, right) => Number(right.commentCount || 0) - Number(left.commentCount || 0))
    .slice(0, 10)
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 52, right: 18, top: 28, bottom: 42 },
    xAxis: {
      type: 'category',
      axisLabel: { interval: 0, rotate: 28 },
      data: rows.map((item) => item.categoryName)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: t('categories.commentVolume'),
        type: 'bar',
        data: rows.map((item) => item.commentCount),
        itemStyle: { color: '#2563eb' }
      },
      {
        name: t('categories.productVolume'),
        type: 'bar',
        data: rows.map((item) => item.productCount),
        itemStyle: { color: '#14b8a6' }
      }
    ]
  }
})

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('categories.title') }}</h2>
        <span class="muted">{{ t('categories.subtitle') }}</span>
      </div>
      <el-button type="primary" @click="loadData">
        <RefreshCw :size="16" />
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <div class="grid metrics">
      <MetricCard :title="t('categories.categoryCount')" :value="categories.length" :hint="t('categories.categoryHint')" tone="blue">
        <Boxes :size="22" />
      </MetricCard>
      <MetricCard :title="t('categories.productTotal')" :value="totalProducts" :hint="t('categories.productHint')" tone="green">
        <ShoppingBag :size="22" />
      </MetricCard>
      <MetricCard :title="t('categories.commentTotal')" :value="totalComments" :hint="t('categories.commentHint')" tone="amber">
        <MessageSquareWarning :size="22" />
      </MetricCard>
      <MetricCard :title="t('categories.highRiskCount')" :value="highRiskCount" :hint="t('categories.topRiskHint', { category: topRiskCategory })" tone="red">
        <AlertTriangle :size="22" />
      </MetricCard>
    </div>

    <div class="grid two section-gap">
      <ChartPanel :title="t('categories.riskChart')" :option="riskOption" :height="340" />
      <ChartPanel :title="t('categories.volumeChart')" :option="volumeOption" :height="340" />
    </div>

    <div class="panel section-gap">
      <div class="panel-title">{{ t('categories.tableTitle') }}</div>
      <el-table :data="categories" size="small" height="520">
        <el-table-column prop="categoryName" :label="t('common.category')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productCount" :label="t('categories.productVolume')" width="120" />
        <el-table-column prop="commentCount" :label="t('categories.commentVolume')" width="120" />
        <el-table-column prop="avgScore" :label="t('common.avgScore')" width="120" />
        <el-table-column prop="negativeCount" :label="t('categories.negativeCount')" width="120" />
        <el-table-column :label="t('common.negativeRate')" width="130">
          <template #default="{ row }">{{ formatPercent(row.negativeRate) }}</template>
        </el-table-column>
        <el-table-column :label="t('categories.topProblem')" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ displayProblemType(row.topProblemType) }}
            <span class="muted">({{ row.topProblemCount || 0 }})</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('categories.riskLevel')" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">
              {{ displayRiskLevel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
