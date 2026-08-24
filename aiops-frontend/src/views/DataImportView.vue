<script setup lang="ts">
import { Database, FileUp, Globe2, Play, ShieldCheck } from 'lucide-vue-next'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { dataImportApi, fileApi, pollTask } from '@/api/modules'
import type { FileUploadResult, Task } from '@/api/types'

const { t } = useI18n()
const uploadLoading = ref(false)
const importLoading = ref(false)
const crawlerLoading = ref(false)
const uploaded = ref<FileUploadResult>()
const csvTask = ref<Task>()
const crawlerTask = ref<Task>()
const stopCsvPolling = ref<(() => void) | null>(null)
const stopCrawlerPolling = ref<(() => void) | null>(null)

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

const uploadCsv = async (options: UploadRequestOptions) => {
  uploadLoading.value = true
  try {
    uploaded.value = await fileApi.uploadCsv(options.file as File)
    ElMessage.success(t('importCenter.uploadSuccess'))
  } finally {
    uploadLoading.value = false
  }
}

const startCsvImport = async () => {
  if (!uploaded.value && !csvForm.dataPath.trim()) {
    ElMessage.warning(t('importCenter.uploadRequired'))
    return
  }

  importLoading.value = true
  try {
    const dataPath = uploaded.value ? undefined : csvForm.dataPath.trim() || undefined
    csvTask.value = await dataImportApi.importCsv({
      fileId: uploaded.value?.fileId,
      objectKey: uploaded.value?.objectKey,
      fileUrl: uploaded.value?.fileUrl,
      dataPath,
      dataSource: csvForm.dataSource,
      importMode: csvForm.importMode
    })

    stopCsvPolling.value?.()
    stopCsvPolling.value = pollTask(
      () => dataImportApi.task(csvTask.value!.taskId, csvTask.value!.importType || 'csv'),
      (latestTask) => {
        csvTask.value = latestTask
        if (latestTask.taskStatus === 'success') {
          importLoading.value = false
          ElMessage.success(t('importCenter.csvImportDone'))
        }
        if (latestTask.taskStatus === 'failed') {
          importLoading.value = false
          ElMessage.error(latestTask.errorMessage || t('importCenter.csvImportFailed'))
        }
      },
      3000,
      () => {
        importLoading.value = false
      }
    )
  } catch {
    importLoading.value = false
  }
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
          drag
          :http-request="uploadCsv"
          :show-file-list="true"
          accept=".csv"
          :limit="1"
        >
          <el-icon class="el-icon--upload"><FileUp /></el-icon>
          <div class="el-upload__text">{{ t('importCenter.uploadHint') }}</div>
          <template #tip>
            <div class="el-upload__tip">{{ t('importCenter.uploadTip') }}</div>
          </template>
        </el-upload>

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

        <div v-if="uploaded" class="insight-block">
          <strong>{{ uploaded.originalName }}</strong>
          <p class="muted">{{ t('importCenter.ossKey', { key: uploaded.objectKey }) }}</p>
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
