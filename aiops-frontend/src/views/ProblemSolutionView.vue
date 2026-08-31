<script setup lang="ts">
import { Lightbulb, Pencil, Plus, RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import { problemSolutionApi, ragKnowledgeApi } from '@/api/modules'
import type { ProblemSolution, RagIndexStatus } from '@/api/types'

const { t } = useI18n()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const ragStatusLoading = ref(false)
const ragReindexing = ref(false)
const ragStatus = ref<RagIndexStatus>()
const dialogVisible = ref(false)
const editingId = ref<number>()
const solutions = ref<ProblemSolution[]>([])
const problemTypes = ['quality', 'logistics', 'price', 'service', 'size', 'other']

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  problemType: '',
  categoryNameEn: '',
  keyword: '',
  enabled: ''
})

const page = reactive({
  total: 0
})
let ragPollingTimer: number | undefined

const ragStatusLabel = computed(() => t(`solutions.ragStates.${ragStatus.value?.state || 'unknown'}`))
const ragStatusTag = computed(() => {
  if (ragStatus.value?.state === 'ready') return 'success'
  if (ragStatus.value?.state === 'failed') return 'danger'
  if (ragStatus.value?.state === 'building') return 'warning'
  return 'info'
})

const defaultForm = (): Partial<ProblemSolution> => ({
  problemType: 'quality',
  categoryNameEn: '',
  solutionTitle: '',
  solutionContent: '',
  keywords: '',
  sourceType: 'manual',
  priority: 0,
  enabled: 1
})

const form = reactive<Partial<ProblemSolution>>(defaultForm())

const displayProblemType = (value?: string) => {
  const key = value?.trim()
  return key && problemTypes.includes(key) ? t(`enums.problemType.${key}`) : key || t('enums.problemType.unclassified')
}

const loadSolutions = async () => {
  loading.value = true
  try {
    const result = await problemSolutionApi.page({
      ...query,
      enabled: query.enabled === '' ? undefined : Number(query.enabled)
    })
    solutions.value = result.records || []
    page.total = result.total || 0
  } finally {
    loading.value = false
  }
}

const stopRagPolling = () => {
  if (ragPollingTimer) {
    window.clearTimeout(ragPollingTimer)
    ragPollingTimer = undefined
  }
}

const loadRagStatus = async () => {
  ragStatusLoading.value = true
  try {
    ragStatus.value = await ragKnowledgeApi.status()
  } finally {
    ragStatusLoading.value = false
  }
}

const startRagPolling = () => {
  stopRagPolling()
  const poll = async () => {
    try {
      await loadRagStatus()
      if (ragStatus.value?.state === 'building') {
        ragPollingTimer = window.setTimeout(poll, 2000)
      } else {
        ragReindexing.value = false
        stopRagPolling()
      }
    } catch {
      ragReindexing.value = false
      stopRagPolling()
    }
  }
  ragPollingTimer = window.setTimeout(poll, 2000)
}

const reindexRag = async () => {
  ragReindexing.value = true
  try {
    ragStatus.value = await ragKnowledgeApi.reindex()
    ElMessage.success(t('solutions.ragRebuildStarted'))
    startRagPolling()
  } catch (error) {
    ragReindexing.value = false
    throw error
  }
}

const resetForm = () => {
  Object.assign(form, defaultForm())
  editingId.value = undefined
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: ProblemSolution) => {
  resetForm()
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveSolution = async () => {
  if (!form.problemType?.trim()) {
    ElMessage.warning(t('solutions.problemTypeRequired'))
    return
  }
  if (!form.solutionTitle?.trim()) {
    ElMessage.warning(t('solutions.titleRequired'))
    return
  }
  if (!form.solutionContent?.trim()) {
    ElMessage.warning(t('solutions.contentRequired'))
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await problemSolutionApi.update(editingId.value, form)
      ElMessage.success(t('solutions.updated'))
    } else {
      await problemSolutionApi.create(form)
      ElMessage.success(t('solutions.created'))
    }
    dialogVisible.value = false
    await loadSolutions()
  } finally {
    saving.value = false
  }
}

const toggleSolution = async (row: ProblemSolution) => {
  const previousValue = Number(row.enabled) === 1 ? 0 : 1
  try {
    await problemSolutionApi.updateStatus(row.id, Number(row.enabled))
    ElMessage.success(Number(row.enabled) === 1 ? t('solutions.enabled') : t('solutions.disabled'))
  } catch (error) {
    row.enabled = previousValue
    throw error
  }
}

onMounted(async () => {
  const keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  if (keyword) {
    query.keyword = keyword
  }
  await Promise.all([loadSolutions(), loadRagStatus()])
  if (ragStatus.value?.state === 'building') {
    startRagPolling()
  }
})

onBeforeUnmount(stopRagPolling)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('solutions.title') }}</h2>
        <span class="muted">{{ t('solutions.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadSolutions">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="16" />
          {{ t('solutions.createSolution') }}
        </el-button>
      </div>
    </div>

    <div class="rag-status-band" v-loading="ragStatusLoading">
      <div class="rag-status-copy">
        <span class="rag-status-label">{{ t('solutions.ragStatus') }}</span>
        <el-tag size="small" :type="ragStatusTag" effect="plain">{{ ragStatusLabel }}</el-tag>
        <span class="muted">{{ t('solutions.ragDocuments', { count: ragStatus?.documentCount ?? 0 }) }}</span>
        <span v-if="ragStatus?.enabled" class="muted">
          {{ t('solutions.ragReviewEvidence', { count: ragStatus.reviewEvidenceCount ?? 0 }) }}
        </span>
        <span v-if="ragStatus?.lastReindexAt" class="muted">
          {{ t('solutions.ragLastReindexAt', { time: ragStatus.lastReindexAt }) }}
        </span>
        <span v-if="ragStatus?.state === 'failed'" class="rag-status-error">{{ ragStatus.lastError }}</span>
      </div>
      <el-button
        :loading="ragReindexing"
        :disabled="ragReindexing || ragStatus?.state === 'building'"
        @click="reindexRag"
      >
        <RefreshCw :size="16" />
        {{ t('solutions.rebuildRagIndex') }}
      </el-button>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item :label="t('solutions.problemType')">
          <el-select v-model="query.problemType" clearable :placeholder="t('common.all')" style="width: 150px">
            <el-option
              v-for="type in problemTypes"
              :key="type"
              :label="displayProblemType(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('solutions.categoryNameEn')">
          <el-input
            v-model="query.categoryNameEn"
            clearable
            :placeholder="t('solutions.categoryPlaceholder')"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item :label="t('solutions.keyword')">
          <el-input v-model="query.keyword" clearable :placeholder="t('solutions.keywordPlaceholder')" style="width: 230px" />
        </el-form-item>
        <el-form-item :label="t('solutions.enabledState')">
          <el-select v-model="query.enabled" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('settings.enabled')" value="1" />
            <el-option :label="t('settings.disabled')" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSolutions">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="solutions" height="520" size="small">
        <el-table-column :label="t('solutions.solutionTitle')" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="solution-title-cell">
              <Lightbulb class="text-amber" :size="16" />
              <span>{{ row.solutionTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('solutions.problemType')" width="130">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">{{ displayProblemType(row.problemType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryNameEn" :label="t('solutions.categoryNameEn')" width="170" show-overflow-tooltip />
        <el-table-column prop="keywords" :label="t('solutions.keywords')" min-width="210" show-overflow-tooltip />
        <el-table-column prop="priority" :label="t('solutions.priority')" width="90" />
        <el-table-column prop="useCount" :label="t('solutions.useCount')" width="90" />
        <el-table-column :label="t('solutions.enabledState')" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="() => toggleSolution(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">
              <Pencil :size="14" />
              {{ t('common.edit') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="section-gap"
        background
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="page.total"
        @change="loadSolutions"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('solutions.editSolution') : t('solutions.createSolution')"
      width="720px"
    >
      <el-form label-position="top">
        <div class="inline-fields">
          <el-form-item :label="t('solutions.problemType')" style="flex: 1">
            <el-select v-model="form.problemType" style="width: 100%">
              <el-option
                v-for="type in problemTypes"
                :key="type"
                :label="displayProblemType(type)"
                :value="type"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('solutions.categoryNameEn')" style="flex: 1">
            <el-input v-model="form.categoryNameEn" :placeholder="t('solutions.categoryPlaceholder')" />
          </el-form-item>
        </div>
        <el-form-item :label="t('solutions.solutionTitle')">
          <el-input v-model="form.solutionTitle" :placeholder="t('solutions.solutionTitlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('solutions.solutionContent')">
          <el-input
            v-model="form.solutionContent"
            type="textarea"
            :autosize="{ minRows: 5, maxRows: 9 }"
            :placeholder="t('solutions.solutionContentPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('solutions.keywords')">
          <el-input v-model="form.keywords" :placeholder="t('solutions.keywordsPlaceholder')" />
        </el-form-item>
        <div class="inline-fields">
          <el-form-item :label="t('solutions.sourceType')" style="flex: 1">
            <el-input v-model="form.sourceType" />
          </el-form-item>
          <el-form-item :label="t('solutions.priority')" style="flex: 1">
            <el-input-number v-model="form.priority" :min="0" :max="999" />
          </el-form-item>
          <el-form-item :label="t('solutions.enabledState')" style="flex: 1">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveSolution">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.rag-status-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  margin-bottom: 16px;
  padding: 8px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.rag-status-copy {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.rag-status-label {
  font-weight: 600;
}

.rag-status-error {
  color: var(--el-color-danger);
}

@media (max-width: 720px) {
  .rag-status-band {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
