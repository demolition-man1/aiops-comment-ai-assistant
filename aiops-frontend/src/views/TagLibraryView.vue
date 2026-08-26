<script setup lang="ts">
import { Pencil, Plus, RefreshCw, Tags } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { tagApi } from '@/api/modules'
import type { CustomTag } from '@/api/types'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number>()
const tags = ref<CustomTag[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  tagGroup: '',
  enabled: ''
})

const page = reactive({
  total: 0
})

const defaultForm = (): Partial<CustomTag> => ({
  tagName: '',
  tagGroup: '',
  color: '#409EFF',
  description: '',
  sortOrder: 0,
  enabled: 1
})

const form = reactive<Partial<CustomTag>>(defaultForm())

const loadTags = async () => {
  loading.value = true
  try {
    const result = await tagApi.page({
      ...query,
      enabled: query.enabled === '' ? undefined : Number(query.enabled)
    })
    tags.value = result.records || []
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

const openEditDialog = (row: CustomTag) => {
  resetForm()
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveTag = async () => {
  if (!form.tagName?.trim()) {
    ElMessage.warning(t('tags.tagNameRequired'))
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await tagApi.update(editingId.value, form)
      ElMessage.success(t('tags.updated'))
    } else {
      await tagApi.create(form)
      ElMessage.success(t('tags.created'))
    }
    dialogVisible.value = false
    await loadTags()
  } finally {
    saving.value = false
  }
}

const toggleTag = async (row: CustomTag) => {
  const previousValue = Number(row.enabled) === 1 ? 0 : 1
  try {
    await tagApi.updateStatus(row.id, Number(row.enabled))
    ElMessage.success(Number(row.enabled) === 1 ? t('tags.enabled') : t('tags.disabled'))
  } catch (error) {
    row.enabled = previousValue
    throw error
  }
}

onMounted(loadTags)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('tags.title') }}</h2>
        <span class="muted">{{ t('tags.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="loadTags">
          <RefreshCw :size="16" />
          {{ t('common.refresh') }}
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="16" />
          {{ t('tags.createTag') }}
        </el-button>
      </div>
    </div>

    <div class="panel">
      <el-form :inline="true">
        <el-form-item :label="t('tags.keyword')">
          <el-input v-model="query.keyword" clearable :placeholder="t('tags.keywordPlaceholder')" style="width: 220px" />
        </el-form-item>
        <el-form-item :label="t('tags.tagGroup')">
          <el-input v-model="query.tagGroup" clearable :placeholder="t('tags.tagGroupPlaceholder')" style="width: 180px" />
        </el-form-item>
        <el-form-item :label="t('tags.enabledState')">
          <el-select v-model="query.enabled" clearable :placeholder="t('common.all')" style="width: 130px">
            <el-option :label="t('settings.enabled')" value="1" />
            <el-option :label="t('settings.disabled')" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTags">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tags" height="520" size="small">
        <el-table-column :label="t('tags.tagName')" min-width="180">
          <template #default="{ row }">
            <el-tag :color="row.color || '#409EFF'" effect="dark">
              <Tags :size="13" />
              {{ row.tagName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tagGroup" :label="t('tags.tagGroup')" width="150" show-overflow-tooltip />
        <el-table-column prop="description" :label="t('tags.description')" min-width="260" show-overflow-tooltip />
        <el-table-column prop="sortOrder" :label="t('tags.sortOrder')" width="90" />
        <el-table-column :label="t('tags.enabledState')" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              :active-value="1"
              :inactive-value="0"
              @change="() => toggleTag(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" :label="t('common.createdAt')" width="180" show-overflow-tooltip />
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
        @change="loadTags"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? t('tags.editTag') : t('tags.createTag')" width="560px">
      <el-form label-position="top">
        <el-form-item :label="t('tags.tagName')">
          <el-input v-model="form.tagName" :placeholder="t('tags.tagNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('tags.tagGroup')">
          <el-input v-model="form.tagGroup" :placeholder="t('tags.tagGroupPlaceholder')" />
        </el-form-item>
        <div class="inline-fields">
          <el-form-item :label="t('tags.color')" style="width: 180px">
            <el-color-picker v-model="form.color" />
          </el-form-item>
          <el-form-item :label="t('tags.sortOrder')" style="flex: 1">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          </el-form-item>
        </div>
        <el-form-item :label="t('tags.description')">
          <el-input
            v-model="form.description"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 5 }"
            :placeholder="t('tags.descriptionPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('tags.enabledState')">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveTag">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>
