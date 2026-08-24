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

import { dashboardApi, productApi, commentApi } from '@/api/modules'
import type { Comment, DashboardData, Overview, Product } from '@/api/types'
import ChartPanel from '@/components/ChartPanel.vue'
import MetricCard from '@/components/MetricCard.vue'
import { formatPercent, toProgressPercent } from '@/utils/metricFormat'

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
        name: item.name,
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
    { type: 'value', name: '评论数' },
    { type: 'value', name: '负面率', axisLabel: { formatter: '{value}%' } }
  ],
  series: [
    {
      name: '评论数',
      type: 'bar',
      data: (dashboard.value?.trendDistribution || []).map((item) => item.commentCount),
      itemStyle: { color: '#2563eb' }
    },
    {
      name: '负面率',
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
      name: '出现次数',
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
        <h2 class="section-title">今日运营概览</h2>
        <span class="muted">汇总商品、评论、评分和负面风险指标</span>
      </div>
      <el-button type="primary" @click="loadData">
        <RefreshCw :size="16" />
        刷新数据
      </el-button>
    </div>

    <div class="grid metrics">
      <MetricCard title="商品数量" :value="overview.productCount" hint="已导入商品总数" tone="blue">
        <Boxes :size="22" />
      </MetricCard>
      <MetricCard title="商家数量" :value="overview.sellerCount" hint="Olist 数据商家覆盖" tone="green">
        <ShoppingBag :size="22" />
      </MetricCard>
      <MetricCard title="评论总量" :value="overview.commentCount" hint="可用于评论挖掘的数据" tone="amber">
        <MessageSquareWarning :size="22" />
      </MetricCard>
      <MetricCard title="平均评分" :value="Number(overview.avgScore || 0).toFixed(2)" :hint="`负面占比 ${formatPercent(overview.negativeRate)}`" tone="red">
        <Star :size="22" />
      </MetricCard>
    </div>

    <div class="grid two section-gap">
      <ChartPanel title="评论趋势与负面率" :option="trendOption" :height="320" />
      <ChartPanel title="情感分布" :option="sentimentOption" :height="320" />
    </div>

    <div class="grid two section-gap">
      <ChartPanel title="差评高频词" :option="keywordOption" :height="310" />

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">近期负面评论</div>
          <el-tag type="danger" effect="plain">
            <AlertTriangle :size="14" />
            风险关注
          </el-tag>
        </div>
        <el-table :data="negativeComments" height="260" size="small">
          <el-table-column prop="productId" label="商品ID" width="130" show-overflow-tooltip />
          <el-table-column prop="reviewScore" label="评分" width="70" />
          <el-table-column label="问题类型" width="110">
            <template #default="{ row }">
              <el-tag type="warning" effect="plain">
                {{ row.effectiveProblemType || row.systemProblemType || '待识别' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评论内容">
            <template #default="{ row }">
              <span class="comment-cell">{{ row.cleanContent || row.reviewContent || row.reviewTitle || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="panel-title">商品风险速览</div>
      <el-table :data="products" size="small">
        <el-table-column prop="productId" label="商品ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="类目" width="160" show-overflow-tooltip />
        <el-table-column prop="reviewCount" label="评论数" width="100" />
        <el-table-column prop="avgScore" label="平均评分" width="110" />
        <el-table-column label="负面占比" width="140">
          <template #default="{ row }">
            <el-progress :percentage="toProgressPercent(row.negativeRate)" :stroke-width="8" :show-text="false" />
            <span class="muted">{{ formatPercent(row.negativeRate) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
