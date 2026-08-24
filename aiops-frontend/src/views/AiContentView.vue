<script setup lang="ts">
import { Copy, PenLine, RefreshCw, WandSparkles } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import { aiApi, productApi } from '@/api/modules'
import type { AiContent, Product } from '@/api/types'

const loading = ref(false)
const products = ref<Product[]>([])
const history = ref<AiContent[]>([])
const current = ref<AiContent>()
const form = reactive({
  targetType: 'product',
  targetId: '',
  contentType: 'product_title',
  styleType: 'simple',
  language: 'zh-CN',
  extraRequirement: ''
})

const loadProducts = async () => {
  const data = await productApi.page({ pageNum: 1, pageSize: 50 })
  products.value = data.records || []
}

const loadHistory = async () => {
  const data = await aiApi.contents({ pageNum: 1, pageSize: 10 })
  history.value = data.records || []
}

const generate = async () => {
  if (!form.targetId) {
    ElMessage.warning('请选择或输入目标商品ID')
    return
  }
  loading.value = true
  try {
    current.value = await aiApi.content({ ...form })
    ElMessage.success('AI 文案已生成')
    await loadHistory()
  } finally {
    loading.value = false
  }
}

const copyContent = async (text?: string) => {
  if (!text) {
    return
  }
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadHistory()])
})
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">AI 文案生成</h2>
        <span class="muted">基于评论反馈和商品信息生成标题、详情、促销和短视频文案</span>
      </div>
      <el-button @click="loadHistory">
        <RefreshCw :size="16" />
        刷新历史
      </el-button>
    </div>

    <div class="grid two">
      <div class="panel">
        <div class="panel-title">生成参数</div>
        <el-form label-position="top">
          <el-form-item label="目标商品">
            <el-select
              v-model="form.targetId"
              filterable
              allow-create
              clearable
              placeholder="选择或输入 product_id"
              style="width: 100%"
            >
              <el-option
                v-for="item in products"
                :key="item.productId"
                :label="`${item.productId} ${item.categoryName || ''}`"
                :value="item.productId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="文案类型">
            <el-segmented
              v-model="form.contentType"
              :options="[
                { label: '商品标题', value: 'product_title' },
                { label: '详情介绍', value: 'detail_description' },
                { label: '短视频', value: 'short_video' },
                { label: '促销话术', value: 'promotion' }
              ]"
            />
          </el-form-item>
          <el-form-item label="风格">
            <el-radio-group v-model="form.styleType">
              <el-radio-button label="simple">简约风</el-radio-button>
              <el-radio-button label="grass">种草风</el-radio-button>
              <el-radio-button label="cost_effective">性价比风</el-radio-button>
              <el-radio-button label="cross_border">跨境风</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="补充要求">
            <el-input
              v-model="form.extraRequirement"
              type="textarea"
              :rows="5"
              placeholder="比如：突出物流快、包装好、适合节日促销"
            />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="generate">
            <WandSparkles :size="16" />
            生成文案
          </el-button>
        </el-form>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">生成结果</div>
          <el-button size="small" :disabled="!current?.generatedContent" @click="copyContent(current?.generatedContent)">
            <Copy :size="14" />
            复制
          </el-button>
        </div>
        <div class="content-result">
          {{ current?.generatedContent || '填写左侧参数后，AI 生成的文案会显示在这里。' }}
        </div>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="status-line">
        <div class="panel-title">文案历史</div>
        <PenLine class="text-blue" :size="20" />
      </div>
      <el-table :data="history" height="360" size="small">
        <el-table-column prop="recordId" label="ID" width="90" />
        <el-table-column prop="modelName" label="模型" width="140" />
        <el-table-column label="内容" show-overflow-tooltip>
          <template #default="{ row }">{{ row.generatedContent }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="current = row">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
