<script setup lang="ts">
import {
  AlertTriangle,
  Boxes,
  MessageSquareWarning,
  RefreshCw,
  ShoppingBag,
  Star
} from 'lucide-vue-next'
import type { EChartsOption } from 'echarts'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { dashboardApi, productApi, commentApi } from '@/api/modules'
import type { Comment, DashboardData, Overview, Product } from '@/api/types'
import ChartPanel from '@/components/ChartPanel.vue'
import MetricCard from '@/components/MetricCard.vue'
import { formatPercent, toProgressPercent } from '@/utils/metricFormat'

const { t } = useI18n()
const loading = ref(false)
const overview = reactive<Overview>({
  productCount: 0,
  sellerCount: 0,
  commentCount: 0,
  avgScore: 0,
  negativeRate: 0
})
const dashboard = ref<DashboardData>()
const products = ref<Product[]>([])
const negativeComments = ref<Comment[]>([])
const sentimentTypes = new Set(['positive', 'neutral', 'negative'])
const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])

const displaySentiment = (value?: string) => {
  const key = value?.trim()
  return key && sentimentTypes.has(key) ? t(`enums.sentiment.${key}`) : key || t('common.unknown')
}

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('enums.problemType.pending')
}

const loadData = async () => {
  loading.value = true
  try {
    const [overviewData, productPage, commentPage] = await Promise.all([
      dashboardApi.overview(),
      productApi.page({ pageNum: 1, pageSize: 8 }),
      commentApi.negative({ pageNum: 1, pageSize: 8 })
    ])

    Object.assign(overview, overviewData)
    products.value = productPage.records || []
    negativeComments.value = commentPage.records || []

    const targetProductId = products.value[0]?.productId
    if (targetProductId) {
      dashboard.value = await dashboardApi.product(targetProductId)
    }
  } finally {
    loading.value = false
  }
}

const sentimentOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '45%'],
      data: (dashboard.value?.sentimentDistribution || []).map((item) => ({
        name: displaySentiment(item.name),
        value: item.count
      }))
    }
  ]
}))

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 38, right: 18, top: 28, bottom: 34 },
  xAxis: {
    type: 'category',
    data: (dashboard.value?.trendDistribution || []).map((item) => item.timeBucket)
  },
  yAxis: [
    { type: 'value', name: t('dashboard.chart.commentCount') },
    { type: 'value', name: t('dashboard.chart.negativeRate'), axisLabel: { formatter: '{value}%' } }
  ],
  series: [
    {
      name: t('dashboard.chart.commentCount'),
      type: 'bar',
      data: (dashboard.value?.trendDistribution || []).map((item) => item.commentCount),
      itemStyle: { color: '#2563eb' }
    },
    {
      name: t('dashboard.chart.negativeRate'),
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      data: (dashboard.value?.trendDistribution || []).map((item) => item.negativeRate),
      itemStyle: { color: '#ef4444' }
    }
  ]
}))

const keywordOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 70, right: 18, top: 20, bottom: 24 },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    inverse: true,
    data: (dashboard.value?.negativeKeywordRank || []).map((item) => item.keyword)
  },
  series: [
    {
      name: t('dashboard.chart.occurrenceCount'),
      type: 'bar',
      data: (dashboard.value?.negativeKeywordRank || []).map((item) => item.count),
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
        <h2 class="section-title">{{ t('dashboard.title') }}</h2>
        <span class="muted">{{ t('dashboard.subtitle') }}</span>
      </div>
      <el-button type="primary" @click="loadData">
        <RefreshCw :size="16" />
        {{ t('dashboard.refreshData') }}
      </el-button>
    </div>

    <div class="grid metrics">
      <MetricCard :title="t('dashboard.productCount')" :value="overview.productCount" :hint="t('dashboard.importedProducts')" tone="blue">
        <Boxes :size="22" />
      </MetricCard>
      <MetricCard :title="t('dashboard.sellerCount')" :value="overview.sellerCount" :hint="t('dashboard.sellerCoverage')" tone="green">
        <ShoppingBag :size="22" />
      </MetricCard>
      <MetricCard :title="t('dashboard.commentTotal')" :value="overview.commentCount" :hint="t('dashboard.availableComments')" tone="amber">
        <MessageSquareWarning :size="22" />
      </MetricCard>
      <MetricCard
        :title="t('common.avgScore')"
        :value="Number(overview.avgScore || 0).toFixed(2)"
        :hint="t('dashboard.negativeRateHint', { rate: formatPercent(overview.negativeRate) })"
        tone="red"
      >
        <Star :size="22" />
      </MetricCard>
    </div>

    <div class="grid two section-gap">
      <ChartPanel :title="t('dashboard.trendTitle')" :option="trendOption" :height="320" />
      <ChartPanel :title="t('dashboard.sentimentTitle')" :option="sentimentOption" :height="320" />
    </div>

    <div class="grid two section-gap">
      <ChartPanel :title="t('dashboard.negativeKeywordsTitle')" :option="keywordOption" :height="310" />

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('dashboard.recentNegative') }}</div>
          <el-tag type="danger" effect="plain">
            <AlertTriangle :size="14" />
            {{ t('dashboard.riskFocus') }}
          </el-tag>
        </div>
        <el-table :data="negativeComments" height="260" size="small">
          <el-table-column prop="productId" :label="t('common.productId')" width="130" show-overflow-tooltip />
          <el-table-column prop="reviewScore" :label="t('common.score')" width="70" />
          <el-table-column :label="t('dashboard.problemType')" width="110">
            <template #default="{ row }">
              <el-tag type="warning" effect="plain">
                {{ displayProblemType(row.effectiveProblemType || row.systemProblemType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('dashboard.reviewContent')">
            <template #default="{ row }">
              <span class="comment-cell">{{ row.cleanContent || row.reviewContent || row.reviewTitle || t('common.dash') }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="panel-title">{{ t('dashboard.productRiskOverview') }}</div>
      <el-table :data="products" size="small">
        <el-table-column prop="productId" :label="t('common.productId')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" :label="t('common.category')" width="160" show-overflow-tooltip />
        <el-table-column prop="reviewCount" :label="t('common.commentCount')" width="100" />
        <el-table-column prop="avgScore" :label="t('common.avgScore')" width="110" />
        <el-table-column :label="t('common.negativeRate')" width="140">
          <template #default="{ row }">
            <el-progress :percentage="toProgressPercent(row.negativeRate)" :stroke-width="8" :show-text="false" />
            <span class="muted">{{ formatPercent(row.negativeRate) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
