<script setup lang="ts">
import { Database, FileUp, Globe2, Play, ShieldCheck } from 'lucide-vue-next'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import { dataImportApi, fileApi, pollTask } from '@/api/modules'
import type { FileUploadResult, Task } from '@/api/types'

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
  remark: '仅科研学习演示，禁止高频大规模爬取'
})

const uploadCsv = async (options: UploadRequestOptions) => {
  uploadLoading.value = true
  try {
    uploaded.value = await fileApi.uploadCsv(options.file as File)
    ElMessage.success('CSV 已上传到对象存储')
  } finally {
    uploadLoading.value = false
  }
}

const startCsvImport = async () => {
  if (!uploaded.value && !csvForm.dataPath.trim()) {
    ElMessage.warning('请先上传 CSV 文件，或填写本地 Olist 数据目录')
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
          ElMessage.success('CSV 数据导入完成')
        }
        if (latestTask.taskStatus === 'failed') {
          importLoading.value = false
          ElMessage.error(latestTask.errorMessage || 'CSV 导入失败')
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
    ElMessage.warning('请输入公开评论页面 URL')
    return
  }

  crawlerLoading.value = true
  try {
    crawlerTask.value = await dataImportApi.importCrawler({ ...crawlerForm })
    stopCrawlerPolling.value?.()
    stopCrawlerPolling.value = pollTask(
      () => dataImportApi.task(crawlerTask.value!.taskId, crawlerTask.value!.importType || 'crawler'),
      (latestTask) => {
        crawlerTask.value = latestTask
        if (latestTask.taskStatus === 'success') {
          crawlerLoading.value = false
          ElMessage.success('爬虫导入任务完成')
        }
        if (latestTask.taskStatus === 'failed') {
          crawlerLoading.value = false
          ElMessage.error(latestTask.errorMessage || '爬虫导入失败')
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
        <h2 class="section-title">数据导入中心</h2>
        <span class="muted">支持阿里云 OSS 上传 CSV，也支持低频公开样例爬虫导入</span>
      </div>
    </div>

    <div class="grid two">
      <div class="panel">
        <div class="status-line">
          <div class="panel-title">CSV 文件导入</div>
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
          <div class="el-upload__text">拖拽 CSV 到这里，或点击上传</div>
          <template #tip>
            <div class="el-upload__tip">推荐使用 Olist CSV，文件会先上传到阿里云 OSS，再触发后端导入任务。</div>
          </template>
        </el-upload>

        <el-form class="section-gap" label-position="top">
          <el-form-item label="数据来源">
            <el-select v-model="csvForm.dataSource" style="width: 100%">
              <el-option label="Olist Kaggle 数据集" value="olist" />
              <el-option label="平台导出 CSV" value="platform_csv" />
            </el-select>
          </el-form-item>
          <el-form-item label="导入模式">
            <el-radio-group v-model="csvForm.importMode">
              <el-radio-button label="full">全量导入</el-radio-button>
              <el-radio-button label="incremental">增量导入</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="本地 Olist 数据目录">
            <el-input v-model="csvForm.dataPath" placeholder="例如 D:\666\olist-brazilian-ecommerce" />
            <div class="muted">完整 Olist 导入推荐填写本机目录，目录内放 9 个 CSV；浏览器上传适合单个 CSV 归档到 OSS。</div>
          </el-form-item>
        </el-form>

        <div v-if="uploaded" class="insight-block">
          <strong>{{ uploaded.originalName }}</strong>
          <p class="muted">OSS Key：{{ uploaded.objectKey }}</p>
        </div>

        <div class="status-line section-gap">
          <el-progress :percentage="csvProgress" style="flex: 1" />
          <el-button type="primary" :loading="uploadLoading || importLoading" @click="startCsvImport">
            <Play :size="16" />
            开始导入
          </el-button>
        </div>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">公开评论爬虫导入</div>
          <Globe2 class="text-green" :size="22" />
        </div>

        <el-alert
          type="warning"
          show-icon
          :closable="false"
          title="仅用于学习研究原型演示，请控制频率和数量，禁止商用高频采集。"
        />

        <el-form class="section-gap" label-position="top">
          <el-form-item label="平台">
            <el-select v-model="crawlerForm.platform" style="width: 100%">
              <el-option label="演示平台" value="demo" />
              <el-option label="淘宝公开样例" value="taobao" />
              <el-option label="拼多多公开样例" value="pdd" />
              <el-option label="Temu 公开样例" value="temu" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标 URL">
            <el-input v-model="crawlerForm.targetUrl" placeholder="粘贴公开商品评论页面 URL" />
          </el-form-item>
          <el-form-item label="采集数量与延时">
            <div class="inline-fields">
              <el-input-number v-model="crawlerForm.maxCount" :min="1" :max="500" />
              <el-input-number v-model="crawlerForm.delaySeconds" :min="1" :max="30" />
            </div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="crawlerForm.remark" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>

        <div class="status-line">
          <el-progress :percentage="crawlerProgress" style="flex: 1" />
          <el-button type="success" :loading="crawlerLoading" @click="startCrawlerImport">
            <Database :size="16" />
            创建爬虫任务
          </el-button>
        </div>
      </div>
    </div>

    <div class="grid three section-gap">
      <div class="panel workflow-card">
        <span class="number">1</span>
        <strong>上传或采集</strong>
        <span class="muted">CSV 走 OSS 上传，爬虫走 Python 服务低频采集。</span>
      </div>
      <div class="panel workflow-card">
        <span class="number">2</span>
        <strong>清洗入库</strong>
        <span class="muted">Java 创建导入任务，Python 完成字段清洗、情感和标签预处理。</span>
      </div>
      <div class="panel workflow-card">
        <span class="number">3</span>
        <strong>分析展示</strong>
        <span class="muted">导入完成后可进入评论工作台生成报告、回复和对比分析。</span>
        <ShieldCheck class="text-green" :size="22" />
      </div>
    </div>
  </section>
</template>
