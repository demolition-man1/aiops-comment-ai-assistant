<script setup lang="ts">
import { Bot, Pencil, Plus, RefreshCw, Star } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { promptTemplateApi } from '@/api/modules'
import type { PromptTemplate } from '@/api/types'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const templates = ref<PromptTemplate[]>([])
const businessTypes = ['report', 'content', 'negative_reply', 'translation', 'product_compare']
const languages = ['zh-CN', 'en-US', 'pt-BR']

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  businessType: '',
  language: '',
  enabled: ''
})

const page = reactive({
  total: 0
})

const defaultForm = (): Partial<PromptTemplate> => ({
  templateName: '',
  businessType: 'report',
  language: 'zh-CN',
  templateContent: '',
  variableSchema: '',
  defaultFlag: 0,
  enabled: 1,
  remark: ''
})

const form = reactive<Partial<PromptTemplate>>(defaultForm())

const displayBusinessType = (value?: string) => {
  const key = value?.trim()
  return key && businessTypes.includes(key) ? t(`prompts.businessTypes.${key}`) : key || t('common.unknown')
}

const loadTemplates = async () => {
  loading.value = true
  try {
    const result = await promptTemplateApi.page({
      ...query,
      enabled: query.enabled === '' ? undefined : Number(query.enabled)
    })
    templates.value = result.records || []
    page.total = result.total || 0
  } finally {
    loading.value = false
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

const openEditDialog = (row: PromptTemplate) => {
  resetForm()
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveTemplate = async () => {
  if (!form.templateName?.trim()) {
    ElMessage.warning(t('prompts.nameRequired'))
    return
  }
  if (!form.templateContent?.trim()) {
    ElMessage.warning(t('prompts.contentRequired'))
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await promptTemplateApi.update(editingId.value, form)
      ElMessage.success(t('prompts.updated'))
    } else {
      await promptTemplateApi.create(form)
      ElMessage.success(t('prompts.created'))
    }
    dialogVisible.value = false
    await loadTemplates()
  } finally {
    saving.value = false
  }
}

const toggleTemplate = async (row: PromptTemplate) => {
  const previousValue = Number(row.enabled) === 1 ? 0 : 1
  try {
    await promptTemplateApi.updateStatus(row.id, Number(row.enabled))
    ElMessage.success(Number(row.enabled) === 1 ? t('prompts.enabled') : t('prompts.disabled'))
  } catch (error) {
    row.enabled = previousValue
    throw error
  }
}

const setDefault = async (row: PromptTemplate) => {
  await promptTemplateApi.setDefault(row.id)
  ElMessage.success(t('prompts.defaultSet'))
  await loadTemplates()
}

onMounted(loadTemplates)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('prompts.title') }}</h2>
        <span class="muted">{{ t('prompts.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadTemplates">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="16" />
          {{ t('prompts.createTemplate') }}
        </el-button>
      </div>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item :label="t('prompts.keyword')">
          <el-input v-model="query.keyword" clearable :placeholder="t('prompts.keywordPlaceholder')" style="width: 220px" />
        </el-form-item>
        <el-form-item :label="t('prompts.businessType')">
          <el-select v-model="query.businessType" clearable :placeholder="t('common.all')" style="width: 170px">
            <el-option
              v-for="type in businessTypes"
              :key="type"
              :label="displayBusinessType(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('prompts.language')">
          <el-select v-model="query.language" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option v-for="language in languages" :key="language" :label="language" :value="language" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('prompts.enabledState')">
          <el-select v-model="query.enabled" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('settings.enabled')" value="1" />
            <el-option :label="t('settings.disabled')" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTemplates">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="templates" height="520" size="small">
        <el-table-column :label="t('prompts.templateName')" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="solution-title-cell">
              <Bot class="text-blue" :size="16" />
              <span>{{ row.templateName }}</span>
              <el-tag v-if="row.defaultFlag === 1" size="small" type="success" effect="plain">{{ t('prompts.defaultFlag') }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('prompts.businessType')" width="150">
          <template #default="{ row }">{{ displayBusinessType(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="language" :label="t('prompts.language')" width="100" />
        <el-table-column prop="remark" :label="t('prompts.remark')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updateTime" :label="t('common.createdAt')" width="180" show-overflow-tooltip />
        <el-table-column :label="t('prompts.enabledState')" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="() => toggleTemplate(row)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">
              <Pencil :size="14" />
              {{ t('common.edit') }}
            </el-button>
            <el-button size="small" type="success" :disabled="row.defaultFlag === 1" @click="setDefault(row)">
              <Star :size="14" />
              {{ t('prompts.setDefault') }}
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
        @change="loadTemplates"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('prompts.editTemplate') : t('prompts.createTemplate')"
      width="820px"
    >
      <el-form label-position="top">
        <div class="inline-fields">
          <el-form-item :label="t('prompts.templateName')" style="flex: 1">
            <el-input v-model="form.templateName" :placeholder="t('prompts.namePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('prompts.businessType')" style="width: 210px">
            <el-select v-model="form.businessType" style="width: 100%">
              <el-option
                v-for="type in businessTypes"
                :key="type"
                :label="displayBusinessType(type)"
                :value="type"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('prompts.language')" style="width: 150px">
            <el-select v-model="form.language" style="width: 100%">
              <el-option v-for="language in languages" :key="language" :label="language" :value="language" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('prompts.templateContent')">
          <el-input
            v-model="form.templateContent"
            type="textarea"
            :autosize="{ minRows: 8, maxRows: 14 }"
            :placeholder="t('prompts.contentPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('prompts.variableSchema')">
          <el-input
            v-model="form.variableSchema"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            :placeholder="t('prompts.variablePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('prompts.remark')">
          <el-input v-model="form.remark" :placeholder="t('prompts.remarkPlaceholder')" />
        </el-form-item>
        <div class="inline-fields">
          <el-form-item :label="t('prompts.defaultFlag')" style="flex: 1">
            <el-switch v-model="form.defaultFlag" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item :label="t('prompts.enabledState')" style="flex: 1">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveTemplate">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>
