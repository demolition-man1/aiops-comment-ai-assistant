<script setup lang="ts">
import { Bot, Coins, RefreshCw, Timer } from 'lucide-vue-next'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { aiCallLogApi } from '@/api/modules'
import type { AiCallLog, AiCallLogOverview } from '@/api/types'
import MetricCard from '@/components/MetricCard.vue'

const { t } = useI18n()
const loading = ref(false)
const logs = ref<AiCallLog[]>([])
const businessTypes = ['report', 'content', 'negative_reply', 'translation', 'product_compare']

const overview = reactive<AiCallLogOverview>({
  totalCalls: 0,
  successCalls: 0,
  failedCalls: 0,
  successRate: 0,
  totalTokens: 0,
  totalCost: 0,
  avgLatencyMs: 0,
  avgQueueLatencyMs: 0,
  avgTotalLatencyMs: 0
})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  businessType: '',
  callStatus: '',
  targetType: '',
  targetId: ''
})

const page = reactive({
  total: 0
})

const displayBusinessType = (value?: string) => {
  const key = value?.trim()
  return key && businessTypes.includes(key) ? t(`prompts.businessTypes.${key}`) : key || t('common.unknown')
}

const formatCost = (value?: number) => Number(value || 0).toFixed(6)
const formatPercent = (value?: number) => `${Number(value || 0).toFixed(2)}%`
const formatLatency = (value?: number) => `${Number(value || 0)}ms`
const displayErrorCategory = (value?: string) => value
  ? t(`aiLogs.failureCategories.${value}`)
  : t('common.dash')

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      ...query,
      businessType: query.businessType || undefined,
      callStatus: query.callStatus || undefined,
      targetType: query.targetType || undefined,
      targetId: query.targetId || undefined
    }
    const [overviewResult, pageResult] = await Promise.all([
      aiCallLogApi.overview(params),
      aiCallLogApi.page(params)
    ])
    Object.assign(overview, overviewResult)
    logs.value = pageResult.records || []
    page.total = pageResult.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('aiLogs.title') }}</h2>
        <span class="muted">{{ t('aiLogs.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadLogs">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="metric-grid">
      <MetricCard :title="t('aiLogs.totalCalls')" :value="overview.totalCalls" :hint="t('aiLogs.successCalls', { count: overview.successCalls })" tone="blue">
        <Bot :size="22" />
      </MetricCard>
      <MetricCard :title="t('aiLogs.successRate')" :value="formatPercent(overview.successRate)" :hint="t('aiLogs.failedCalls', { count: overview.failedCalls })" tone="green">
        <RefreshCw :size="22" />
      </MetricCard>
      <MetricCard :title="t('aiLogs.totalTokens')" :value="overview.totalTokens" :hint="t('aiLogs.costHint', { cost: formatCost(overview.totalCost) })" tone="amber">
        <Coins :size="22" />
      </MetricCard>
      <MetricCard :title="t('aiLogs.totalLatency')" :value="formatLatency(overview.avgTotalLatencyMs)" :hint="`${t('aiLogs.queueLatency')}: ${formatLatency(overview.avgQueueLatencyMs)} · ${t('aiLogs.providerLatency')}: ${formatLatency(overview.avgLatencyMs)}`" tone="red">
        <Timer :size="22" />
      </MetricCard>
    </div>

    <div class="panel section-gap">
      <el-form :inline="true">
        <el-form-item :label="t('prompts.businessType')">
          <el-select v-model="query.businessType" clearable :placeholder="t('common.all')" style="width: 170px">
            <el-option
              v-for="type in businessTypes"
              :key="type"
              :label="displayBusinessType(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.callStatus" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('aiLogs.success')" value="success" />
            <el-option :label="t('aiLogs.failed')" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('aiLogs.targetType')">
          <el-input v-model="query.targetType" clearable :placeholder="t('aiLogs.targetTypePlaceholder')" style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('aiLogs.targetId')">
          <el-input v-model="query.targetId" clearable :placeholder="t('aiLogs.targetIdPlaceholder')" style="width: 210px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogs">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" height="520" size="small">
        <el-table-column :label="t('prompts.businessType')" width="150">
          <template #default="{ row }">{{ displayBusinessType(row.businessType) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.callStatus === 'success' ? 'success' : 'danger'" effect="plain">
              {{ row.callStatus === 'success' ? t('aiLogs.success') : t('aiLogs.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" :label="t('aiLogs.targetType')" width="130" show-overflow-tooltip />
        <el-table-column prop="targetId" :label="t('aiLogs.targetId')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="jobId" :label="t('aiLogs.jobId')" width="100" />
        <el-table-column prop="modelName" :label="t('common.model')" width="140" show-overflow-tooltip />
        <el-table-column prop="tokenUsage" :label="t('aiLogs.tokenUsage')" width="120" />
        <el-table-column :label="t('aiLogs.estimatedCost')" width="140">
          <template #default="{ row }">{{ formatCost(row.estimatedCost) }}</template>
        </el-table-column>
        <el-table-column :label="t('aiLogs.queueLatency')" width="120">
          <template #default="{ row }">{{ formatLatency(row.queueLatencyMs) }}</template>
        </el-table-column>
        <el-table-column :label="t('aiLogs.providerLatency')" width="120">
          <template #default="{ row }">{{ formatLatency(row.latencyMs) }}</template>
        </el-table-column>
        <el-table-column :label="t('aiLogs.totalLatency')" width="120">
          <template #default="{ row }">{{ formatLatency(row.totalLatencyMs) }}</template>
        </el-table-column>
        <el-table-column :label="t('aiLogs.errorCategory')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ displayErrorCategory(row.errorCode) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createdAt')" width="180" show-overflow-tooltip />
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="section-gap"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="page.total"
        @change="loadLogs"
      />
    </div>
  </section>
</template>
