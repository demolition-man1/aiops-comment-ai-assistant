<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Archive, ArchiveRestore, BarChart3, Download, Eye, FileText, MessageSquareWarning, RefreshCw, Search, ShoppingBag, Star } from 'lucide-vue-next'

import { aiApi, reportApi, reportArchiveApi } from '@/api/modules'
import type { DashboardData, OperationReport, ProductRank, ReportArchive, ReportOverview } from '@/api/types'
import ChartPanel from '@/components/ChartPanel.vue'
import MetricCard from '@/components/MetricCard.vue'
import { useLocaleStore } from '@/stores/locale'
import { saveBlob } from '@/utils/download'
import { formatPercent } from '@/utils/metricFormat'

const { t } = useI18n()
const localeStore = useLocaleStore()
const loading = ref(false)
const exporting = ref(false)
const archiveLoading = ref(false)
const archiveDetailLoading = ref(false)
const archiveDialogVisible = ref(false)
const sourceReportDialogVisible = ref(false)
const sourceReportLoading = ref(false)
const archiveActionLoadingId = ref<number>()
const archiveStatusLoadingId = ref<number>()
const archivePdfLoadingId = ref<number>()
const archivedReports = ref<ReportArchive[]>([])
const sourceReports = ref<OperationReport[]>([])
const selectedArchive = ref<ReportArchive>()
const archiveQuery = reactive({
  targetType: '',
  targetId: '',
  keyword: '',
  archiveStatus: '',
  dateRange: [] as string[]
})
const archivePage = reactive({
  pageNum: 1,
  pageSize: 5,
  total: 0
})
const sourcePage = reactive({
  pageNum: 1,
  pageSize: 5,
  total: 0
})
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
const targetTypes = new Set(['product', 'seller'])
const archiveStatuses = new Set(['archived', 'restored'])

const displaySentiment = (value?: string) => {
  const key = value?.trim()
  return key && sentimentTypes.has(key) ? t(`enums.sentiment.${key}`) : key || t('common.unknown')
}

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('common.unknown')
}

const displayTargetType = (value?: string) => {
  const key = value?.trim()
  return key && targetTypes.has(key) ? t(`reports.targetTypes.${key}`) : key || t('common.unknown')
}

const displayArchiveStatus = (value?: string) => {
  const key = value?.trim()
  return key && archiveStatuses.has(key) ? t(`reports.archiveStatuses.${key}`) : key || t('common.unknown')
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return t('common.dash')
  }
  return value.replace('T', ' ').slice(0, 19)
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

const loadArchives = async () => {
  archiveLoading.value = true
  try {
    const page = await reportArchiveApi.page({
      pageNum: archivePage.pageNum,
      pageSize: archivePage.pageSize,
      targetType: archiveQuery.targetType || undefined,
      targetId: archiveQuery.targetId.trim() || undefined,
      keyword: archiveQuery.keyword.trim() || undefined,
      archiveStatus: archiveQuery.archiveStatus || undefined,
      startTime: archiveQuery.dateRange[0] || undefined,
      endTime: archiveQuery.dateRange[1] || undefined
    })
    archivedReports.value = page.records || []
    archivePage.total = page.total || 0
  } finally {
    archiveLoading.value = false
  }
}

const searchArchives = async () => {
  archivePage.pageNum = 1
  await loadArchives()
}

const resetArchiveFilters = async () => {
  archiveQuery.targetType = ''
  archiveQuery.targetId = ''
  archiveQuery.keyword = ''
  archiveQuery.archiveStatus = ''
  archiveQuery.dateRange = []
  archivePage.pageNum = 1
  await loadArchives()
}

const handleArchivePageSize = async (pageSize: number) => {
  archivePage.pageSize = pageSize
  archivePage.pageNum = 1
  await loadArchives()
}

const viewArchive = async (row: ReportArchive) => {
  archiveDialogVisible.value = true
  archiveDetailLoading.value = true
  try {
    selectedArchive.value = await reportArchiveApi.detail(row.archiveId)
  } finally {
    archiveDetailLoading.value = false
  }
}

const loadSourceReports = async () => {
  sourceReportLoading.value = true
  try {
    const page = await aiApi.reports({
      pageNum: sourcePage.pageNum,
      pageSize: sourcePage.pageSize,
      targetType: archiveQuery.targetType || undefined,
      targetId: archiveQuery.targetId.trim() || undefined
    })
    sourceReports.value = page.records || []
    sourcePage.total = page.total || 0
  } finally {
    sourceReportLoading.value = false
  }
}

const openSourceReportDialog = async () => {
  sourceReportDialogVisible.value = true
  sourcePage.pageNum = 1
  await loadSourceReports()
}

const handleSourcePageSize = async (pageSize: number) => {
  sourcePage.pageSize = pageSize
  sourcePage.pageNum = 1
  await loadSourceReports()
}

const archiveSourceReport = async (row: OperationReport) => {
  archiveActionLoadingId.value = row.reportId
  try {
    await reportArchiveApi.archive(row.reportId)
    ElMessage.success(t('reports.archiveCreated'))
    sourceReportDialogVisible.value = false
    archivePage.pageNum = 1
    await loadArchives()
  } finally {
    archiveActionLoadingId.value = undefined
  }
}

const updateArchiveStatus = async (row: ReportArchive) => {
  const nextStatus = row.archiveStatus === 'archived' ? 'restored' : 'archived'
  archiveStatusLoadingId.value = row.archiveId
  try {
    await reportArchiveApi.updateStatus(row.archiveId, nextStatus)
    ElMessage.success(t('reports.archiveStatusUpdated'))
    await loadArchives()
  } finally {
    archiveStatusLoadingId.value = undefined
  }
}

const exportArchivePdf = async (row: ReportArchive) => {
  archivePdfLoadingId.value = row.archiveId
  try {
    const blob = await reportArchiveApi.exportPdf(row.archiveId, localeStore.locale)
    const target = (row.targetId || 'archive')
      .replace(/[^A-Za-z0-9._-]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'archive'
    saveBlob(blob, `operations-report-${target}-${row.archiveId}.pdf`)
    ElMessage.success(t('reports.pdfExported'))
  } finally {
    archivePdfLoadingId.value = undefined
  }
}

const archiveSections = computed(() => {
  const report = selectedArchive.value
  if (!report) {
    return []
  }
  return [
    { title: t('reports.painPoints'), content: report.consumerPainPoints },
    { title: t('reports.advantages'), content: report.productAdvantages },
    { title: t('reports.disadvantages'), content: report.productDisadvantages },
    { title: t('reports.operationSuggestions'), content: report.operationSuggestions },
    { title: t('reports.copywritingSuggestions'), content: report.copywritingSuggestions },
    { title: t('reports.serviceSuggestions'), content: report.serviceSuggestions }
  ].filter((item) => item.content)
})

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

onMounted(() => {
  void loadData()
  void loadArchives()
})
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

    <div class="panel section-gap">
      <div class="status-line">
        <div>
          <div class="panel-title">{{ t('reports.archiveTitle') }}</div>
          <span class="muted">{{ t('reports.archiveSubtitle') }}</span>
        </div>
        <div class="archive-heading-actions">
          <el-tag type="success" effect="plain">
            <Archive :size="14" />
            {{ t('reports.archiveStatus') }}
          </el-tag>
          <el-button type="primary" :icon="Archive" @click="openSourceReportDialog">
            {{ t('reports.archiveAction') }}
          </el-button>
        </div>
      </div>

      <div class="archive-toolbar">
        <el-select v-model="archiveQuery.targetType" class="archive-target-select" :placeholder="t('reports.filterTargetType')" clearable>
          <el-option :label="t('reports.allTargets')" value="" />
          <el-option :label="t('reports.targetTypes.product')" value="product" />
          <el-option :label="t('reports.targetTypes.seller')" value="seller" />
        </el-select>
        <el-input
          v-model="archiveQuery.targetId"
          class="archive-target-input"
          :placeholder="t('reports.targetIdPlaceholder')"
          clearable
          @keyup.enter="searchArchives"
        />
        <el-input
          v-model="archiveQuery.keyword"
          class="archive-keyword-input"
          :placeholder="t('reports.archiveKeywordPlaceholder')"
          clearable
          @keyup.enter="searchArchives"
        />
        <el-select
          v-model="archiveQuery.archiveStatus"
          class="archive-status-select"
          :placeholder="t('reports.archiveStatusFilter')"
          clearable
        >
          <el-option :label="t('reports.allStatuses')" value="" />
          <el-option :label="t('reports.archiveStatuses.archived')" value="archived" />
          <el-option :label="t('reports.archiveStatuses.restored')" value="restored" />
        </el-select>
        <el-date-picker
          v-model="archiveQuery.dateRange"
          class="archive-date-range"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          :range-separator="t('common.to')"
          :start-placeholder="t('taskCenter.startTime')"
          :end-placeholder="t('taskCenter.endTime')"
          :aria-label="t('reports.archiveTimeRange')"
        />
        <el-button :icon="Search" type="primary" @click="searchArchives">{{ t('common.search') }}</el-button>
        <el-button @click="resetArchiveFilters">{{ t('common.reset') }}</el-button>
      </div>

      <el-table v-loading="archiveLoading" :data="archivedReports" size="small" height="330">
        <el-table-column prop="archiveId" :label="t('common.id')" width="90" />
        <el-table-column :label="t('reports.archiveReportTitle')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="archive-title-cell">
              <FileText :size="15" />
              <span>{{ row.reportTitle || t('reports.untitledReport') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('reports.target')" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ displayTargetType(row.targetType) }}</el-tag>
            <span class="archive-target-id">{{ row.targetId || t('common.dash') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" :label="t('common.model')" width="130" show-overflow-tooltip />
        <el-table-column :label="t('reports.archiveTime')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.archiveTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.archiveStatus === 'archived' ? 'success' : 'info'" effect="plain">
              {{ displayArchiveStatus(row.archiveStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="330" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Eye" @click="viewArchive(row)">{{ t('reports.viewDetail') }}</el-button>
            <el-button
              size="small"
              :icon="Download"
              :loading="archivePdfLoadingId === row.archiveId"
              @click="exportArchivePdf(row)"
            >
              {{ t('reports.exportPdf') }}
            </el-button>
            <el-button
              size="small"
              :type="row.archiveStatus === 'archived' ? 'default' : 'primary'"
              :icon="row.archiveStatus === 'archived' ? ArchiveRestore : Archive"
              :loading="archiveStatusLoadingId === row.archiveId"
              @click="updateArchiveStatus(row)"
            >
              {{ row.archiveStatus === 'archived' ? t('reports.restoreAction') : t('reports.rearchiveAction') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="archive-pagination">
        <el-pagination
          v-model:current-page="archivePage.pageNum"
          v-model:page-size="archivePage.pageSize"
          :total="archivePage.total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleArchivePageSize"
          @current-change="loadArchives"
        />
      </div>
    </div>

    <el-dialog v-model="archiveDialogVisible" :title="selectedArchive?.reportTitle || t('reports.detailTitle')" width="760px">
      <div v-loading="archiveDetailLoading" class="archive-detail">
        <el-descriptions v-if="selectedArchive" :column="2" border size="small">
          <el-descriptions-item :label="t('common.id')">{{ selectedArchive.archiveId }}</el-descriptions-item>
          <el-descriptions-item :label="t('reports.sourceReportId')">{{ selectedArchive.sourceReportId }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.status')">
            {{ displayArchiveStatus(selectedArchive.archiveStatus) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('reports.target')">
            {{ displayTargetType(selectedArchive.targetType) }} / {{ selectedArchive.targetId || t('common.dash') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('common.model')">{{ selectedArchive.modelName || t('common.dash') }}</el-descriptions-item>
          <el-descriptions-item :label="t('reports.reportCreateTime')">
            {{ formatDateTime(selectedArchive.reportCreateTime) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('reports.archiveTime')">
            {{ formatDateTime(selectedArchive.archiveTime) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('reports.archiveRemark')" :span="2">
            {{ selectedArchive.archiveRemark || t('common.dash') }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-for="section in archiveSections" :key="section.title" class="insight-block">
          <strong>{{ section.title }}</strong>
          <p>{{ section.content }}</p>
        </div>

        <div v-if="selectedArchive?.fullReport" class="insight-block">
          <strong>{{ t('reports.fullReport') }}</strong>
          <p>{{ selectedArchive.fullReport }}</p>
        </div>
      </div>
      <template #footer>
        <el-button
          v-if="selectedArchive"
          type="primary"
          :icon="Download"
          :loading="archivePdfLoadingId === selectedArchive.archiveId"
          @click="exportArchivePdf(selectedArchive)"
        >
          {{ t('reports.exportPdf') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sourceReportDialogVisible" :title="t('reports.archiveAction')" width="860px">
      <p class="source-dialog-hint">{{ t('reports.selectReportSubtitle') }}</p>
      <el-table v-loading="sourceReportLoading" :data="sourceReports" size="small" height="360">
        <el-table-column prop="reportId" :label="t('common.id')" width="90" />
        <el-table-column prop="reportTitle" :label="t('reports.archiveReportTitle')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('reports.target')" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ displayTargetType(row.targetType) }}</el-tag>
            <span class="archive-target-id">{{ row.targetId || t('common.dash') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('reports.reportCreateTime')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :icon="Archive"
              :loading="archiveActionLoadingId === row.reportId"
              @click="archiveSourceReport(row)"
            >
              {{ t('reports.archiveAction') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="archive-pagination">
        <el-pagination
          v-model:current-page="sourcePage.pageNum"
          v-model:page-size="sourcePage.pageSize"
          :total="sourcePage.total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSourcePageSize"
          @current-change="loadSourceReports"
        />
      </div>
    </el-dialog>
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

.archive-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}

.archive-heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.archive-target-select {
  width: 190px;
}

.archive-target-input {
  width: 240px;
}

.archive-keyword-input {
  width: 220px;
}

.archive-status-select {
  width: 170px;
}

.archive-date-range {
  width: 360px;
}

.archive-title-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.archive-title-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.archive-target-id {
  margin-left: 8px;
  color: #475569;
}

.archive-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.archive-detail {
  display: grid;
  gap: 12px;
}

.archive-detail p {
  margin: 8px 0 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.source-dialog-hint {
  margin: 0 0 14px;
  color: #64748b;
}

@media (max-width: 900px) {
  .archive-heading-actions {
    flex-wrap: wrap;
  }

  .archive-target-select,
  .archive-target-input,
  .archive-keyword-input,
  .archive-status-select,
  .archive-date-range {
    width: 100%;
  }
}
</style>
