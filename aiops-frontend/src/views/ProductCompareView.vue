<script setup lang="ts">
import { GitCompareArrows, RefreshCw, Sparkles } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import AiJobProgressPanel from '@/components/AiJobProgressPanel.vue'
import { aiJobApi, analysisApi, productApi } from '@/api/modules'
import type { AiJob, Product, ProductCompareReport } from '@/api/types'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()
const loading = ref(false)
const products = ref<Product[]>([])
const reports = ref<ProductCompareReport[]>([])
const currentReport = ref<ProductCompareReport>()
const currentJob = ref<AiJob>()
const form = reactive({
  leftProductId: '',
  rightProductId: '',
  forceRefresh: false
})

const loadProducts = async () => {
  const data = await productApi.page({ pageNum: 1, pageSize: 50 })
  products.value = data.records || []
}

const loadReports = async () => {
  const data = await analysisApi.comparePage({ pageNum: 1, pageSize: 8 })
  reports.value = data.records || []
}

const compare = async () => {
  if (!form.leftProductId || !form.rightProductId) {
    ElMessage.warning(t('compare.selectTwoWarning'))
    return
  }
  if (form.leftProductId === form.rightProductId) {
    ElMessage.warning(t('compare.sameProductWarning'))
    return
  }

  loading.value = true
  try {
    const job = await aiJobApi.createProductCompare({
      leftProductId: form.leftProductId,
      rightProductId: form.rightProductId,
      language: localeStore.locale,
      forceRefresh: form.forceRefresh
    }, crypto.randomUUID())
    currentJob.value = await aiJobApi.job(job.jobId)
    ElMessage.success(t('jobs.created', { jobId: job.jobId }))
  } finally {
    loading.value = false
  }
}

const openCompareResult = async (job: AiJob) => {
  if (job.resultType !== 'product_compare' || !job.resultId) return
  await loadReports()
  currentReport.value = reports.value.find(item => item.reportId === job.resultId)
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadReports()])
})
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('compare.title') }}</h2>
        <span class="muted">{{ t('compare.subtitle') }}</span>
      </div>
      <el-button @click="loadReports">
        <RefreshCw :size="16" />
        {{ t('common.refreshHistory') }}
      </el-button>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item :label="t('compare.productA')">
          <el-select v-model="form.leftProductId" filterable clearable :placeholder="t('compare.selectProduct')" style="width: 300px">
            <el-option
              v-for="item in products"
              :key="item.productId"
              :label="`${item.productId} ${item.categoryName || ''}`"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('compare.productB')">
          <el-select v-model="form.rightProductId" filterable clearable :placeholder="t('compare.selectProduct')" style="width: 300px">
            <el-option
              v-for="item in products"
              :key="item.productId"
              :label="`${item.productId} ${item.categoryName || ''}`"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.forceRefresh">{{ t('compare.forceRefresh') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="compare">
            <GitCompareArrows :size="16" />
            {{ t('compare.start') }}
          </el-button>
        </el-form-item>
      </el-form>
      <AiJobProgressPanel v-if="currentJob" class="section-gap" :job="currentJob" @result="openCompareResult" />
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="panel-title">{{ t('compare.currentReport') }}</div>
        <el-empty v-if="!currentReport" :description="t('compare.emptyReport')" />
        <template v-else>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item :label="t('compare.productA')">{{ currentReport.leftProductId }}</el-descriptions-item>
            <el-descriptions-item :label="t('compare.productB')">{{ currentReport.rightProductId }}</el-descriptions-item>
            <el-descriptions-item :label="t('common.model')">{{ currentReport.modelName || t('common.dash') }}</el-descriptions-item>
          </el-descriptions>
          <div class="insight-block section-gap">
            <strong>{{ t('compare.summary') }}</strong>
            <p>{{ currentReport.compareSummary || t('common.dash') }}</p>
          </div>
          <div class="insight-block">
            <strong>{{ t('compare.advantage') }}</strong>
            <p>{{ currentReport.advantageAnalysis || t('common.dash') }}</p>
          </div>
          <div class="insight-block">
            <strong>{{ t('compare.risk') }}</strong>
            <p>{{ currentReport.riskAnalysis || t('common.dash') }}</p>
          </div>
          <div class="insight-block">
            <strong>{{ t('compare.suggestion') }}</strong>
            <p>{{ currentReport.operationSuggestions || t('common.dash') }}</p>
          </div>
        </template>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('compare.history') }}</div>
          <Sparkles class="text-amber" :size="20" />
        </div>
        <el-table :data="reports" height="430" size="small">
          <el-table-column prop="leftProductId" :label="t('compare.productALabel')" show-overflow-tooltip />
          <el-table-column prop="rightProductId" :label="t('compare.productBLabel')" show-overflow-tooltip />
          <el-table-column prop="createTime" :label="t('common.createdAt')" width="160" />
          <el-table-column :label="t('common.action')" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="currentReport = row">{{ t('common.view') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>
