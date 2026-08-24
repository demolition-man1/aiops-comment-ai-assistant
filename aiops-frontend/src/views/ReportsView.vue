<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { BarChart3, Download, MessageSquareWarning, RefreshCw, ShoppingBag, Star } from 'lucide-vue-next'

import { reportApi } from '@/api/modules'
import type { DashboardData, ProductRank, ReportOverview } from '@/api/types'
import ChartPanel from '@/components/ChartPanel.vue'
import MetricCard from '@/components/MetricCard.vue'
import { saveBlob } from '@/utils/download'
import { formatPercent } from '@/utils/metricFormat'

const { t } = useI18n()
const loading = ref(false)
const exporting = ref(false)
const overview = reactive<ReportOverview>({
  productCount: 0,
  sellerCount: 0,
  commentCount: 0,
  avgScore: 0,
  negativeRate: 0,
  trendDistribution: [],
  sentimentDistribution: [],
  problemDistribution: [],
  highRiskProducts: [],
  hotProducts: [],
  topRatedProducts: []
})

const distributions = ref<DashboardData>({
  scoreDistribution: [],
  sentimentDistribution: [],
  categoryDistribution: [],
  keywordRank: [],
  negativeKeywordRank: [],
  problemDistribution: [],
  customTagDistribution: [],
  trendDistribution: []
})

const ranks = reactive<ProductRank>({
  hotProducts: [],
  highRiskProducts: [],
  topRatedProducts: []
})

const sentimentTypes = new Set(['positive', 'neutral', 'negative'])
const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])

const displaySentiment = (value?: string) => {
  const key = value?.trim()
  return key && sentimentTypes.has(key) ? t(`enums.sentiment.${key}`) : key || t('common.unknown')
}

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('common.unknown')
}

const loadData = async () => {
  loading.value = true
  try {
    const [overviewData, distributionData, rankData] = await Promise.all([
      reportApi.overview(),
      reportApi.distributions(),
      reportApi.productRank({ limit: 8 })
    ])
    Object.assign(overview, overviewData)
    distributions.value = distributionData
    ranks.hotProducts = rankData.hotProducts || []
    ranks.highRiskProducts = rankData.highRiskProducts || []
    ranks.topRatedProducts = rankData.topRatedProducts || []
  } finally {
    loading.value = false
  }
}

const exportReport = async () => {
  exporting.value = true
  try {
    const blob = await reportApi.exportCsv()
    saveBlob(blob, `aiops-report-${Date.now()}.csv`)
    ElMessage.success(t('reports.exported'))
  } finally {
    exporting.value = false
  }
}

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 42, right: 24, top: 32, bottom: 34 },
  xAxis: {
    type: 'category',
    data: (overview.trendDistribution || []).map((item) => item.timeBucket)
  },
  yAxis: [
    { type: 'value', name: t('dashboard.chart.commentCount') },
    { type: 'value', name: t('dashboard.chart.negativeRate'), axisLabel: { formatter: '{value}%' } }
  ],
  series: [
    {
      name: t('dashboard.chart.commentCount'),
      type: 'bar',
      data: (overview.trendDistribution || []).map((item) => item.commentCount),
      itemStyle: { color: '#2563eb' }
    },
    {
      name: t('dashboard.chart.negativeRate'),
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: (overview.trendDistribution || []).map((item) => item.negativeRate),
      itemStyle: { color: '#ef4444' }
    }
  ]
}))

const sentimentOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['50%', '45%'],
      data: (distributions.value.sentimentDistribution || []).map((item) => ({
        name: displaySentiment(item.name),
        value: item.count
      }))
    }
  ]
}))

const problemOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 72, right: 18, top: 22, bottom: 24 },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    inverse: true,
    data: (distributions.value.problemDistribution || []).map((item) => displayProblemType(item.name))
  },
  series: [
    {
      name: t('dashboard.chart.occurrenceCount'),
      type: 'bar',
      data: (distributions.value.problemDistribution || []).map((item) => item.count),
      itemStyle: { color: '#f59e0b' }
    }
  ]
}))

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('reports.title') }}</h2>
        <span class="muted">{{ t('reports.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button :loading="exporting" @click="exportReport">
          <Download :size="16" />
          {{ t('reports.exportCsv') }}
        </el-button>
        <el-button type="primary" @click="loadData">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="grid metrics">
      <MetricCard :title="t('dashboard.productCount')" :value="overview.productCount" :hint="t('reports.productHint')" tone="blue">
        <ShoppingBag :size="22" />
      </MetricCard>
      <MetricCard :title="t('dashboard.commentTotal')" :value="overview.commentCount" :hint="t('reports.commentHint')" tone="amber">
        <MessageSquareWarning :size="22" />
      </MetricCard>
      <MetricCard :title="t('common.avgScore')" :value="Number(overview.avgScore || 0).toFixed(2)" :hint="t('reports.scoreHint')" tone="green">
        <Star :size="22" />
      </MetricCard>
      <MetricCard :title="t('common.negativeRate')" :value="formatPercent(overview.negativeRate)" :hint="t('reports.riskHint')" tone="red">
        <BarChart3 :size="22" />
      </MetricCard>
    </div>

    <div class="grid two section-gap">
      <ChartPanel :title="t('reports.trend')" :option="trendOption" :height="320" />
      <ChartPanel :title="t('reports.sentiment')" :option="sentimentOption" :height="320" />
    </div>

    <div class="grid two section-gap">
      <ChartPanel :title="t('reports.problem')" :option="problemOption" :height="310" />
      <div class="panel">
        <div class="panel-title">{{ t('reports.reportSummary') }}</div>
        <div class="report-summary">
          <div>
            <span class="muted">{{ t('reports.hotProductCount') }}</span>
            <strong>{{ ranks.hotProducts.length }}</strong>
          </div>
          <div>
            <span class="muted">{{ t('reports.highRiskCount') }}</span>
            <strong>{{ ranks.highRiskProducts.length }}</strong>
          </div>
          <div>
            <span class="muted">{{ t('reports.topRatedCount') }}</span>
            <strong>{{ ranks.topRatedProducts.length }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="grid three section-gap">
      <div class="panel">
        <div class="panel-title">{{ t('reports.hotProducts') }}</div>
        <el-table :data="ranks.hotProducts" size="small" height="300">
          <el-table-column prop="productId" :label="t('common.productId')" show-overflow-tooltip />
          <el-table-column prop="reviewCount" :label="t('common.commentCount')" width="100" />
        </el-table>
      </div>
      <div class="panel">
        <div class="panel-title">{{ t('reports.highRiskProducts') }}</div>
        <el-table :data="ranks.highRiskProducts" size="small" height="300">
          <el-table-column prop="productId" :label="t('common.productId')" show-overflow-tooltip />
          <el-table-column :label="t('common.negativeRate')" width="110">
            <template #default="{ row }">{{ formatPercent(row.negativeRate) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="panel">
        <div class="panel-title">{{ t('reports.topRatedProducts') }}</div>
        <el-table :data="ranks.topRatedProducts" size="small" height="300">
          <el-table-column prop="productId" :label="t('common.productId')" show-overflow-tooltip />
          <el-table-column prop="avgScore" :label="t('common.avgScore')" width="100" />
        </el-table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.report-summary {
  display: grid;
  gap: 12px;
}

.report-summary > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #f8fafc;
}

.report-summary strong {
  color: #0f172a;
  font-size: 24px;
}
</style>
