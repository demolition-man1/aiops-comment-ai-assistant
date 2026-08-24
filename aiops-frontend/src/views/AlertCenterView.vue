<script setup lang="ts">
import { AlertTriangle, Bell, MessageSquareWarning, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { commentApi, dashboardApi } from '@/api/modules'
import type { Comment, Overview } from '@/api/types'
import { formatPercent, toPercentValue } from '@/utils/metricFormat'

const { t } = useI18n()
interface AlertItem {
  title: string
  level: 'danger' | 'warning' | 'success'
  metric: string
  target: string
  detail: string
}

const loading = ref(false)
const overview = reactive<Overview>({
  productCount: 0,
  sellerCount: 0,
  commentCount: 0,
  avgScore: 0,
  negativeRate: 0
})
const negativeComments = ref<Comment[]>([])
const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])

const thresholds = reactive({
  negativeRate: Number(localStorage.getItem('aiops_alert_negative_rate') || 20),
  negativeCount: Number(localStorage.getItem('aiops_alert_negative_count') || 5)
})

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('enums.problemType.pending')
}

const loadData = async () => {
  loading.value = true
  try {
    const [overviewData, comments] = await Promise.all([
      dashboardApi.overview(),
      commentApi.negative({ pageNum: 1, pageSize: 20 })
    ])
    Object.assign(overview, overviewData)
    negativeComments.value = comments.records || []
  } finally {
    loading.value = false
  }
}

const alerts = computed<AlertItem[]>(() => {
  const items: AlertItem[] = []

  const overviewNegativeRate = toPercentValue(overview.negativeRate)
  if (overviewNegativeRate >= thresholds.negativeRate) {
    items.push({
      title: t('alerts.negativeRateHigh'),
      level: 'danger',
      metric: formatPercent(overview.negativeRate),
      target: t('alerts.allStoreReviews'),
      detail: t('alerts.thresholdPercent', { value: thresholds.negativeRate })
    })
  }

  if (negativeComments.value.length >= thresholds.negativeCount) {
    items.push({
      title: t('alerts.recentNegativeBacklog'),
      level: 'warning',
      metric: `${negativeComments.value.length} ${t('common.countSuffix')}`,
      target: t('alerts.negativePool'),
      detail: t('alerts.thresholdCount', { value: thresholds.negativeCount })
    })
  }

  const problemType = negativeComments.value.find((item) => item.effectiveProblemType || item.systemProblemType)
  if (problemType) {
    items.push({
      title: t('alerts.keyProblemAppears'),
      level: 'warning',
      metric: displayProblemType(problemType.effectiveProblemType || problemType.systemProblemType),
      target: problemType.productId || t('alerts.productReviews'),
      detail: problemType.cleanContent || problemType.reviewContent || problemType.reviewTitle || t('common.dash')
    })
  }

  if (!items.length) {
    items.push({
      title: t('alerts.noAlerts'),
      level: 'success',
      metric: t('alerts.normal'),
      target: t('alerts.operationStatus'),
      detail: t('alerts.riskNormal')
    })
  }

  return items
})

const saveThresholds = () => {
  localStorage.setItem('aiops_alert_negative_rate', String(thresholds.negativeRate))
  localStorage.setItem('aiops_alert_negative_count', String(thresholds.negativeCount))
}

const levelType = (level: AlertItem['level']) => {
  if (level === 'danger') return 'danger'
  if (level === 'warning') return 'warning'
  return 'success'
}

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('alerts.title') }}</h2>
        <span class="muted">{{ t('alerts.subtitle') }}</span>
      </div>
      <el-button type="primary" @click="loadData">
        <RefreshCw :size="16" />
        {{ t('alerts.refresh') }}
      </el-button>
    </div>

    <div class="grid metrics">
      <section class="metric-card metric-red">
        <div class="metric-head">
          <span>{{ t('alerts.negativeRate') }}</span>
          <AlertTriangle :size="22" />
        </div>
        <strong>{{ formatPercent(overview.negativeRate) }}</strong>
        <p>{{ t('alerts.thresholdPercent', { value: thresholds.negativeRate }) }}</p>
      </section>
      <section class="metric-card metric-amber">
        <div class="metric-head">
          <span>{{ t('alerts.negativeReviews') }}</span>
          <MessageSquareWarning :size="22" />
        </div>
        <strong>{{ negativeComments.length }}</strong>
        <p>{{ t('alerts.recentNegativeList') }}</p>
      </section>
      <section class="metric-card metric-blue">
        <div class="metric-head">
          <span>{{ t('alerts.commentTotal') }}</span>
          <Bell :size="22" />
        </div>
        <strong>{{ overview.commentCount }}</strong>
        <p>{{ t('alerts.commentBase') }}</p>
      </section>
      <section class="metric-card metric-green">
        <div class="metric-head">
          <span>{{ t('alerts.averageScore') }}</span>
          <ShieldCheck :size="22" />
        </div>
        <strong>{{ Number(overview.avgScore || 0).toFixed(2) }}</strong>
        <p>{{ t('alerts.scoreReference') }}</p>
      </section>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="panel-title">{{ t('alerts.thresholdTitle') }}</div>
        <el-form label-position="top">
          <el-form-item :label="t('alerts.negativeRateThreshold')">
            <el-slider v-model="thresholds.negativeRate" :min="1" :max="80" show-input @change="saveThresholds" />
          </el-form-item>
          <el-form-item :label="t('alerts.negativeCountThreshold')">
            <el-input-number
              v-model="thresholds.negativeCount"
              :min="1"
              :max="100"
              style="width: 180px"
              @change="saveThresholds"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-title">{{ t('alerts.currentAlerts') }}</div>
        <div v-for="item in alerts" :key="`${item.title}-${item.target}`" class="alert-row">
          <el-tag :type="levelType(item.level)" effect="dark">{{ item.metric }}</el-tag>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.target }}｜{{ item.detail }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="panel-title">{{ t('alerts.negativeDetails') }}</div>
      <el-table :data="negativeComments" height="360" size="small">
        <el-table-column prop="reviewScore" :label="t('common.score')" width="80" />
        <el-table-column prop="productId" :label="t('common.productId')" width="180" show-overflow-tooltip />
        <el-table-column :label="t('dashboard.problemType')" width="150">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">
              {{ displayProblemType(row.effectiveProblemType || row.systemProblemType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('dashboard.reviewContent')" min-width="360">
          <template #default="{ row }">
            <span class="comment-cell">{{ row.cleanContent || row.reviewContent || row.reviewTitle || t('common.dash') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" :label="t('common.reviewTime')" width="180" />
      </el-table>
    </div>
  </section>
</template>
