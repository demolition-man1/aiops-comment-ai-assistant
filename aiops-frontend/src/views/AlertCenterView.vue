<script setup lang="ts">
import { AlertTriangle, Bell, MessageSquareWarning, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'

import { commentApi, dashboardApi } from '@/api/modules'
import type { Comment, Overview } from '@/api/types'
import { formatPercent, toPercentValue } from '@/utils/metricFormat'

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

const thresholds = reactive({
  negativeRate: Number(localStorage.getItem('aiops_alert_negative_rate') || 20),
  negativeCount: Number(localStorage.getItem('aiops_alert_negative_count') || 5)
})

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
      title: '负面评论占比过高',
      level: 'danger',
      metric: formatPercent(overview.negativeRate),
      target: '全店评论',
      detail: `当前阈值 ${thresholds.negativeRate}%`
    })
  }

  if (negativeComments.value.length >= thresholds.negativeCount) {
    items.push({
      title: '近期负面评论积压',
      level: 'warning',
      metric: `${negativeComments.value.length} 条`,
      target: '负面评论池',
      detail: `当前阈值 ${thresholds.negativeCount} 条`
    })
  }

  const problemType = negativeComments.value.find((item) => item.effectiveProblemType || item.systemProblemType)
  if (problemType) {
    items.push({
      title: '重点问题类型出现',
      level: 'warning',
      metric: problemType.effectiveProblemType || problemType.systemProblemType || '待识别',
      target: problemType.productId || '商品评论',
      detail: problemType.cleanContent || problemType.reviewContent || problemType.reviewTitle || '-'
    })
  }

  if (!items.length) {
    items.push({
      title: '暂无触发告警',
      level: 'success',
      metric: '正常',
      target: '运营状态',
      detail: '当前评论风险未超过阈值'
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
        <h2 class="section-title">告警中心</h2>
        <span class="muted">基于实时概览和负面评论阈值识别运营风险</span>
      </div>
      <el-button type="primary" @click="loadData">
        <RefreshCw :size="16" />
        刷新告警
      </el-button>
    </div>

    <div class="grid metrics">
      <section class="metric-card metric-red">
        <div class="metric-head">
          <span>负面占比</span>
          <AlertTriangle :size="22" />
        </div>
        <strong>{{ formatPercent(overview.negativeRate) }}</strong>
        <p>阈值 {{ thresholds.negativeRate }}%</p>
      </section>
      <section class="metric-card metric-amber">
        <div class="metric-head">
          <span>负面评论</span>
          <MessageSquareWarning :size="22" />
        </div>
        <strong>{{ negativeComments.length }}</strong>
        <p>最近负面评论列表</p>
      </section>
      <section class="metric-card metric-blue">
        <div class="metric-head">
          <span>评论总量</span>
          <Bell :size="22" />
        </div>
        <strong>{{ overview.commentCount }}</strong>
        <p>全量评论基数</p>
      </section>
      <section class="metric-card metric-green">
        <div class="metric-head">
          <span>平均评分</span>
          <ShieldCheck :size="22" />
        </div>
        <strong>{{ Number(overview.avgScore || 0).toFixed(2) }}</strong>
        <p>评分稳定性参考</p>
      </section>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="panel-title">告警阈值</div>
        <el-form label-position="top">
          <el-form-item label="负面占比阈值">
            <el-slider v-model="thresholds.negativeRate" :min="1" :max="80" show-input @change="saveThresholds" />
          </el-form-item>
          <el-form-item label="负面评论数量阈值">
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
        <div class="panel-title">当前告警</div>
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
      <div class="panel-title">负面评论明细</div>
      <el-table :data="negativeComments" height="360" size="small">
        <el-table-column prop="reviewScore" label="评分" width="80" />
        <el-table-column prop="productId" label="商品ID" width="180" show-overflow-tooltip />
        <el-table-column label="问题类型" width="150">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">
              {{ row.effectiveProblemType || row.systemProblemType || '待识别' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="360">
          <template #default="{ row }">
            <span class="comment-cell">{{ row.cleanContent || row.reviewContent || row.reviewTitle || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="评论时间" width="180" />
      </el-table>
    </div>
  </section>
</template>
