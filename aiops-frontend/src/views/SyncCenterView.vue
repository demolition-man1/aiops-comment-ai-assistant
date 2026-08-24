<script setup lang="ts">
import { Clock3, Pencil, Play, Plus, RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { syncApi } from '@/api/modules'
import type { SyncConfig, SyncExecution } from '@/api/types'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const configs = ref<SyncConfig[]>([])
const executions = ref<SyncExecution[]>([])
const sourceTypes = ['olist_directory', 'csv_file', 'crawler']
const importModes = ['full', 'incremental']

const defaultForm = (): Partial<SyncConfig> => ({
  syncName: '',
  sourceType: 'olist_directory',
  dataSource: 'olist',
  importMode: 'incremental',
  dataPath: 'D:\\666\\olist-brazilian-ecommerce',
  targetType: 'product_comment',
  maxCount: 100,
  delaySeconds: 3,
  cronExpression: '0 0 2 * * ?',
  autoAnalysis: 0,
  enabled: 1,
  remark: ''
})

const form = reactive<Partial<SyncConfig>>(defaultForm())

const page = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const executionPage = reactive({
  pageNum: 1,
  pageSize: 8,
  total: 0
})

const enabledConfigs = computed(() => configs.value.filter((item) => Number(item.enabled) === 1).length)
const lastExecution = computed(() => executions.value[0])

const statusTagType = (status?: string) => {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'processing') return 'warning'
  return 'info'
}

const displaySourceType = (sourceType?: string) => {
  return sourceType && sourceTypes.includes(sourceType) ? t(`sync.sourceTypes.${sourceType}`) : sourceType || t('common.unknown')
}

const displayImportMode = (importMode?: string) => {
  return importMode && importModes.includes(importMode) ? t(`sync.importModes.${importMode}`) : importMode || t('common.dash')
}

const displayStatus = (status?: string) => {
  return status ? t(`enums.taskStatus.${status}`, status) : t('common.unknown')
}

const resetForm = () => {
  Object.assign(form, defaultForm())
  editingId.value = undefined
}

const loadConfigs = async () => {
  const result = await syncApi.configs({
    pageNum: page.pageNum,
    pageSize: page.pageSize
  })
  configs.value = result.records || []
  page.total = result.total || 0
}

const loadExecutions = async () => {
  const result = await syncApi.executions({
    pageNum: executionPage.pageNum,
    pageSize: executionPage.pageSize
  })
  executions.value = result.records || []
  executionPage.total = result.total || 0
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([loadConfigs(), loadExecutions()])
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row: SyncConfig) => {
  resetForm()
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveConfig = async () => {
  if (!form.syncName?.trim()) {
    ElMessage.warning(t('sync.nameRequired'))
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await syncApi.updateConfig(editingId.value, payload)
      ElMessage.success(t('sync.updated'))
    } else {
      await syncApi.createConfig(payload)
      ElMessage.success(t('sync.created'))
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

const toggleConfig = async (row: SyncConfig) => {
  const previousValue = Number(row.enabled) === 1 ? 0 : 1
  try {
    const updated = Number(row.enabled) === 1
      ? await syncApi.enableConfig(row.id)
      : await syncApi.disableConfig(row.id)
    Object.assign(row, updated)
    ElMessage.success(Number(row.enabled) === 1 ? t('sync.enabled') : t('sync.disabled'))
    await loadExecutions()
  } catch (error) {
    row.enabled = previousValue
    throw error
  }
}

const triggerConfig = async (row: SyncConfig) => {
  await syncApi.trigger(row.id)
  ElMessage.success(t('sync.triggered'))
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('sync.title') }}</h2>
        <span class="muted">{{ t('sync.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadData">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="16" />
          {{ t('sync.createConfig') }}
        </el-button>
      </div>
    </div>

    <div class="grid three">
      <div class="panel metric-mini">
        <span class="muted">{{ t('sync.configCount') }}</span>
        <strong>{{ page.total }}</strong>
      </div>
      <div class="panel metric-mini">
        <span class="muted">{{ t('sync.enabledCount') }}</span>
        <strong>{{ enabledConfigs }}</strong>
      </div>
      <div class="panel metric-mini">
        <span class="muted">{{ t('sync.lastExecution') }}</span>
        <strong>{{ lastExecution ? displayStatus(lastExecution.executionStatus) : t('common.noData') }}</strong>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="status-line">
        <div class="panel-title">{{ t('sync.configList') }}</div>
        <Clock3 class="text-blue" :size="22" />
      </div>
      <el-table :data="configs" size="small">
        <el-table-column prop="syncName" :label="t('sync.syncName')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('sync.sourceType')" width="150">
          <template #default="{ row }">{{ displaySourceType(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column :label="t('sync.importMode')" width="120">
          <template #default="{ row }">{{ displayImportMode(row.importMode) }}</template>
        </el-table-column>
        <el-table-column prop="cronExpression" :label="t('sync.cronExpression')" width="150" show-overflow-tooltip />
        <el-table-column prop="nextRunTime" :label="t('sync.nextRunTime')" width="180" show-overflow-tooltip />
        <el-table-column :label="t('sync.enabledState')" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="() => toggleConfig(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="190" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" @click="openEditDialog(row)">
                <Pencil :size="14" />
                {{ t('common.edit') }}
              </el-button>
              <el-button size="small" type="primary" @click="triggerConfig(row)">
                <Play :size="14" />
                {{ t('sync.triggerNow') }}
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
        layout="total, prev, pager, next"
        :total="page.total"
        @current-change="loadConfigs"
      />
    </div>

    <div class="panel section-gap">
      <div class="panel-title">{{ t('sync.executionHistory') }}</div>
      <el-table :data="executions" size="small">
        <el-table-column prop="syncName" :label="t('sync.syncName')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="triggerType" :label="t('sync.triggerType')" width="120" />
        <el-table-column :label="t('common.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.executionStatus)" effect="plain">
              {{ displayStatus(row.executionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="linkedTaskId" :label="t('sync.linkedTask')" width="140" />
        <el-table-column prop="errorMessage" :label="t('sync.errorMessage')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="startTime" :label="t('sync.startTime')" width="180" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="executionPage.pageNum"
        v-model:page-size="executionPage.pageSize"
        class="section-gap"
        background
        layout="total, prev, pager, next"
        :total="executionPage.total"
        @current-change="loadExecutions"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? t('sync.editConfig') : t('sync.createConfig')" width="680px">
      <el-form label-position="top">
        <div class="sync-form-grid">
          <el-form-item :label="t('sync.syncName')">
            <el-input v-model="form.syncName" :placeholder="t('sync.syncNamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('sync.sourceType')">
            <el-select v-model="form.sourceType" style="width: 100%">
              <el-option
                v-for="sourceType in sourceTypes"
                :key="sourceType"
                :label="t(`sync.sourceTypes.${sourceType}`)"
                :value="sourceType"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('sync.cronExpression')">
            <el-input v-model="form.cronExpression" />
          </el-form-item>
          <el-form-item :label="t('sync.importMode')">
            <el-select v-model="form.importMode" style="width: 100%">
              <el-option
                v-for="importMode in importModes"
                :key="importMode"
                :label="t(`sync.importModes.${importMode}`)"
                :value="importMode"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item v-if="form.sourceType === 'olist_directory'" :label="t('sync.dataPath')">
          <el-input v-model="form.dataPath" />
        </el-form-item>

        <template v-if="form.sourceType === 'csv_file'">
          <el-form-item :label="t('sync.fileUrl')">
            <el-input v-model="form.fileUrl" :placeholder="t('sync.fileUrlPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('sync.objectKey')">
            <el-input v-model="form.objectKey" />
          </el-form-item>
        </template>

        <template v-if="form.sourceType === 'crawler'">
          <div class="sync-form-grid">
            <el-form-item :label="t('sync.platform')">
              <el-input v-model="form.platform" />
            </el-form-item>
            <el-form-item :label="t('sync.targetType')">
              <el-input v-model="form.targetType" />
            </el-form-item>
          </div>
          <el-form-item :label="t('sync.targetUrl')">
            <el-input v-model="form.targetUrl" />
          </el-form-item>
          <div class="sync-form-grid">
            <el-form-item :label="t('sync.maxCount')">
              <el-input-number v-model="form.maxCount" :min="1" :max="500" style="width: 100%" />
            </el-form-item>
            <el-form-item :label="t('sync.delaySeconds')">
              <el-input-number v-model="form.delaySeconds" :min="1" :max="30" style="width: 100%" />
            </el-form-item>
          </div>
        </template>

        <div class="sync-form-grid">
          <el-form-item :label="t('sync.enabledState')">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item :label="t('sync.autoAnalysis')">
            <el-switch v-model="form.autoAnalysis" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>

        <el-form-item :label="t('sync.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.metric-mini {
  min-height: 112px;
}

.metric-mini strong {
  display: block;
  margin-top: 14px;
  color: #0f172a;
  font-size: 28px;
}

.sync-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
</style>
