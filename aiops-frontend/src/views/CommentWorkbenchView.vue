<script setup lang="ts">
import { Bot, ClipboardCheck, Copy, MessageCircleReply, RefreshCw, Tags } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { aiApi, analysisApi, commentApi, pollTask } from '@/api/modules'
import type { AnalysisResult, Comment, NegativeReply, OperationReport, Task } from '@/api/types'
import { resolveAnalysisProductId } from '@/utils/analysisTarget'
import { formatPercent } from '@/utils/metricFormat'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()
const loading = ref(false)
const taskLoading = ref(false)
const comments = ref<Comment[]>([])
const total = ref(0)
const selected = ref<Comment>()
const task = ref<Task>()
const analysis = ref<AnalysisResult>()
const report = ref<OperationReport>()
const reply = ref<NegativeReply>()
const replySource = ref<Comment>()
const replyDialogVisible = ref(false)
const replyLoadingId = ref<number>()
const stopPolling = ref<(() => void) | null>(null)
const sentimentTypes = new Set(['positive', 'neutral', 'negative'])
const problemTypes = new Set(['quality', 'logistics', 'price', 'service', 'size', 'other', 'unclassified', 'pending'])

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
  customTagsText: ''
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

const selectComment = (row: Comment) => {
  selected.value = row
}

const createAnalysisTask = async () => {
  const productId = resolveAnalysisProductId({
    queryProductId: query.productId,
    selectedProductId: selected.value?.productId,
    firstVisibleProductId: comments.value[0]?.productId
  })
  if (!productId) {
    ElMessage.warning(t('comments.selectProductWarning'))
    return
  }

  taskLoading.value = true
  query.productId = productId
  try {
    task.value = await analysisApi.createTask({
      targetType: 'product',
      targetId: productId,
      analysisType: 'comment'
    })
  } catch {
    taskLoading.value = false
    return
  }

  stopPolling.value?.()
  stopPolling.value = pollTask(
    () => analysisApi.task(task.value!.taskId),
    async (latestTask) => {
      task.value = latestTask
      if (latestTask.taskStatus === 'success') {
        taskLoading.value = false
        analysis.value = await analysisApi.product(productId)
        ElMessage.success(t('comments.analysisDone'))
      }
      if (latestTask.taskStatus === 'failed') {
        taskLoading.value = false
        ElMessage.error(latestTask.errorMessage || t('comments.analysisFailed'))
      }
    },
    3000,
    () => {
      taskLoading.value = false
    }
  )
}

const generateReport = async () => {
  const productId = query.productId || selected.value?.productId
  if (!productId) {
    ElMessage.warning(t('comments.specifyProductWarning'))
    return
  }
  report.value = await aiApi.productReport({ productId, language: localeStore.locale })
  ElMessage.success(t('comments.reportGenerated'))
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

const openTagDialog = (row: Comment) => {
  tagDialog.visible = true
  tagDialog.commentId = row.id
  tagDialog.manualProblemType = row.manualProblemType || row.effectiveProblemType || row.systemProblemType || ''
  tagDialog.customTagsText = (row.customTags || []).join(',')
}

const saveTags = async () => {
  await commentApi.updateTags(tagDialog.commentId, {
    manualProblemType: tagDialog.manualProblemType,
    customTags: tagDialog.customTagsText
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  })
  tagDialog.visible = false
  ElMessage.success(t('comments.tagsSaved'))
  await loadComments()
}

onMounted(loadComments)
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
        <el-button type="primary" :loading="taskLoading" @click="createAnalysisTask">
          <Bot :size="16" />
          {{ t('comments.analyzeProduct') }}
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
        <el-table-column :label="t('common.action')" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openTagDialog(row)">
                <Tags :size="14" />
                {{ t('comments.tag') }}
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
        <el-button type="success" @click="generateReport">
          <ClipboardCheck :size="16" />
          {{ t('comments.generateReport') }}
        </el-button>
      </div>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
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
        </div>
      </div>
    </div>

    <el-dialog v-model="tagDialog.visible" :title="t('comments.tagDialogTitle')" width="480px">
      <el-form label-position="top">
        <el-form-item :label="t('comments.manualProblemType')">
          <el-input v-model="tagDialog.manualProblemType" :placeholder="t('comments.manualProblemPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('comments.customTags')">
          <el-input v-model="tagDialog.customTagsText" :placeholder="t('comments.customTagsPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveTags">{{ t('common.save') }}</el-button>
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
