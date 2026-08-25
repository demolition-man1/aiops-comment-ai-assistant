<script setup lang="ts">
import { Database, FileUp, Globe2, Play, ShieldCheck } from 'lucide-vue-next'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { dataImportApi, fileApi, pollTask } from '@/api/modules'
import type { CsvImportPreflight, FileUploadResult, Task } from '@/api/types'
import {
  getMissingMappedFields,
  parseCsvPreview,
  suggestColumnMapping,
  type CsvColumnMapping,
  type CsvPreviewResult
} from '@/utils/csvPreview'

const { t } = useI18n()
const uploadLoading = ref(false)
const importLoading = ref(false)
const sampleLoading = ref(false)
const crawlerLoading = ref(false)
const uploaded = ref<FileUploadResult>()
const selectedFile = ref<File>()
const csvPreview = ref<CsvPreviewResult>()
const csvPreflight = ref<CsvImportPreflight>()
const csvFileHash = ref('')
const csvAllowDuplicate = ref(false)
const uploadRef = ref<UploadInstance>()
const csvTask = ref<Task>()
const crawlerTask = ref<Task>()
const stopCsvPolling = ref<(() => void) | null>(null)
const stopCrawlerPolling = ref<(() => void) | null>(null)
const csvMapping = reactive<Record<keyof CsvColumnMapping, string | undefined>>({
  product_id: undefined,
  review_score: undefined,
  review_content: undefined,
  review_title: undefined,
  review_id: undefined,
  order_id: undefined,
  seller_id: undefined,
  review_time: undefined
})
const mappingFields: Array<{ key: keyof CsvColumnMapping; label: string; required?: boolean }> = [
  { key: 'product_id', label: 'product_id', required: true },
  { key: 'review_score', label: 'review_score', required: true },
  { key: 'review_content', label: 'review_content' },
  { key: 'review_title', label: 'review_title' },
  { key: 'review_id', label: 'review_id' },
  { key: 'order_id', label: 'order_id' },
  { key: 'seller_id', label: 'seller_id' },
  { key: 'review_time', label: 'review_time' }
]

const csvForm = reactive({
  dataSource: 'olist',
  importMode: 'full',
  dataPath: 'D:\\666\\olist-brazilian-ecommerce'
})

const crawlerForm = reactive({
  platform: 'demo',
  targetUrl: '',
  targetType: 'product_comment',
  maxCount: 100,
  delaySeconds: 3,
  remark: ''
})

const clearSelectedCsv = () => {
  selectedFile.value = undefined
  uploaded.value = undefined
  csvPreview.value = undefined
  csvPreflight.value = undefined
  csvFileHash.value = ''
  csvAllowDuplicate.value = false
  resetCsvMapping()
}

const resetCsvMapping = () => {
  mappingFields.forEach((field) => {
    csvMapping[field.key] = undefined
  })
}

const applyCsvMapping = (mapping: CsvColumnMapping) => {
  resetCsvMapping()
  Object.entries(mapping).forEach(([field, column]) => {
    csvMapping[field as keyof CsvColumnMapping] = column
  })
}

const compactColumnMapping = () => {
  return Object.fromEntries(
    Object.entries(csvMapping).filter(([, column]) => Boolean(column))
  ) as Record<string, string>
}

const hashFile = async (file: File) => {
  const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer())
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
}

const inspectSingleCsvFile = async (file: UploadFile) => {
  const rawFile = file.raw
  if (!rawFile) {
    clearSelectedCsv()
    return false
  }

  try {
    const text = await rawFile.text()
    const preview = parseCsvPreview(text, 20)
    if (preview.columns.length === 0) {
      throw new Error('empty csv')
    }
    selectedFile.value = rawFile
    uploaded.value = undefined
    csvPreview.value = preview
    csvFileHash.value = await hashFile(rawFile)
    csvAllowDuplicate.value = false
    applyCsvMapping(suggestColumnMapping(preview.columns))
    try {
      csvPreflight.value = await dataImportApi.preflightCsv({
        fileName: rawFile.name,
        fileSize: rawFile.size,
        fileHash: csvFileHash.value,
        dataSource: csvForm.dataSource,
        importMode: csvForm.importMode,
        estimatedRows: preview.estimatedRows,
        columnMapping: compactColumnMapping()
      })
    } catch {
      csvPreflight.value = undefined
      ElMessage.warning(t('importCenter.preflightFailed'))
    }
  } catch {
    clearSelectedCsv()
    uploadRef.value?.clearFiles()
    ElMessage.error(t('importCenter.csvPreviewFailed'))
    return false
  }
  return true
}

const selectCsvFile = async (file: UploadFile) => {
  await inspectSingleCsvFile(file)
}

const removeCsvFile = () => {
  clearSelectedCsv()
}

const uploadSelectedCsv = async () => {
  if (!selectedFile.value) {
    return uploaded.value
  }

  uploadLoading.value = true
  try {
    uploaded.value = await fileApi.uploadCsv(selectedFile.value)
    ElMessage.success(t('importCenter.uploadSuccess'))
    return uploaded.value
  } finally {
    uploadLoading.value = false
  }
}

const startCsvImport = async () => {
  if (!selectedFile.value && !uploaded.value && !csvForm.dataPath.trim()) {
    ElMessage.warning(t('importCenter.uploadRequired'))
    return
  }
  if (selectedFile.value) {
    const missing = getMissingMappedFields(csvMapping)
    if (missing.length > 0) {
      ElMessage.warning(t('importCenter.mappingRequired', { columns: missing.join(', ') }))
      return
    }
    if (csvPreflight.value?.duplicateLikely && !csvAllowDuplicate.value) {
      try {
        await ElMessageBox.confirm(
          csvPreflight.value.duplicateMessage || t('importCenter.duplicateConfirm'),
          t('importCenter.duplicateTitle'),
          { type: 'warning', confirmButtonText: t('importCenter.continueImport'), cancelButtonText: t('common.cancel') }
        )
        csvAllowDuplicate.value = true
      } catch {
        return
      }
    }
  }

  importLoading.value = true
  try {
    const uploadResult = selectedFile.value && !uploaded.value
      ? await uploadSelectedCsv()
      : uploaded.value
    const dataPath = uploadResult ? undefined : csvForm.dataPath.trim() || undefined
    csvTask.value = await dataImportApi.importCsv({
      fileId: uploadResult?.fileId,
      objectKey: uploadResult?.objectKey,
      fileUrl: uploadResult?.fileUrl,
      dataPath,
      dataSource: csvForm.dataSource,
      importMode: csvForm.importMode,
      fileHash: csvFileHash.value || undefined,
      columnMapping: selectedFile.value ? compactColumnMapping() : undefined,
      allowDuplicate: csvAllowDuplicate.value
    })
    monitorCsvTask()
  } catch {
    importLoading.value = false
  }
}

const startSampleImport = async () => {
  sampleLoading.value = true
  try {
    csvTask.value = await dataImportApi.importSample()
    monitorCsvTask()
  } catch {
    sampleLoading.value = false
  }
}

const monitorCsvTask = () => {
  stopCsvPolling.value?.()
  stopCsvPolling.value = pollTask(
    () => dataImportApi.task(csvTask.value!.taskId, csvTask.value!.importType || 'csv'),
    (latestTask) => {
      csvTask.value = latestTask
      if (latestTask.taskStatus === 'success') {
        importLoading.value = false
        sampleLoading.value = false
        ElMessage.success(t('importCenter.csvImportDone'))
      }
      if (latestTask.taskStatus === 'failed') {
        importLoading.value = false
        sampleLoading.value = false
        ElMessage.error(latestTask.errorMessage || t('importCenter.csvImportFailed'))
      }
    },
    3000,
    () => {
      importLoading.value = false
      sampleLoading.value = false
    }
  )
}

const startCrawlerImport = async () => {
  if (!crawlerForm.targetUrl) {
    ElMessage.warning(t('importCenter.crawlerUrlRequired'))
    return
  }

  crawlerLoading.value = true
  try {
    crawlerTask.value = await dataImportApi.importCrawler({
      ...crawlerForm,
      remark: crawlerForm.remark || t('importCenter.researchRemark')
    })
    stopCrawlerPolling.value?.()
    stopCrawlerPolling.value = pollTask(
      () => dataImportApi.task(crawlerTask.value!.taskId, crawlerTask.value!.importType || 'crawler'),
      (latestTask) => {
        crawlerTask.value = latestTask
        if (latestTask.taskStatus === 'success') {
          crawlerLoading.value = false
          ElMessage.success(t('importCenter.crawlerDone'))
        }
        if (latestTask.taskStatus === 'failed') {
          crawlerLoading.value = false
          ElMessage.error(latestTask.errorMessage || t('importCenter.crawlerFailed'))
        }
      },
      3000,
      () => {
        crawlerLoading.value = false
      }
    )
  } catch {
    crawlerLoading.value = false
  }
}

const csvProgress = computed(() => Number(csvTask.value?.progress || 0))
const crawlerProgress = computed(() => Number(crawlerTask.value?.progress || 0))
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('importCenter.title') }}</h2>
        <span class="muted">{{ t('importCenter.subtitle') }}</span>
      </div>
    </div>

    <div class="grid two">
      <div id="scheduled-sync" class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('importCenter.csvTitle') }}</div>
          <FileUp class="text-blue" :size="22" />
        </div>

        <el-upload
          ref="uploadRef"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="true"
          accept=".csv"
          :limit="1"
          :on-change="selectCsvFile"
          :on-remove="removeCsvFile"
        >
          <el-icon class="el-icon--upload"><FileUp /></el-icon>
          <div class="el-upload__text">{{ t('importCenter.uploadHint') }}</div>
          <template #tip>
            <div class="el-upload__tip">{{ t('importCenter.uploadTip') }}</div>
          </template>
        </el-upload>

        <div class="status-line section-gap">
          <div>
            <strong>{{ t('importCenter.sampleTitle') }}</strong>
            <p class="muted">{{ t('importCenter.sampleDesc') }}</p>
          </div>
          <el-button type="warning" plain :loading="sampleLoading" @click="startSampleImport">
            <Play :size="16" />
            {{ t('importCenter.importSample') }}
          </el-button>
        </div>

        <el-form class="section-gap" label-position="top">
          <el-form-item :label="t('importCenter.dataSource')">
            <el-select v-model="csvForm.dataSource" style="width: 100%">
              <el-option :label="t('importCenter.olistSource')" value="olist" />
              <el-option :label="t('importCenter.platformCsv')" value="platform_csv" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('importCenter.importMode')">
            <el-radio-group v-model="csvForm.importMode">
              <el-radio-button label="full">{{ t('importCenter.fullImport') }}</el-radio-button>
              <el-radio-button label="incremental">{{ t('importCenter.incrementalImport') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="t('importCenter.localOlistPath')">
            <el-input v-model="csvForm.dataPath" :placeholder="t('importCenter.localPathPlaceholder')" />
            <div class="muted">{{ t('importCenter.localPathTip') }}</div>
          </el-form-item>
        </el-form>

        <div v-if="uploaded || selectedFile" class="insight-block">
          <strong>{{ uploaded?.originalName || selectedFile?.name }}</strong>
          <p v-if="uploaded" class="muted">{{ t('importCenter.ossKey', { key: uploaded.objectKey }) }}</p>
          <div v-if="csvPreview" class="preview-summary">
            <span>{{ t('importCenter.estimatedRows', { count: csvPreview.estimatedRows }) }}</span>
            <span>{{ t('importCenter.previewRows', { count: csvPreview.rows.length }) }}</span>
          </div>
          <el-alert
            v-if="csvPreflight?.duplicateLikely"
            class="section-gap"
            type="warning"
            show-icon
            :closable="false"
            :title="csvPreflight.duplicateMessage || t('importCenter.duplicateConfirm')"
          />
          <div v-if="csvPreview" class="mapping-grid section-gap">
            <el-form-item
              v-for="field in mappingFields"
              :key="field.key"
              :label="`${field.label}${field.required ? ' *' : ''}`"
            >
              <el-select v-model="csvMapping[field.key]" clearable :placeholder="t('importCenter.selectColumn')">
                <el-option
                  v-for="column in csvPreview.columns"
                  :key="column"
                  :label="column"
                  :value="column"
                />
              </el-select>
            </el-form-item>
          </div>
          <el-table v-if="csvPreview" class="preview-table" :data="csvPreview.rows" size="small" max-height="260">
            <el-table-column
              v-for="column in csvPreview.columns.slice(0, 8)"
              :key="column"
              :prop="column"
              :label="column"
              min-width="140"
              show-overflow-tooltip
            />
          </el-table>
          <p v-if="csvPreview && csvPreview.columns.length > 8" class="muted">
            {{ t('importCenter.previewColumnTip', { count: csvPreview.columns.length }) }}
          </p>
        </div>

        <div class="status-line section-gap">
          <el-progress :percentage="csvProgress" style="flex: 1" />
          <el-button type="primary" :loading="uploadLoading || importLoading" @click="startCsvImport">
            <Play :size="16" />
            {{ t('importCenter.startImport') }}
          </el-button>
        </div>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('importCenter.crawlerTitle') }}</div>
          <Globe2 class="text-green" :size="22" />
        </div>

        <el-alert
          type="warning"
          show-icon
          :closable="false"
          :title="t('importCenter.crawlerWarning')"
        />

        <el-form class="section-gap" label-position="top">
          <el-form-item :label="t('importCenter.platform')">
            <el-select v-model="crawlerForm.platform" style="width: 100%">
              <el-option :label="t('importCenter.demoPlatform')" value="demo" />
              <el-option :label="t('importCenter.taobaoSample')" value="taobao" />
              <el-option :label="t('importCenter.pddSample')" value="pdd" />
              <el-option :label="t('importCenter.temuSample')" value="temu" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('importCenter.targetUrl')">
            <el-input v-model="crawlerForm.targetUrl" :placeholder="t('importCenter.targetUrlPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('importCenter.crawlLimitDelay')">
            <div class="inline-fields">
              <el-input-number v-model="crawlerForm.maxCount" :min="1" :max="500" />
              <el-input-number v-model="crawlerForm.delaySeconds" :min="1" :max="30" />
            </div>
          </el-form-item>
          <el-form-item :label="t('importCenter.remark')">
            <el-input v-model="crawlerForm.remark" type="textarea" :rows="3" :placeholder="t('importCenter.researchRemark')" />
          </el-form-item>
        </el-form>

        <div class="status-line">
          <el-progress :percentage="crawlerProgress" style="flex: 1" />
          <el-button type="success" :loading="crawlerLoading" @click="startCrawlerImport">
            <Database :size="16" />
            {{ t('importCenter.createCrawlerTask') }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="grid three section-gap">
      <div class="panel workflow-card">
        <span class="number">1</span>
        <strong>{{ t('importCenter.workflowUpload') }}</strong>
        <span class="muted">{{ t('importCenter.workflowUploadDesc') }}</span>
      </div>
      <div class="panel workflow-card">
        <span class="number">2</span>
        <strong>{{ t('importCenter.workflowClean') }}</strong>
        <span class="muted">{{ t('importCenter.workflowCleanDesc') }}</span>
      </div>
      <div class="panel workflow-card">
        <span class="number">3</span>
        <strong>{{ t('importCenter.workflowAnalyze') }}</strong>
        <span class="muted">{{ t('importCenter.workflowAnalyzeDesc') }}</span>
        <ShieldCheck class="text-green" :size="22" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.preview-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: #475569;
}

.mapping-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.mapping-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.preview-table {
  margin-top: 12px;
}
</style>
