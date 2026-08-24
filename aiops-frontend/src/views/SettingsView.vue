<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Bot, Database, RotateCcw, Save, Shield, SlidersHorizontal } from 'lucide-vue-next'
import { computed, reactive } from 'vue'
import { useI18n } from 'vue-i18n'

import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const settings = reactive({
  taskPollingSeconds: Number(localStorage.getItem('aiops_task_polling_seconds') || 3),
  alertNegativeRate: Number(localStorage.getItem('aiops_alert_negative_rate') || 20),
  alertNegativeCount: Number(localStorage.getItem('aiops_alert_negative_count') || 5),
  aiRateLimitPerMinute: Number(localStorage.getItem('aiops_ai_rate_limit_per_minute') || 20),
  enableResultCache: localStorage.getItem('aiops_enable_result_cache') !== 'false',
  enableAlert: localStorage.getItem('aiops_enable_alert') !== 'false'
})

const tokenStatus = computed(() => (localStorage.getItem('aiops_token') ? t('settings.loggedIn') : t('settings.notLoggedIn')))
const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
const showRuntimeStatus = computed(() => import.meta.env.DEV || auth.user?.role === 'admin')

const save = () => {
  localStorage.setItem('aiops_task_polling_seconds', String(settings.taskPollingSeconds))
  localStorage.setItem('aiops_alert_negative_rate', String(settings.alertNegativeRate))
  localStorage.setItem('aiops_alert_negative_count', String(settings.alertNegativeCount))
  localStorage.setItem('aiops_ai_rate_limit_per_minute', String(settings.aiRateLimitPerMinute))
  localStorage.setItem('aiops_enable_result_cache', String(settings.enableResultCache))
  localStorage.setItem('aiops_enable_alert', String(settings.enableAlert))
  ElMessage.success(t('settings.saved'))
}

const reset = () => {
  Object.assign(settings, {
    taskPollingSeconds: 3,
    alertNegativeRate: 20,
    alertNegativeCount: 5,
    aiRateLimitPerMinute: 20,
    enableResultCache: true,
    enableAlert: true
  })
  save()
}
</script>

<template>
  <section class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ t('settings.title') }}</h2>
        <span class="muted">{{ t('settings.subtitle') }}</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="reset">
          <RotateCcw :size="16" />
          {{ t('settings.restoreDefaults') }}
        </el-button>
        <el-button type="primary" @click="save">
          <Save :size="16" />
          {{ t('settings.saveSettings') }}
        </el-button>
      </div>
    </div>

    <div :class="['grid', showRuntimeStatus ? 'two' : 'single']">
      <div v-if="showRuntimeStatus" class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('settings.runtimeStatus') }}</div>
          <Database class="text-blue" :size="22" />
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item :label="t('settings.currentApi')">{{ apiBase }}</el-descriptions-item>
          <el-descriptions-item :label="t('settings.loginStatus')">{{ tokenStatus }}</el-descriptions-item>
          <el-descriptions-item :label="t('settings.authMethod')">JWT Bearer Token</el-descriptions-item>
          <el-descriptions-item :label="t('settings.serviceStatus')">{{ t('settings.serviceNormal') }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('settings.taskCache') }}</div>
          <SlidersHorizontal class="text-green" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item :label="t('settings.taskPollingInterval')">
            <el-input-number v-model="settings.taskPollingSeconds" :min="1" :max="30" />
            <span class="field-unit">{{ t('settings.seconds') }}</span>
          </el-form-item>
          <el-form-item :label="t('settings.resultCache')">
            <el-switch v-model="settings.enableResultCache" :active-text="t('settings.enabled')" :inactive-text="t('settings.disabled')" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('settings.alertThreshold') }}</div>
          <Shield class="text-amber" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item :label="t('settings.enableAlert')">
            <el-switch v-model="settings.enableAlert" :active-text="t('settings.enabled')" :inactive-text="t('settings.disabled')" />
          </el-form-item>
          <el-form-item :label="t('settings.negativeRateThreshold')">
            <el-slider v-model="settings.alertNegativeRate" :min="1" :max="80" show-input />
          </el-form-item>
          <el-form-item :label="t('settings.negativeCountThreshold')">
            <el-input-number v-model="settings.alertNegativeCount" :min="1" :max="100" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">{{ t('settings.aiControl') }}</div>
          <Bot class="text-red" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item :label="t('settings.aiRateLimit')">
            <el-input-number v-model="settings.aiRateLimitPerMinute" :min="1" :max="120" />
          </el-form-item>
          <el-alert
            type="info"
            show-icon
            :closable="false"
            :title="t('settings.aiLimitTip')"
          />
        </el-form>
      </div>
    </div>
  </section>
</template>
