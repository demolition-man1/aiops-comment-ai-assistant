<script setup lang="ts">
import { ExternalLink, RotateCcw, X } from 'lucide-vue-next'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import type { AiJob } from '@/api/types'
import { useAiJob } from '@/composables/useAiJob'

defineOptions({ name: 'AiJobProgressPanel' })

const props = defineProps<{ job: AiJob }>()
const emit = defineEmits<{ result: [job: AiJob] }>()
const { t } = useI18n()
const live = useAiJob()
const currentJob = live.job
const emittedResultJobId = ref<number>()

const start = () => void live.start(props.job.jobId)
const isActive = (status?: string) => status === 'pending' || status === 'processing'

onMounted(start)
onBeforeUnmount(live.stop)
watch(() => props.job.jobId, () => {
  emittedResultJobId.value = undefined
  start()
})
watch(() => currentJob.value?.taskStatus, (status) => {
  const resolvedJob = currentJob.value || props.job
  if (status === 'success' && emittedResultJobId.value !== resolvedJob.jobId) {
    emittedResultJobId.value = resolvedJob.jobId
    emit('result', resolvedJob)
  }
})
</script>

<template>
  <div class="ai-job-progress" :aria-label="t('jobs.progress')">
    <div class="status-line">
      <div>
        <strong>{{ t(`enums.taskStatus.${currentJob?.taskStatus || props.job.taskStatus}`) }}</strong>
        <span class="muted">{{ t(`jobs.stages.${currentJob?.jobStage || props.job.jobStage || 'preparing'}`) }}</span>
      </div>
      <span v-if="live.reconnecting" class="muted">{{ t('jobs.reconnecting') }}</span>
      <span v-else-if="live.connected" class="muted">{{ t('jobs.live') }}</span>
    </div>
    <el-progress :percentage="Number(currentJob?.progress ?? props.job.progress ?? 0)" :stroke-width="7" />
    <div v-if="live.previewText" class="text-preview" aria-live="polite">
      <span class="muted">{{ t('jobs.preview') }}</span>
      {{ live.previewText }}
    </div>
    <div v-if="currentJob?.queueLatencyMs !== undefined || currentJob?.providerLatencyMs !== undefined" class="timing-line muted">
      {{ t('jobs.queueLatency') }} {{ currentJob?.queueLatencyMs ?? 0 }}ms ·
      {{ t('jobs.providerLatency') }} {{ currentJob?.providerLatencyMs ?? 0 }}ms ·
      {{ t('jobs.totalLatency') }} {{ currentJob?.totalLatencyMs ?? 0 }}ms
    </div>
    <div class="ai-job-actions">
      <el-tooltip :content="t('jobs.cancel')">
        <el-button v-if="isActive(currentJob?.taskStatus || props.job.taskStatus)" circle size="small" type="danger" plain @click="live.cancel">
          <X :size="14" />
        </el-button>
      </el-tooltip>
      <el-tooltip :content="t('jobs.retry')">
        <el-button v-if="['failed', 'timed_out', 'cancelled'].includes(currentJob?.taskStatus || props.job.taskStatus)" circle size="small" @click="live.retry">
          <RotateCcw :size="14" />
        </el-button>
      </el-tooltip>
      <el-tooltip :content="t('jobs.openResult')">
        <el-button v-if="(currentJob?.taskStatus || props.job.taskStatus) === 'success'" circle size="small" type="primary" plain @click="emit('result', currentJob || props.job)">
          <ExternalLink :size="14" />
        </el-button>
      </el-tooltip>
    </div>
  </div>
</template>

<style scoped>
.ai-job-progress { display: grid; gap: 8px; min-width: 180px; }
.ai-job-actions { display: flex; justify-content: flex-end; gap: 6px; min-height: 28px; }
.timing-line { font-size: 12px; white-space: nowrap; }
.text-preview { max-height: 92px; overflow: auto; white-space: pre-wrap; font-size: 12px; line-height: 1.5; }
</style>
