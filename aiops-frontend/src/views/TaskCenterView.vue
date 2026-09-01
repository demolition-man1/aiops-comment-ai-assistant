<script setup lang="ts">
import { Download, Eye, RefreshCw, RotateCcw, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import AiJobProgressPanel from '@/components/AiJobProgressPanel.vue'
import { aiJobApi, taskCenterApi } from '@/api/modules'
import type { AiJob, TaskRecord } from '@/api/types'
import { saveBlob } from '@/utils/download'

const { t } = useI18n()
const loading = ref(false)
const exporting = ref(false)
const retryingKey = ref('')
const drawerVisible = ref(false)
const selectedTask = ref<TaskRecord>()
const tasks = ref<TaskRecord[]>([])
const aiJobs = ref<Record<number, AiJob>>({})

const filters = reactive({
  taskType: '',
  taskStatus: '',
  keyword: ''
})

const page = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const taskTypes = ['csv_import', 'crawler_import', 'comment_analysis', 'scheduled_sync', 'operation_report', 'product_compare', 'negative_reply', 'content']
const taskStatuses = ['pending', 'processing', 'success', 'failed', 'timed_out', 'cancelled']

const statusTagType = (status?: string) => {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'processing') return 'warning'
  return 'info'
}

const displayTaskType = (taskType?: string) => {
  return taskType && taskTypes.includes(taskType) ? t(`enums.taskType.${taskType}`) : taskType || t('common.unknown')
}

const displayTaskStatus = (status?: string) => {
  return status && taskStatuses.includes(status) ? t(`enums.taskStatus.${status}`) : status || t('common.unknown')
}

const loadTasks = async () => {
  loading.value = true
  try {
    const result = await taskCenterApi.page({
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      taskType: filters.taskType || undefined,
      taskStatus: filters.taskStatus || undefined,
      keyword: filters.keyword || undefined
    })
    tasks.value = result.records || []
    page.total = result.total || 0
    const aiRows = tasks.value.filter(row => ['operation_report', 'product_compare', 'negative_reply', 'content'].includes(row.taskType))
    const resolved = await Promise.all(aiRows.map(async row => [row.sourceId, await aiJobApi.job(row.sourceId)] as const))
    aiJobs.value = Object.fromEntries(resolved)
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  filters.taskType = ''
  filters.taskStatus = ''
  filters.keyword = ''
  page.pageNum = 1
  await loadTasks()
}

const openDetail = async (row: TaskRecord) => {
  selectedTask.value = await taskCenterApi.detail(row.recordKey)
  drawerVisible.value = true
}

const retryTask = async (row: TaskRecord) => {
  retryingKey.value = row.recordKey
  try {
    await taskCenterApi.retry(row.recordKey)
    ElMessage.success(t('tasks.retryCreated'))
    await loadTasks()
  } finally {
    retryingKey.value = ''
  }
}

const handleAiResult = () => void loadTasks()

const exportTasks = async () => {
  exporting.value = true
  try {
    const blob = await taskCenterApi.exportCsv({
      taskType: filters.taskType || undefined,
      taskStatus: filters.taskStatus || undefined,
      keyword: filters.keyword || undefined
    })
    saveBlob(blob, `aiops-tasks-${Date.now()}.csv`)
    ElMessage.success(t('tasks.exported'))
  } finally {
    exporting.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('tasks.title') }}</h2>
        <span class="muted">{{ t('tasks.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button :loading="exporting" @click="exportTasks">
          <Download :size="16" />
          {{ t('tasks.exportCsv') }}
        </el-button>
        <el-button @click="loadTasks">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="panel">
      <el-form class="task-filter" label-position="top">
        <el-form-item :label="t('tasks.taskType')">
          <el-select v-model="filters.taskType" clearable style="width: 100%">
            <el-option :label="t('common.all')" value="" />
            <el-option v-for="item in taskTypes" :key="item" :label="t(`enums.taskType.${item}`)" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="filters.taskStatus" clearable style="width: 100%">
            <el-option :label="t('common.all')" value="" />
            <el-option v-for="item in taskStatuses" :key="item" :label="t(`enums.taskStatus.${item}`)" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('tasks.keyword')">
          <el-input v-model="filters.keyword" clearable :placeholder="t('tasks.keywordPlaceholder')" />
        </el-form-item>
        <div class="task-filter-actions">
          <el-button type="primary" @click="loadTasks">
            <Search :size="16" />
            {{ t('common.search') }}
          </el-button>
          <el-button @click="resetFilters">{{ t('common.reset') }}</el-button>
        </div>
      </el-form>
    </div>

    <div class="panel section-gap">
      <div class="panel-title">{{ t('tasks.taskList') }}</div>
      <el-table :data="tasks" size="small">
        <el-table-column prop="taskName" :label="t('tasks.taskName')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('tasks.taskType')" width="150">
          <template #default="{ row }">{{ displayTaskType(row.taskType) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.taskStatus)" effect="plain">
              {{ displayTaskStatus(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.progress')" width="220">
          <template #default="{ row }">
            <AiJobProgressPanel v-if="aiJobs[row.sourceId]" :job="aiJobs[row.sourceId]" @result="handleAiResult" />
            <el-progress v-else :percentage="Number(row.progress || 0)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="targetId" :label="t('tasks.target')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="errorMessage" :label="t('tasks.errorMessage')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" :label="t('common.createdAt')" width="180" show-overflow-tooltip />
        <el-table-column :label="t('common.action')" width="180" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openDetail(row)">
                <Eye :size="14" />
                {{ t('common.view') }}
              </el-button>
              <el-button
                size="small"
                type="primary"
                :loading="retryingKey === row.recordKey"
                @click="retryTask(row)"
              >
                <RotateCcw :size="14" />
                {{ t('tasks.retry') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page.pageNum"
        v-model:page-size="page.pageSize"
        class="section-gap"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :total="page.total"
        @current-change="loadTasks"
        @size-change="loadTasks"
      />
    </div>

    <el-drawer v-model="drawerVisible" :title="t('tasks.detail')" size="520px">
      <el-descriptions v-if="selectedTask" :column="1" border>
        <el-descriptions-item :label="t('tasks.recordKey')">{{ selectedTask.recordKey }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.taskName')">{{ selectedTask.taskName }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.taskType')">{{ displayTaskType(selectedTask.taskType) }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">{{ displayTaskStatus(selectedTask.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.target')">{{ selectedTask.targetId || t('common.dash') }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.sourceTable')">{{ selectedTask.sourceTable }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.startTime')">{{ selectedTask.startTime || t('common.dash') }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.endTime')">{{ selectedTask.endTime || t('common.dash') }}</el-descriptions-item>
        <el-descriptions-item :label="t('tasks.errorMessage')">
          <span class="task-error">{{ selectedTask.errorMessage || t('common.dash') }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </section>
</template>

<style scoped>
.task-filter {
  display: grid;
  grid-template-columns: 180px 180px minmax(240px, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.task-filter-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 18px;
}

.task-error {
  color: #ef4444;
}
</style>
