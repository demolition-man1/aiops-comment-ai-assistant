<script setup lang="ts">
import { GitCompareArrows, RefreshCw, Sparkles } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { analysisApi, productApi } from '@/api/modules'
import type { Product, ProductCompareReport } from '@/api/types'

const loading = ref(false)
const products = ref<Product[]>([])
const reports = ref<ProductCompareReport[]>([])
const currentReport = ref<ProductCompareReport>()
const form = reactive({
  leftProductId: '',
  rightProductId: '',
  forceRefresh: false
})

const loadProducts = async () => {
  const data = await productApi.page({ pageNum: 1, pageSize: 50 })
  products.value = data.records || []
}

const loadReports = async () => {
  const data = await analysisApi.comparePage({ pageNum: 1, pageSize: 8 })
  reports.value = data.records || []
}

const compare = async () => {
  if (!form.leftProductId || !form.rightProductId) {
    ElMessage.warning('请选择两个商品')
    return
  }
  if (form.leftProductId === form.rightProductId) {
    ElMessage.warning('两个商品不能相同')
    return
  }

  loading.value = true
  try {
    currentReport.value = await analysisApi.compare({
      leftProductId: form.leftProductId,
      rightProductId: form.rightProductId,
      language: 'zh-CN',
      forceRefresh: form.forceRefresh
    })
    ElMessage.success('商品对比报告已生成')
    await loadReports()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadReports()])
})
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">商品 A / B 对比</h2>
        <span class="muted">对比两款商品的评论痛点、优势短板和运营建议</span>
      </div>
      <el-button @click="loadReports">
        <RefreshCw :size="16" />
        刷新历史
      </el-button>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item label="商品 A">
          <el-select v-model="form.leftProductId" filterable clearable placeholder="选择商品" style="width: 300px">
            <el-option
              v-for="item in products"
              :key="item.productId"
              :label="`${item.productId} ${item.categoryName || ''}`"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品 B">
          <el-select v-model="form.rightProductId" filterable clearable placeholder="选择商品" style="width: 300px">
            <el-option
              v-for="item in products"
              :key="item.productId"
              :label="`${item.productId} ${item.categoryName || ''}`"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.forceRefresh">强制重新生成</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="compare">
            <GitCompareArrows :size="16" />
            开始对比
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="panel-title">当前对比报告</div>
        <el-empty v-if="!currentReport" description="选择两个商品后生成 AI 对比报告" />
        <template v-else>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="商品 A">{{ currentReport.leftProductId }}</el-descriptions-item>
            <el-descriptions-item label="商品 B">{{ currentReport.rightProductId }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ currentReport.modelName || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="insight-block section-gap">
            <strong>对比总结</strong>
            <p>{{ currentReport.compareSummary || '-' }}</p>
          </div>
          <div class="insight-block">
            <strong>优势分析</strong>
            <p>{{ currentReport.advantageAnalysis || '-' }}</p>
          </div>
          <div class="insight-block">
            <strong>风险分析</strong>
            <p>{{ currentReport.riskAnalysis || '-' }}</p>
          </div>
          <div class="insight-block">
            <strong>运营建议</strong>
            <p>{{ currentReport.operationSuggestions || '-' }}</p>
          </div>
        </template>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">历史对比记录</div>
          <Sparkles class="text-amber" :size="20" />
        </div>
        <el-table :data="reports" height="430" size="small">
          <el-table-column prop="leftProductId" label="商品A" show-overflow-tooltip />
          <el-table-column prop="rightProductId" label="商品B" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="160" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="currentReport = row">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>
