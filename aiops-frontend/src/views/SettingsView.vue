<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Bot, Database, RotateCcw, Save, Shield, SlidersHorizontal } from 'lucide-vue-next'
import { computed, reactive } from 'vue'

const settings = reactive({
  taskPollingSeconds: Number(localStorage.getItem('aiops_task_polling_seconds') || 3),
  alertNegativeRate: Number(localStorage.getItem('aiops_alert_negative_rate') || 20),
  alertNegativeCount: Number(localStorage.getItem('aiops_alert_negative_count') || 5),
  aiRateLimitPerMinute: Number(localStorage.getItem('aiops_ai_rate_limit_per_minute') || 20),
  enableResultCache: localStorage.getItem('aiops_enable_result_cache') !== 'false',
  enableAlert: localStorage.getItem('aiops_enable_alert') !== 'false'
})

const tokenStatus = computed(() => (localStorage.getItem('aiops_token') ? '已登录' : '未登录'))
const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080'

const save = () => {
  localStorage.setItem('aiops_task_polling_seconds', String(settings.taskPollingSeconds))
  localStorage.setItem('aiops_alert_negative_rate', String(settings.alertNegativeRate))
  localStorage.setItem('aiops_alert_negative_count', String(settings.alertNegativeCount))
  localStorage.setItem('aiops_ai_rate_limit_per_minute', String(settings.aiRateLimitPerMinute))
  localStorage.setItem('aiops_enable_result_cache', String(settings.enableResultCache))
  localStorage.setItem('aiops_enable_alert', String(settings.enableAlert))
  ElMessage.success('设置已保存')
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
        <h2 class="section-title">系统设置</h2>
        <span class="muted">联调参数、告警阈值、AI 调用与缓存偏好</span>
      </div>
      <div class="toolbar-actions">
        <el-button @click="reset">
          <RotateCcw :size="16" />
          恢复默认
        </el-button>
        <el-button type="primary" @click="save">
          <Save :size="16" />
          保存设置
        </el-button>
      </div>
    </div>

    <div class="grid two">
      <div class="panel">
        <div class="status-line">
          <div class="panel-title">联调状态</div>
          <Database class="text-blue" :size="22" />
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="前端 API Base">{{ apiBase }}</el-descriptions-item>
          <el-descriptions-item label="后端代理地址">{{ backendUrl }}</el-descriptions-item>
          <el-descriptions-item label="登录状态">{{ tokenStatus }}</el-descriptions-item>
          <el-descriptions-item label="鉴权方式">Authorization: Bearer token</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">任务与缓存</div>
          <SlidersHorizontal class="text-green" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item label="任务轮询间隔">
            <el-input-number v-model="settings.taskPollingSeconds" :min="1" :max="30" />
            <span class="field-unit">秒</span>
          </el-form-item>
          <el-form-item label="分析结果缓存">
            <el-switch v-model="settings.enableResultCache" active-text="开启" inactive-text="关闭" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="grid two section-gap">
      <div class="panel">
        <div class="status-line">
          <div class="panel-title">告警阈值</div>
          <Shield class="text-amber" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item label="启用告警">
            <el-switch v-model="settings.enableAlert" active-text="开启" inactive-text="关闭" />
          </el-form-item>
          <el-form-item label="负面占比阈值">
            <el-slider v-model="settings.alertNegativeRate" :min="1" :max="80" show-input />
          </el-form-item>
          <el-form-item label="负面评论数量阈值">
            <el-input-number v-model="settings.alertNegativeCount" :min="1" :max="100" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="status-line">
          <div class="panel-title">AI 调用控制</div>
          <Bot class="text-red" :size="22" />
        </div>
        <el-form label-position="top">
          <el-form-item label="每分钟 AI 调用上限">
            <el-input-number v-model="settings.aiRateLimitPerMinute" :min="1" :max="120" />
          </el-form-item>
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="正式限流由 Java 后端 Redis 配置执行，这里保存前端联调偏好。"
          />
        </el-form>
      </div>
    </div>
  </section>
</template>
