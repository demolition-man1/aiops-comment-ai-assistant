<script setup lang="ts">
import { Bot, ClipboardCheck, Copy, Languages, MessageCircleReply, RefreshCw, Tags } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import { aiApi, analysisApi, commentApi, pollTask, problemSolutionApi, tagApi } from '@/api/modules'
import type {
  AnalysisResult,
  Comment,
  CommentTranslation,
  CustomTag,
  NegativeReply,
  OperationReport,
  ProblemSolution,
  RagReference,
  Task
} from '@/api/types'
import { resolveAnalysisProductId } from '@/utils/analysisTarget'
import { AnalysisWorkflowError, runAnalysisWorkflow } from '@/utils/analysisWorkflow'
import { formatPercent } from '@/utils/metricFormat'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const router = useRouter()
const localeStore = useLocaleStore()
const loading = ref(false)
const taskLoading = ref(false)
const fullWorkflowLoading = ref(false)
const reportLoading = ref(false)
const comments = ref<Comment[]>([])
const total = ref(0)
const selected = ref<Comment>()
const task = ref<Task>()
const analysis = ref<AnalysisResult>()
const report = ref<OperationReport>()
const reply = ref<NegativeReply>()
const replyHistory = ref<NegativeReply[]>([])
const replyHistoryLoading = ref(false)
const replySource = ref<Comment>()
const replyDialogVisible = ref(false)
const replyLoadingId = ref<number>()
const translation = ref<CommentTranslation>()
const translationSource = ref<Comment>()
const translationDialogVisible = ref(false)
const translationLoadingId = ref<number>()
const activeTags = ref<CustomTag[]>([])
const recommendedSolutions = ref<ProblemSolution[]>([])
const solutionLoading = ref(false)
const stopPolling = ref<(() => void) | null>(null)
const sentimentTypes = new Set(['positive', 'neutral', 'negative'])
const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])
const workflowBusy = computed(() => taskLoading.value || fullWorkflowLoading.value)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  productId: '',
  sentiment: '',
  isNegative: ''
})

const tagDialog = reactive({
  visible: false,
  commentId: 0,
  manualProblemType: '',
  customTags: [] as string[]
})

const displaySentiment = (value?: string) => {
  const key = value?.trim()
  return key && sentimentTypes.has(key) ? t(`enums.sentiment.${key}`) : key || t('common.dash')
}

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.has(key) ? t(`enums.problemType.${key}`) : key || t('enums.problemType.unclassified')
}

const loadComments = async () => {
  loading.value = true
  try {
    const data = await commentApi.page({
      ...query,
      isNegative: query.isNegative === '' ? undefined : Number(query.isNegative)
    })
    comments.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadActiveTags = async () => {
  activeTags.value = await tagApi.active()
}

const loadReplyHistory = async () => {
  replyHistoryLoading.value = true
  try {
    const data = await aiApi.negativeReplies({ pageNum: 1, pageSize: 8 })
    replyHistory.value = data.records || []
  } finally {
    replyHistoryLoading.value = false
  }
}

const loadRecommendations = async (row?: Comment) => {
  if (!row) {
    recommendedSolutions.value = []
    return
  }
  const problemType = row.effectiveProblemType || row.manualProblemType || row.systemProblemType
  const keyword = displayCommentContent(row)
  if (!problemType && !keyword) {
    recommendedSolutions.value = []
    return
  }
  solutionLoading.value = true
  try {
    recommendedSolutions.value = await problemSolutionApi.recommend({
      problemType,
      keyword: problemType ? undefined : keyword
    })
  } finally {
    solutionLoading.value = false
  }
}

const selectComment = (row?: Comment) => {
  if (!row) {
    return
  }
  selected.value = row
  void loadRecommendations(row)
}

const resolveWorkflowProductId = () =>
  resolveAnalysisProductId({
    queryProductId: query.productId,
    selectedProductId: selected.value?.productId,
    firstVisibleProductId: comments.value[0]?.productId
  })

const waitForAnalysisTask = (taskId: number, onTaskUpdate: (latestTask: Task) => void) =>
  new Promise<Task>((resolve, reject) => {
    let settled = false
    let cancelPolling: () => void = () => undefined

    const finish = (action: () => void) => {
      if (settled) {
        return
      }
      settled = true
      cancelPolling()
      stopPolling.value = null
      action()
    }

    stopPolling.value?.()
    cancelPolling = pollTask(
      () => analysisApi.task(taskId),
      (latestTask) => {
        onTaskUpdate(latestTask)
        if (latestTask.taskStatus === 'success') {
          finish(() => resolve(latestTask))
        } else if (latestTask.taskStatus === 'failed') {
          finish(() => reject(new Error(latestTask.errorMessage || t('comments.analysisFailed'))))
        }
      },
      3000,
      (error) => finish(() => reject(error))
    )
    stopPolling.value = cancelPolling
  })

const runReviewWorkflow = async (includeReport: boolean) => {
  const productId = resolveWorkflowProductId()
  if (!productId) {
    ElMessage.warning(t('comments.selectProductWarning'))
    return
  }

  query.productId = productId
  taskLoading.value = !includeReport
  fullWorkflowLoading.value = includeReport
  analysis.value = undefined
  report.value = undefined
  try {
    const result = await runAnalysisWorkflow<Task, AnalysisResult, OperationReport>({
      productId,
      includeReport,
      dependencies: {
        createTask: (targetId) => analysisApi.createTask({
          targetType: 'product',
          targetId,
          analysisType: 'comment'
        }),
        waitForTask: waitForAnalysisTask,
        loadAnalysis: (targetId) => analysisApi.product(targetId),
        generateReport: (targetId) => aiApi.productReport({
          productId: targetId,
          language: localeStore.locale
        })
      },
      callbacks: {
        onTaskUpdate: (latestTask) => {
          task.value = latestTask
        },
        onAnalysisLoaded: (latestAnalysis) => {
          analysis.value = latestAnalysis
        }
      }
    })
    task.value = result.task
    analysis.value = result.analysis
    report.value = result.report
    ElMessage.success(t(includeReport ? 'comments.analysisAndReportDone' : 'comments.analysisDone'))
  } catch (error) {
    if (error instanceof AnalysisWorkflowError && error.stage === 'generate-report') {
      ElMessage.error(t('comments.analysisDoneReportFailed'))
    } else if (
      error instanceof AnalysisWorkflowError
      && error.stage === 'wait-task'
      && task.value?.taskStatus === 'failed'
    ) {
      ElMessage.error(task.value.errorMessage || t('comments.analysisFailed'))
    }
  } finally {
    taskLoading.value = false
    fullWorkflowLoading.value = false
  }
}

const analyzeOnly = () => runReviewWorkflow(false)

const analyzeAndGenerateReport = () => runReviewWorkflow(true)

const generateReport = async () => {
  const productId = resolveWorkflowProductId()
  if (!productId) {
    ElMessage.warning(t('comments.specifyProductWarning'))
    return
  }
  query.productId = productId
  reportLoading.value = true
  try {
    report.value = await aiApi.productReport({ productId, language: localeStore.locale })
    ElMessage.success(t('comments.reportGenerated'))
  } finally {
    reportLoading.value = false
  }
}

const generateReply = async () => {
  const target = selected.value
  if (!target?.id) {
    ElMessage.warning(t('comments.selectCommentWarning'))
    return
  }
  replyLoadingId.value = target.id
  try {
    replySource.value = target
    reply.value = await aiApi.negativeReply({
      commentId: target.id,
      toneType: 'professional',
      language: localeStore.locale
    })
    void loadReplyHistory()
    replyDialogVisible.value = true
    ElMessage.success(t('comments.replyGenerated'))
  } finally {
    replyLoadingId.value = undefined
  }
}

const generateReplyFor = async (row: Comment) => {
  selectComment(row)
  await generateReply()
}

const translateCommentFor = async (row: Comment) => {
  if (!row.id) {
    ElMessage.warning(t('comments.selectCommentWarning'))
    return
  }
  selectComment(row)
  translationLoadingId.value = row.id
  try {
    translationSource.value = row
    translation.value = await commentApi.translate(row.id, { language: localeStore.locale })
    translationDialogVisible.value = true
    ElMessage.success(t('comments.translationGenerated'))
  } finally {
    translationLoadingId.value = undefined
  }
}

const isMeaningfulText = (value?: string) => {
  const compact = (value || '').replace(/\s+/g, '').toLowerCase()
  return Boolean(compact && !['nan', 'nannan', 'null', 'none'].includes(compact))
}

const displayCommentContent = (row?: Comment) => {
  if (!row) {
    return t('common.dash')
  }
  if (isMeaningfulText(row.cleanContent)) {
    return row.cleanContent
  }
  if (isMeaningfulText(row.reviewContent)) {
    return row.reviewContent
  }
  if (isMeaningfulText(row.reviewTitle)) {
    return row.reviewTitle
  }
  return t('comments.originalMissing', {
    score: row.reviewScore ?? t('common.dash'),
    tag: displayProblemType(row.effectiveProblemType || row.systemProblemType)
  })
}

const copyReply = async () => {
  if (!reply.value?.replyContent) {
    return
  }
  await navigator.clipboard.writeText(reply.value.replyContent)
  ElMessage.success(t('comments.replyCopied'))
}

const formatRagReference = (reference: RagReference) =>
  reference.title || `${t(`comments.ragSourceTypes.${reference.sourceType}`)} #${reference.sourceId}`

const isProblemSolutionReference = (reference: RagReference) => reference.sourceType === 'problem_solution'

const openSolutionReference = (reference: RagReference) => {
  if (!isProblemSolutionReference(reference)) {
    return
  }
  void router.push({
    name: 'solutions',
    query: { keyword: reference.title || String(reference.sourceId) }
  })
}

const copyTranslation = async () => {
  if (!translation.value?.translatedContent) {
    return
  }
  await navigator.clipboard.writeText(translation.value.translatedContent)
  ElMessage.success(t('comments.translationCopied'))
}

const openTagDialog = (row: Comment) => {
  tagDialog.visible = true
  tagDialog.commentId = row.id
  tagDialog.manualProblemType = row.manualProblemType || row.effectiveProblemType || row.systemProblemType || ''
  tagDialog.customTags = [...(row.customTags || [])]
}

const saveTags = async () => {
  await commentApi.updateTags(tagDialog.commentId, {
    manualProblemType: tagDialog.manualProblemType,
    customTags: tagDialog.customTags.map((item) => item.trim()).filter(Boolean)
  })
  tagDialog.visible = false
  ElMessage.success(t('comments.tagsSaved'))
  await loadComments()
}

onMounted(async () => {
  await Promise.all([loadComments(), loadActiveTags(), loadReplyHistory()])
})

onBeforeUnmount(() => {
  stopPolling.value?.()
})
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('comments.title') }}</h2>
        <span class="muted">{{ t('comments.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadComments">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
        <el-button :loading="taskLoading" :disabled="fullWorkflowLoading || reportLoading" @click="analyzeOnly">
          <Bot :size="16" />
          {{ t('comments.analyzeOnly') }}
        </el-button>
        <el-button
          type="primary"
          :loading="fullWorkflowLoading"
          :disabled="taskLoading || reportLoading"
          @click="analyzeAndGenerateReport"
        >
          <ClipboardCheck :size="16" />
          {{ t('comments.analyzeAndGenerateReport') }}
        </el-button>
      </div>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item :label="t('common.productId')">
          <el-input v-model="query.productId" clearable :placeholder="t('comments.productPlaceholder')" style="width: 260px" />
        </el-form-item>
        <el-form-item :label="t('comments.sentiment')">
          <el-select v-model="query.sentiment" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('enums.sentiment.positive')" value="positive" />
            <el-option :label="t('enums.sentiment.neutral')" value="neutral" />
            <el-option :label="t('enums.sentiment.negative')" value="negative" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('comments.negative')">
          <el-select v-model="query.isNegative" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('common.yes')" value="1" />
            <el-option :label="t('common.no')" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadComments">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="comments"
        height="420"
        highlight-current-row
        @current-change="selectComment"
      >
        <el-table-column prop="reviewScore" :label="t('common.score')" width="72" />
        <el-table-column prop="sentiment" :label="t('comments.sentiment')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.sentiment === 'negative' ? 'danger' : row.sentiment === 'positive' ? 'success' : 'info'">
              {{ displaySentiment(row.sentiment) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productId" :label="t('common.productId')" width="170" show-overflow-tooltip />
        <el-table-column :label="t('comments.problemTag')" width="190">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag size="small" type="warning" effect="plain">
                {{ displayProblemType(row.effectiveProblemType || row.systemProblemType) }}
              </el-tag>
              <el-tag v-for="tag in row.customTags || []" :key="tag" size="small" effect="plain">
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('comments.reviewContent')" min-width="280">
          <template #default="{ row }">
            <span class="comment-cell">{{ displayCommentContent(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="340" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openTagDialog(row)">
                <Tags :size="14" />
                {{ t('comments.tag') }}
              </el-button>
              <el-button
                size="small"
                plain
                :loading="translationLoadingId === row.id"
                @click="translateCommentFor(row)"
              >
                <Languages :size="14" />
                {{ t('comments.translate') }}
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                :loading="replyLoadingId === row.id"
                @click="generateReplyFor(row)"
              >
                <MessageCircleReply :size="14" />
                {{ t('comments.reply') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="status-line section-gap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @change="loadComments"
        />
        <el-button type="success" :loading="reportLoading" :disabled="workflowBusy" @click="generateReport">
          <ClipboardCheck :size="16" />
          {{ t('comments.generateReport') }}
        </el-button>
      </div>
    </div>

    <div class="grid two section-gap">
      <div id="task-center" class="panel">
        <div class="panel-title">{{ t('comments.taskMetrics') }}</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="t('common.taskStatus')">{{ task?.taskStatus || t('comments.taskNotCreated') }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.progress')">{{ task?.progress ?? 0 }}%</el-descriptions-item>
          <el-descriptions-item :label="t('comments.totalComments')">{{ analysis?.totalCount ?? t('common.dash') }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.negativeRate')">
            {{ analysis?.negativeRate == null ? t('common.dash') : formatPercent(analysis.negativeRate) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="insight-block section-gap">
          {{ analysis?.summary || t('comments.noAnalysisSummary') }}
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">{{ t('comments.aiOutput') }}</div>
        <div class="insight-block">
          <strong>{{ t('comments.operationReport') }}</strong>
          <p>{{ report?.operationSuggestions || report?.fullReport || t('comments.reportMissing') }}</p>
        </div>
        <div class="insight-block">
          <strong>{{ t('comments.negativeReply') }}</strong>
          <p>{{ reply?.replyContent || t('comments.replyMissing') }}</p>
          <div v-if="reply?.ragUsed && reply.ragReferences?.length" class="rag-reference-list">
            <span class="rag-reference-label">{{ t('comments.ragReferences') }}</span>
            <template v-for="reference in reply.ragReferences" :key="`${reference.sourceType}-${reference.sourceId}`">
              <el-button
                v-if="isProblemSolutionReference(reference)"
                link
                type="primary"
                class="rag-reference-link"
                @click="openSolutionReference(reference)"
              >
                {{ formatRagReference(reference) }}
              </el-button>
              <span v-else class="rag-reference-text">{{ formatRagReference(reference) }}</span>
            </template>
          </div>
        </div>
        <div class="insight-block" v-loading="solutionLoading">
          <strong>{{ t('comments.solutionRecommendations') }}</strong>
          <div v-if="recommendedSolutions.length" class="solution-list">
            <div v-for="solution in recommendedSolutions" :key="solution.id" class="solution-item">
              <div>
                <strong>{{ solution.solutionTitle }}</strong>
                <p>{{ solution.solutionContent }}</p>
                <span v-if="solution.keywords" class="muted">
                  {{ t('comments.solutionKeyword', { keywords: solution.keywords }) }}
                </span>
              </div>
            </div>
          </div>
          <p v-else>{{ t('comments.solutionMissing') }}</p>
        </div>
      </div>
    </div>

    <div class="panel section-gap" v-loading="replyHistoryLoading">
      <div class="panel-title">{{ t('comments.replyHistory') }}</div>
      <el-table v-if="replyHistory.length" :data="replyHistory" size="small" max-height="300">
        <el-table-column prop="problemType" :label="t('comments.problemTag')" width="130">
          <template #default="{ row }">{{ displayProblemType(row.problemType) }}</template>
        </el-table-column>
        <el-table-column :label="t('comments.negativeReply')" min-width="360" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.replyContent }}</div>
            <div v-if="row.ragUsed && row.ragReferences?.length" class="rag-reference-list compact">
              <span class="rag-reference-label">{{ t('comments.ragReferences') }}</span>
              <template v-for="reference in row.ragReferences" :key="`${reference.sourceType}-${reference.sourceId}`">
                <el-button
                  v-if="isProblemSolutionReference(reference)"
                  link
                  type="primary"
                  class="rag-reference-link"
                  @click="openSolutionReference(reference)"
                >
                  {{ formatRagReference(reference) }}
                </el-button>
                <span v-else class="rag-reference-text">{{ formatRagReference(reference) }}</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createdAt')" width="180" />
      </el-table>
      <el-empty v-else :description="t('comments.replyHistoryEmpty')" :image-size="72" />
    </div>

    <el-dialog v-model="tagDialog.visible" :title="t('comments.tagDialogTitle')" width="480px">
      <el-form label-position="top">
        <el-form-item :label="t('comments.manualProblemType')">
          <el-input v-model="tagDialog.manualProblemType" :placeholder="t('comments.manualProblemPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('comments.customTags')">
          <el-select
            v-model="tagDialog.customTags"
            multiple
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            :placeholder="t('comments.customTagsPlaceholder')"
          >
            <el-option
              v-for="tag in activeTags"
              :key="tag.id"
              :label="tag.tagName"
              :value="tag.tagName"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveTags">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="translationDialogVisible" :title="t('comments.translationDialogTitle')" width="720px">
      <div class="reply-dialog-body">
        <div class="reply-context">
          <div class="reply-meta">
            <el-tag type="info" effect="plain">
              {{ t('comments.sourceLanguage', { language: translation?.sourceLanguage || 'auto' }) }}
            </el-tag>
            <el-tag type="success" effect="plain">
              {{ t('comments.targetLanguage', { language: translation?.targetLanguage || localeStore.locale }) }}
            </el-tag>
            <span class="muted">{{ t('comments.replyProductId', { id: translationSource?.productId || t('common.dash') }) }}</span>
          </div>
          <p>
            <strong>{{ t('comments.originalReview') }}</strong>
          </p>
          <p>{{ translation?.originalContent || displayCommentContent(translationSource) }}</p>
        </div>
        <div class="reply-context">
          <p>
            <strong>{{ t('comments.translatedReview') }}</strong>
          </p>
          <el-input
            :model-value="translation?.translatedContent || ''"
            type="textarea"
            readonly
            :autosize="{ minRows: 6, maxRows: 12 }"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="translationDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" :disabled="!translation?.translatedContent" @click="copyTranslation">
          <Copy :size="14" />
          {{ t('comments.copyTranslation') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyDialogVisible" :title="t('comments.replyDialogTitle')" width="720px">
      <div class="reply-dialog-body">
        <div class="reply-context">
          <div class="reply-meta">
            <el-tag type="info" effect="plain">
              {{ t('comments.replyScore', { score: replySource?.reviewScore ?? t('common.dash') }) }}
            </el-tag>
            <el-tag type="warning" effect="plain">
              {{ displayProblemType(replySource?.effectiveProblemType || replySource?.systemProblemType) }}
            </el-tag>
            <span class="muted">{{ t('comments.replyProductId', { id: replySource?.productId || t('common.dash') }) }}</span>
          </div>
          <p>{{ displayCommentContent(replySource) }}</p>
        </div>
        <el-input
          :model-value="reply?.replyContent || ''"
          type="textarea"
          readonly
          :autosize="{ minRows: 7, maxRows: 12 }"
        />
        <div v-if="reply?.ragUsed && reply.ragReferences?.length" class="rag-reference-list reply-dialog-references">
          <span class="rag-reference-label">{{ t('comments.ragReferences') }}</span>
          <template v-for="reference in reply.ragReferences" :key="`${reference.sourceType}-${reference.sourceId}`">
            <el-button
              v-if="isProblemSolutionReference(reference)"
              link
              type="primary"
              class="rag-reference-link"
              @click="openSolutionReference(reference)"
            >
              {{ formatRagReference(reference) }}
            </el-button>
            <span v-else class="rag-reference-text">{{ formatRagReference(reference) }}</span>
          </template>
        </div>
      </div>
      <template #footer>
        <el-button @click="replyDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" :disabled="!reply?.replyContent" @click="copyReply">
          <Copy :size="14" />
          {{ t('comments.copyReply') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.rag-reference-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  margin-top: 8px;
  font-size: 12px;
}

.rag-reference-list.compact {
  margin-top: 4px;
}

.rag-reference-label {
  color: var(--el-text-color-secondary);
}

.rag-reference-link {
  height: auto;
  min-height: 20px;
  padding: 0;
  text-align: left;
  white-space: normal;
}

.rag-reference-text {
  color: var(--el-text-color-regular);
}

.reply-dialog-references {
  margin-top: 10px;
}
</style>
