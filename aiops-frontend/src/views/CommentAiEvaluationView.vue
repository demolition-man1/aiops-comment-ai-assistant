<script setup lang="ts">
import { CirclePlay, ClipboardCheck, RefreshCw, Save, Target } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

import { commentAiShadowApi } from '@/api/modules'
import type { CommentAiEvaluation, CommentAiHybridReadiness, CommentAiShadowResult, CommentAiShadowRun, CommentAiShadowTask } from '@/api/types'
import MetricCard from '@/components/MetricCard.vue'

const { t, locale } = useI18n()

const form = reactive({
  targetType: 'product' as 'product' | 'seller',
  targetId: '',
  sampleSize: 20,
  sampleSeed: 20260830,
  maxTotalTokens: 12000
})

const runQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  targetType: '',
  targetId: '',
  runStatus: ''
})

const resultQuery = reactive({
  pageNum: 1,
  pageSize: 20,
  annotationStatus: 'all'
})

const runs = ref<CommentAiShadowRun[]>([])
const results = ref<CommentAiShadowResult[]>([])
const runTotal = ref(0)
const resultTotal = ref(0)
const selectedRunId = ref<number>()
const activeTask = ref<CommentAiShadowTask>()
const evaluation = ref<CommentAiEvaluation>()
const hybridReadiness = ref<CommentAiHybridReadiness>()
const loading = ref(false)
const starting = ref(false)
const resultLoading = ref(false)
const annotationVisible = ref(false)
const activationConfirmVisible = ref(false)
const activating = ref(false)
const savingCommentId = ref<number>()
const annotationRow = ref<CommentAiShadowResult>()
const annotationForm = reactive({
  manualSentiment: 'negative' as 'positive' | 'neutral' | 'negative',
  manualProblemTypes: [] as string[],
  annotationNote: ''
})

let pollTimer: number | undefined

const selectedRun = computed(() => runs.value.find((item) => item.runId === selectedRunId.value))
const qualityReady = computed(() => evaluation.value?.qualityReady === true)

const isTerminalTask = (status?: string) => ['success', 'partial', 'budget_stopped', 'failed'].includes(status || '')
const isActiveTask = (status?: string) => status === 'pending' || status === 'processing'
const statusType = (status?: string) => {
  if (status === 'success') return 'success'
  if (status === 'processing' || status === 'pending') return 'warning'
  if (status === 'partial' || status === 'budget_stopped') return 'info'
  return 'danger'
}
const formatPercent = (value?: number | null) => value == null ? '—' : (value * 100).toFixed(1) + '%'
const formatDelta = (value?: number | null) => value == null ? '—' : (value >= 0 ? '+' : '') + (value * 100).toFixed(1) + '%'
const formatLabels = (labels?: string[]) => labels?.filter(Boolean).join(', ') || '—'
const displaySentiment = (value?: string) => value ? t('enums.sentiment.' + value) : '—'
const hasAnnotation = (row: CommentAiShadowResult) => Boolean(row.manualSentiment)

const loadRuns = async () => {
  loading.value = true
  try {
    const page = await commentAiShadowApi.runs({
      ...runQuery,
      targetType: runQuery.targetType || undefined,
      targetId: runQuery.targetId || undefined,
      runStatus: runQuery.runStatus || undefined
    })
    runs.value = page.records || []
    runTotal.value = page.total || 0
    if (!selectedRunId.value && runs.value[0]) {
      await selectRun(runs.value[0].runId)
    }
  } finally {
    loading.value = false
  }
}

const loadSelectedRun = async () => {
  if (!selectedRunId.value) {
    results.value = []
    evaluation.value = undefined
    hybridReadiness.value = undefined
    return
  }
  resultLoading.value = true
  try {
    const [page, metrics, readiness] = await Promise.all([
      commentAiShadowApi.results(selectedRunId.value, resultQuery),
      commentAiShadowApi.evaluation(selectedRunId.value),
      commentAiShadowApi.hybridReadiness(selectedRunId.value)
    ])
    results.value = page.records || []
    resultTotal.value = page.total || 0
    evaluation.value = metrics
    hybridReadiness.value = readiness
  } finally {
    resultLoading.value = false
  }
}

const selectRun = async (runId: number) => {
  selectedRunId.value = runId
  resultQuery.pageNum = 1
  await loadSelectedRun()
}

const selectRunRow = (row?: CommentAiShadowRun) => {
  if (row) void selectRun(row.runId)
}

const pollActiveTask = async (taskId: number) => {
  if (pollTimer) window.clearTimeout(pollTimer)
  try {
    const task = await commentAiShadowApi.task(taskId)
    activeTask.value = task
    if (task.runId) selectedRunId.value = task.runId
    if (isActiveTask(task.taskStatus)) {
      pollTimer = window.setTimeout(() => void pollActiveTask(taskId), 3000)
      return
    }
    await loadRuns()
    await loadSelectedRun()
    if (isTerminalTask(task.taskStatus) && task.taskStatus !== 'failed') {
      ElMessage.success(t('aiEvaluation.runCompleted'))
    }
  } catch {
    activeTask.value = undefined
  }
}

const startRun = async () => {
  const targetId = form.targetId.trim()
  if (!targetId) {
    ElMessage.warning(t('aiEvaluation.targetRequired'))
    return
  }
  if (form.sampleSize < 1 || form.sampleSize > 100 || form.maxTotalTokens < 1000 || form.maxTotalTokens > 100000) {
    ElMessage.warning(t('aiEvaluation.invalidRunConfig'))
    return
  }
  starting.value = true
  try {
    const task = await commentAiShadowApi.createTask({
      ...form,
      targetId,
      language: locale.value
    })
    activeTask.value = task
    selectedRunId.value = task.runId
    await loadRuns()
    await loadSelectedRun()
    void pollActiveTask(task.taskId)
    ElMessage.success(t('aiEvaluation.runCreated'))
  } finally {
    starting.value = false
  }
}

const openAnnotation = (row: CommentAiShadowResult) => {
  annotationRow.value = row
  annotationForm.manualSentiment = (row.manualSentiment || row.aiSentiment || row.ruleSentiment || 'negative') as typeof annotationForm.manualSentiment
  annotationForm.manualProblemTypes = [...(row.manualProblemTypes || [])]
  annotationForm.annotationNote = row.annotationNote || ''
  annotationVisible.value = true
}

const saveAnnotation = async () => {
  if (!annotationRow.value) return
  if (annotationForm.manualProblemTypes.length > 5) {
    ElMessage.warning(t('aiEvaluation.tooManyLabels'))
    return
  }
  savingCommentId.value = annotationRow.value.commentId
  try {
    await commentAiShadowApi.upsertAnnotation(annotationRow.value.commentId, {
      manualSentiment: annotationForm.manualSentiment,
      manualProblemTypes: annotationForm.manualProblemTypes.map((item) => item.trim()).filter(Boolean),
      annotationNote: annotationForm.annotationNote.trim() || undefined
    })
    annotationVisible.value = false
    await loadSelectedRun()
    ElMessage.success(t('aiEvaluation.annotationSaved'))
  } finally {
    savingCommentId.value = undefined
  }
}

const openActivationConfirm = () => {
  if (!hybridReadiness.value?.ready) return
  activationConfirmVisible.value = true
}

const activateHybrid = async () => {
  if (!selectedRunId.value) return
  activating.value = true
  try {
    hybridReadiness.value = await commentAiShadowApi.activateHybrid(selectedRunId.value)
    activationConfirmVisible.value = false
    ElMessage.success(t('aiEvaluation.hybridActivated'))
  } finally {
    activating.value = false
  }
}

onMounted(() => void loadRuns())
onBeforeUnmount(() => {
  if (pollTimer) window.clearTimeout(pollTimer)
})
</script>

<template>
  <section class="page evaluation-page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('aiEvaluation.title') }}</h2>
        <span class="muted">{{ t('aiEvaluation.subtitle') }}</span>
      </div>
      <el-button :icon="RefreshCw" @click="loadRuns">{{ t('common.refresh') }}</el-button>
    </div>

    <section class="evaluation-section">
      <div class="evaluation-section-head">
        <div>
          <h3>{{ t('aiEvaluation.runToolbar') }}</h3>
          <p>{{ t('aiEvaluation.runToolbarHint') }}</p>
        </div>
      </div>
      <el-form class="evaluation-run-form" :inline="true" @submit.prevent>
        <el-form-item :label="t('aiEvaluation.targetType')">
          <el-select v-model="form.targetType" style="width: 128px">
            <el-option :label="t('aiEvaluation.product')" value="product" />
            <el-option :label="t('aiEvaluation.seller')" value="seller" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.targetId')">
          <el-input v-model="form.targetId" :placeholder="t('aiEvaluation.targetIdPlaceholder')" style="width: 210px" />
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.sampleSize')">
          <el-input-number v-model="form.sampleSize" :min="1" :max="100" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.sampleSeed')">
          <el-input-number v-model="form.sampleSeed" :min="0" :max="2147483647" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.tokenBudget')">
          <el-input-number v-model="form.maxTotalTokens" :min="1000" :max="100000" :step="1000" controls-position="right" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="CirclePlay" :loading="starting" @click="startRun">
            {{ t('aiEvaluation.startRun') }}
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="activeTask && isActiveTask(activeTask.taskStatus)" class="section-gap" type="info" :closable="false">
        <template #title>
          {{ t('aiEvaluation.taskProgress', { progress: activeTask.progress || 0, count: activeTask.actualSampleSize || 0 }) }}
        </template>
      </el-alert>
    </section>

    <section class="evaluation-section section-gap" v-loading="loading">
      <div class="evaluation-section-head">
        <div>
          <h3>{{ t('aiEvaluation.runHistory') }}</h3>
          <p>{{ t('aiEvaluation.runHistoryHint') }}</p>
        </div>
        <el-form :inline="true" class="evaluation-filter">
          <el-form-item :label="t('common.status')">
            <el-select v-model="runQuery.runStatus" clearable style="width: 136px" @change="loadRuns">
              <el-option :label="t('aiEvaluation.processing')" value="processing" />
              <el-option :label="t('aiEvaluation.success')" value="success" />
              <el-option :label="t('aiEvaluation.partial')" value="partial" />
              <el-option :label="t('aiEvaluation.failed')" value="failed" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <el-table :data="runs" highlight-current-row row-key="runId" @current-change="selectRunRow">
        <el-table-column prop="runId" label="ID" width="82" />
        <el-table-column :label="t('aiEvaluation.target')" min-width="170">
          <template #default="{ row }">{{ row.targetType }} / {{ row.targetId }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="132">
          <template #default="{ row }"><el-tag :type="statusType(row.runStatus)" effect="plain">{{ row.runStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="t('aiEvaluation.sample')" width="112">
          <template #default="{ row }">{{ row.actualSampleSize }} / {{ row.requestedSampleSize }}</template>
        </el-table-column>
        <el-table-column prop="totalCalls" :label="t('aiEvaluation.calls')" width="92" />
        <el-table-column prop="totalTokens" :label="t('aiEvaluation.tokens')" width="104" />
        <el-table-column prop="endTime" :label="t('aiEvaluation.completedAt')" width="176" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="runQuery.pageNum"
        v-model:page-size="runQuery.pageSize"
        class="section-gap"
        background
        layout="total, prev, pager, next"
        :total="runTotal"
        @change="loadRuns"
      />
    </section>

    <section class="evaluation-section section-gap" v-loading="resultLoading">
      <div class="evaluation-section-head">
        <div>
          <h3>{{ t('aiEvaluation.annotationTable') }}</h3>
          <p>{{ selectedRun ? t('aiEvaluation.selectedRun', { id: selectedRun.runId }) : t('aiEvaluation.selectRunHint') }}</p>
        </div>
        <el-select v-model="resultQuery.annotationStatus" :disabled="!selectedRunId" style="width: 154px" @change="() => { resultQuery.pageNum = 1; loadSelectedRun() }">
          <el-option :label="t('common.all')" value="all" />
          <el-option :label="t('aiEvaluation.annotated')" value="annotated" />
          <el-option :label="t('aiEvaluation.unannotated')" value="unannotated" />
        </el-select>
      </div>
      <el-table :data="results" height="540" size="small">
        <el-table-column prop="sampleOrder" :label="t('aiEvaluation.sample')" width="76" />
        <el-table-column :label="t('common.score')" width="72">
          <template #default="{ row }">{{ row.reviewScore ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="reviewContent" :label="t('aiEvaluation.review')" min-width="240" show-overflow-tooltip />
        <el-table-column :label="t('aiEvaluation.ruleOutput')" min-width="150">
          <template #default="{ row }">
            <div>{{ displaySentiment(row.ruleSentiment) }}</div>
            <span class="muted">{{ row.ruleProblemType || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('aiEvaluation.aiOutput')" min-width="172">
          <template #default="{ row }">
            <div>{{ displaySentiment(row.aiSentiment) }}</div>
            <span class="muted">{{ formatLabels(row.aiProblems) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('aiEvaluation.evidence')" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.aiEvidence || row.errorMessage || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('aiEvaluation.callStatus')" width="118">
          <template #default="{ row }"><el-tag :type="statusType(row.callStatus)" effect="plain">{{ row.callStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="t('aiEvaluation.annotation')" min-width="172">
          <template #default="{ row }">
            <template v-if="hasAnnotation(row)">
              <div>{{ displaySentiment(row.manualSentiment) }}</div>
              <span class="muted">{{ formatLabels(row.manualProblemTypes) }}</span>
            </template>
            <span v-else class="muted">{{ t('aiEvaluation.unannotated') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="96" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="savingCommentId === row.commentId" @click="openAnnotation(row)">
              {{ hasAnnotation(row) ? t('common.edit') : t('aiEvaluation.annotate') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="resultQuery.pageNum"
        v-model:page-size="resultQuery.pageSize"
        class="section-gap"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[20, 50, 100]"
        :total="resultTotal"
        :disabled="!selectedRunId"
        @change="loadSelectedRun"
      />
    </section>

    <section class="evaluation-section section-gap">
      <div class="evaluation-section-head">
        <div>
          <h3>{{ t('aiEvaluation.evaluationReport') }}</h3>
          <p>{{ t('aiEvaluation.evaluationHint') }}</p>
        </div>
      </div>
      <el-alert
        v-if="!qualityReady"
        type="warning"
        :closable="false"
        :title="t('aiEvaluation.qualityPending')"
      />
      <el-alert
        v-else
        type="success"
        :closable="false"
        :title="t('aiEvaluation.qualityReady')"
      />
      <template v-if="evaluation">
        <div class="metric-grid section-gap">
          <MetricCard :title="t('aiEvaluation.annotationCoverage')" :value="formatPercent(evaluation.annotationCoverage)" :hint="t('aiEvaluation.annotatedCount', { count: evaluation.annotatedCount, total: evaluation.sampleCount })" tone="blue"><ClipboardCheck :size="22" /></MetricCard>
          <MetricCard :title="t('aiEvaluation.callSuccessRate')" :value="formatPercent(evaluation.callSuccessRate)" :hint="t('aiEvaluation.callCount', { success: evaluation.successfulCallCount, total: evaluation.attemptedCallCount })" tone="green"><Target :size="22" /></MetricCard>
          <MetricCard :title="t('aiEvaluation.jsonValidRate')" :value="formatPercent(evaluation.jsonValidRate)" :hint="t('aiEvaluation.evidenceValidRate', { rate: formatPercent(evaluation.evidenceValidRate) })" tone="amber"><ClipboardCheck :size="22" /></MetricCard>
          <MetricCard :title="t('aiEvaluation.averageLatency')" :value="String(evaluation.averageLatencyMs?.toFixed?.(0) || 0) + 'ms'" :hint="t('aiEvaluation.tokenSummary', { tokens: evaluation.totalTokens, estimated: evaluation.estimatedTokenRowCount })" tone="red"><RefreshCw :size="22" /></MetricCard>
        </div>
        <el-table class="section-gap" :data="[
          { name: t('aiEvaluation.sentimentAccuracy'), rule: evaluation.rule?.sentimentAccuracy, ai: evaluation.ai?.sentimentAccuracy, delta: evaluation.delta?.sentimentAccuracy },
          { name: t('aiEvaluation.problemMicroF1'), rule: evaluation.rule?.problemMicroF1, ai: evaluation.ai?.problemMicroF1, delta: evaluation.delta?.problemMicroF1 },
          { name: t('aiEvaluation.problemMacroF1'), rule: evaluation.rule?.problemMacroF1, ai: evaluation.ai?.problemMacroF1, delta: evaluation.delta?.problemMacroF1 }
        ]" size="small">
          <el-table-column prop="name" :label="t('aiEvaluation.metric')" min-width="190" />
          <el-table-column :label="t('aiEvaluation.rule')" min-width="130"><template #default="{ row }">{{ formatPercent(row.rule) }}</template></el-table-column>
          <el-table-column :label="t('aiEvaluation.ai')" min-width="130"><template #default="{ row }">{{ formatPercent(row.ai) }}</template></el-table-column>
          <el-table-column :label="t('aiEvaluation.delta')" min-width="130"><template #default="{ row }">{{ formatDelta(row.delta) }}</template></el-table-column>
        </el-table>
      </template>
      <el-empty v-else :description="t('aiEvaluation.selectRunHint')" :image-size="72" />
    </section>

    <section class="evaluation-section section-gap">
      <div class="evaluation-section-head">
        <div>
          <h3>{{ t('aiEvaluation.hybridActivation') }}</h3>
          <p>{{ t('aiEvaluation.hybridActivationHint') }}</p>
        </div>
        <el-button type="primary" :disabled="!hybridReadiness?.ready" @click="openActivationConfirm">
          {{ t('aiEvaluation.activateHybrid') }}
        </el-button>
      </div>
      <template v-if="hybridReadiness">
        <el-alert
          :type="hybridReadiness.ready ? 'success' : 'warning'"
          :closable="false"
          :title="hybridReadiness.ready ? t('aiEvaluation.hybridReady') : t('aiEvaluation.hybridBlocked')"
        />
        <div class="hybrid-status section-gap">
          <el-tag effect="plain">{{ t('aiEvaluation.hybridMode', { mode: hybridReadiness.mode }) }}</el-tag>
          <el-tag effect="plain">{{ t('aiEvaluation.eligibleDecisions', { count: hybridReadiness.eligibleDecisionCount }) }}</el-tag>
          <el-tag effect="plain">{{ t('aiEvaluation.activeDecisions', { count: hybridReadiness.activeDecisionCount }) }}</el-tag>
        </div>
        <div v-if="hybridReadiness.failures.length" class="hybrid-failures">
          <el-tag v-for="failure in hybridReadiness.failures" :key="failure" type="danger" effect="plain">
            {{ t('aiEvaluation.gateFailures.' + failure) }}
          </el-tag>
        </div>
      </template>
      <el-empty v-else :description="t('aiEvaluation.selectRunHint')" :image-size="72" />
    </section>

    <el-dialog v-model="annotationVisible" :title="t('aiEvaluation.annotationDialog')" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item :label="t('aiEvaluation.manualSentiment')">
          <el-radio-group v-model="annotationForm.manualSentiment">
            <el-radio-button label="positive">{{ t('enums.sentiment.positive') }}</el-radio-button>
            <el-radio-button label="neutral">{{ t('enums.sentiment.neutral') }}</el-radio-button>
            <el-radio-button label="negative">{{ t('enums.sentiment.negative') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.manualLabels')">
          <el-select v-model="annotationForm.manualProblemTypes" multiple filterable allow-create default-first-option :multiple-limit="5" style="width: 100%" :placeholder="t('aiEvaluation.manualLabelsPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('aiEvaluation.annotationNote')">
          <el-input v-model="annotationForm.annotationNote" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="annotationVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :icon="Save" :loading="Boolean(savingCommentId)" @click="saveAnnotation">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="activationConfirmVisible" :title="t('aiEvaluation.activationConfirmTitle')" width="480px" destroy-on-close>
      <p class="dialog-copy">{{ t('aiEvaluation.activationConfirmBody') }}</p>
      <template #footer>
        <el-button @click="activationConfirmVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="activating" @click="activateHybrid">{{ t('aiEvaluation.activationConfirmAction') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>
