<script setup lang="ts">
import { Bot, ClipboardCheck, Copy, MessageCircleReply, RefreshCw, Tags } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { aiApi, analysisApi, commentApi, pollTask } from '@/api/modules'
import type { AnalysisResult, Comment, NegativeReply, OperationReport, Task } from '@/api/types'
import { resolveAnalysisProductId } from '@/utils/analysisTarget'
import { formatPercent } from '@/utils/metricFormat'

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
    ElMessage.warning('请先输入商品ID，或选择一条评论')
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
        ElMessage.success('评论分析完成')
      }
      if (latestTask.taskStatus === 'failed') {
        taskLoading.value = false
        ElMessage.error(latestTask.errorMessage || '评论分析失败')
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
    ElMessage.warning('请先指定商品ID')
    return
  }
  report.value = await aiApi.productReport({ productId, language: 'zh-CN' })
  ElMessage.success('AI 运营报告已生成')
}

const generateReply = async () => {
  const target = selected.value
  if (!target?.id) {
    ElMessage.warning('请先选择一条评论')
    return
  }
  replyLoadingId.value = target.id
  try {
    replySource.value = target
    reply.value = await aiApi.negativeReply({
      commentId: target.id,
      toneType: 'professional',
      language: 'zh-CN'
    })
    replyDialogVisible.value = true
    ElMessage.success('差评回复已生成')
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
    return '-'
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
  return `原评论内容缺失，仅识别到评分 ${row.reviewScore ?? '-'} 分和问题标签 ${row.effectiveProblemType || row.systemProblemType || '未分类'}`
}

const copyReply = async () => {
  if (!reply.value?.replyContent) {
    return
  }
  await navigator.clipboard.writeText(reply.value.replyContent)
  ElMessage.success('回复内容已复制')
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
  ElMessage.success('标签已保存')
  await loadComments()
}

onMounted(loadComments)
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">评论智能工作台</h2>
        <span class="muted">筛选评论、编辑标签、触发分析并生成回复</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadComments">
          <RefreshCw :size="16" />
          刷新
        </el-button>
        <el-button type="primary" :loading="taskLoading" @click="createAnalysisTask">
          <Bot :size="16" />
          分析商品评论
        </el-button>
      </div>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item label="商品ID">
          <el-input v-model="query.productId" clearable placeholder="输入 product_id" style="width: 260px" />
        </el-form-item>
        <el-form-item label="情感">
          <el-select v-model="query.sentiment" clearable placeholder="全部" style="width: 130px">
            <el-option label="正面" value="positive" />
            <el-option label="中性" value="neutral" />
            <el-option label="负面" value="negative" />
          </el-select>
        </el-form-item>
        <el-form-item label="差评">
          <el-select v-model="query.isNegative" clearable placeholder="全部" style="width: 130px">
            <el-option label="是" value="1" />
            <el-option label="否" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadComments">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="comments"
        height="420"
        highlight-current-row
        @current-change="selectComment"
      >
        <el-table-column prop="reviewScore" label="评分" width="72" />
        <el-table-column prop="sentiment" label="情感" width="90">
          <template #default="{ row }">
            <el-tag :type="row.sentiment === 'negative' ? 'danger' : row.sentiment === 'positive' ? 'success' : 'info'">
              {{ row.sentiment || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productId" label="商品ID" width="170" show-overflow-tooltip />
        <el-table-column label="问题标签" width="190">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag size="small" type="warning" effect="plain">
                {{ row.effectiveProblemType || row.systemProblemType || '未分类' }}
              </el-tag>
              <el-tag v-for="tag in row.customTags || []" :key="tag" size="small" effect="plain">
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="280">
          <template #default="{ row }">
            <span class="comment-cell">{{ displayCommentContent(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openTagDialog(row)">
                <Tags :size="14" />
                标签
              </el-button>
              <el-button
                size="small"
                type="primary"
                plain
                :loading="replyLoadingId === row.id"
                @click="generateReplyFor(row)"
              >
                <MessageCircleReply :size="14" />
                回复
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
          生成运营报告
        </el-button>
      </div>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="panel-title">分析任务与指标</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务状态">{{ task?.taskStatus || '未创建' }}</el-descriptions-item>
          <el-descriptions-item label="进度">{{ task?.progress ?? 0 }}%</el-descriptions-item>
          <el-descriptions-item label="评论总量">{{ analysis?.totalCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="负面占比">
            {{ analysis?.negativeRate == null ? '-' : formatPercent(analysis.negativeRate) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="insight-block section-gap">
          {{ analysis?.summary || '完成评论分析后，这里会展示 AI 汇总出的核心结论。' }}
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">AI 输出结果</div>
        <div class="insight-block">
          <strong>运营报告</strong>
          <p>{{ report?.operationSuggestions || report?.fullReport || '点击“生成运营报告”后展示商品优化建议。' }}</p>
        </div>
        <div class="insight-block">
          <strong>差评回复</strong>
          <p>{{ reply?.replyContent || '选择评论并生成回复后展示可复制话术。' }}</p>
        </div>
      </div>
    </div>

    <el-dialog v-model="tagDialog.visible" title="编辑评论标签" width="480px">
      <el-form label-position="top">
        <el-form-item label="人工问题分类">
          <el-input v-model="tagDialog.manualProblemType" placeholder="如：物流慢、包装破损、尺码偏小" />
        </el-form-item>
        <el-form-item label="自定义标签">
          <el-input v-model="tagDialog.customTagsText" placeholder="多个标签用英文逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyDialogVisible" title="AI 差评回复" width="720px">
      <div class="reply-dialog-body">
        <div class="reply-context">
          <div class="reply-meta">
            <el-tag type="info" effect="plain">评分 {{ replySource?.reviewScore ?? '-' }}</el-tag>
            <el-tag type="warning" effect="plain">
              {{ replySource?.effectiveProblemType || replySource?.systemProblemType || '未分类' }}
            </el-tag>
            <span class="muted">商品ID：{{ replySource?.productId || '-' }}</span>
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
        <el-button @click="replyDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!reply?.replyContent" @click="copyReply">
          <Copy :size="14" />
          复制回复
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>
