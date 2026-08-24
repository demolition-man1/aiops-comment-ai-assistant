<script setup lang="ts">
import { Copy, PenLine, RefreshCw, WandSparkles } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { aiApi, productApi } from '@/api/modules'
import type { AiContent, Product } from '@/api/types'
import { useLocaleStore } from '@/stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()
const loading = ref(false)
const products = ref<Product[]>([])
const history = ref<AiContent[]>([])
const current = ref<AiContent>()
const form = reactive({
  targetType: 'product',
  targetId: '',
  contentType: 'product_title',
  styleType: 'simple',
  extraRequirement: ''
})

const contentTypeOptions = computed(() => [
  { label: t('aiContent.productTitle'), value: 'product_title' },
  { label: t('aiContent.detailDescription'), value: 'detail_description' },
  { label: t('aiContent.shortVideo'), value: 'short_video' },
  { label: t('aiContent.promotion'), value: 'promotion' }
])

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
    ElMessage.warning(t('aiContent.targetRequired'))
    return
  }
  loading.value = true
  try {
    current.value = await aiApi.content({ ...form, language: localeStore.locale })
    ElMessage.success(t('aiContent.generated'))
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
  ElMessage.success(t('common.copied'))
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadHistory()])
})
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('aiContent.title') }}</h2>
        <span class="muted">{{ t('aiContent.subtitle') }}</span>
      </div>
      <el-button @click="loadHistory">
        <RefreshCw :size="16" />
        {{ t('common.refreshHistory') }}
      </el-button>
    </div>

    <div class="grid two">
      <div class="panel">
        <div class="panel-title">{{ t('aiContent.params') }}</div>
        <el-form label-position="top">
          <el-form-item :label="t('aiContent.targetProduct')">
            <el-select
              v-model="form.targetId"
              filterable
              allow-create
              clearable
              :placeholder="t('aiContent.targetPlaceholder')"
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
          <el-form-item :label="t('aiContent.contentType')">
            <el-segmented
              v-model="form.contentType"
              :options="contentTypeOptions"
            />
          </el-form-item>
          <el-form-item :label="t('aiContent.style')">
            <el-radio-group v-model="form.styleType">
              <el-radio-button label="simple">{{ t('aiContent.simple') }}</el-radio-button>
              <el-radio-button label="grass">{{ t('aiContent.grass') }}</el-radio-button>
              <el-radio-button label="cost_effective">{{ t('aiContent.costEffective') }}</el-radio-button>
              <el-radio-button label="cross_border">{{ t('aiContent.crossBorder') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="t('aiContent.extraRequirement')">
            <el-input
              v-model="form.extraRequirement"
              type="textarea"
              :rows="5"
              :placeholder="t('aiContent.extraPlaceholder')"
            />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="generate">
            <WandSparkles :size="16" />
            {{ t('aiContent.generate') }}
          </el-button>
        </el-form>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('aiContent.result') }}</div>
          <el-button size="small" :disabled="!current?.generatedContent" @click="copyContent(current?.generatedContent)">
            <Copy :size="14" />
            {{ t('common.copy') }}
          </el-button>
        </div>
        <div class="content-result">
          {{ current?.generatedContent || t('aiContent.emptyResult') }}
        </div>
      </div>
    </div>

    <div class="panel section-gap">
      <div class="status-line">
        <div class="panel-title">{{ t('aiContent.history') }}</div>
        <PenLine class="text-blue" :size="20" />
      </div>
      <el-table :data="history" height="360" size="small">
        <el-table-column prop="recordId" :label="t('common.id')" width="90" />
        <el-table-column prop="modelName" :label="t('common.model')" width="140" />
        <el-table-column :label="t('common.content')" show-overflow-tooltip>
          <template #default="{ row }">{{ row.generatedContent }}</template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="100">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="current = row">{{ t('common.view') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
