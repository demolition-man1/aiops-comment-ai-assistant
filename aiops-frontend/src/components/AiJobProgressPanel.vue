<script setup lang="ts">
import { ExternalLink, RotateCcw, X } from 'lucide-vue-next'
import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import type { AiJob } from '@/api/types'
import { useAiJob } from '@/composables/useAiJob'

defineOptions({ name: 'AiJobProgressPanel' })

const props = defineProps<{ job: AiJob }>()
const emit = defineEmits<{ result: [job: AiJob] }>()
const { t } = useI18n()
const live = useAiJob()
const currentJob = live.job

const start = () => void live.start(props.job.jobId)
const isActive = (status?: string) => status === 'pending' || status === 'processing'

onMounted(start)
onBeforeUnmount(live.stop)
watch(() => props.job.jobId, start)
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
</style>
