import { computed, ref } from 'vue'

import { subscribeAiJob } from '@/api/aiJobStream'
import { aiJobApi } from '@/api/modules'
import type { AiJob, AiJobEvent } from '@/api/types'

const terminalStatuses = new Set(['success', 'failed', 'timed_out', 'cancelled'])

export function useAiJob() {
  const job = ref<AiJob>()
  const connected = ref(false)
  const reconnecting = ref(false)
  const previewText = ref('')
  const lastEventId = ref<number>()
  const lastDeltaId = ref<number>()
  let controller: AbortController | undefined
  let reconnectTimer: ReturnType<typeof setTimeout> | undefined

  const isTerminal = computed(() => Boolean(job.value && terminalStatuses.has(job.value.taskStatus)))

  const refresh = async (jobId = job.value?.jobId) => {
    if (!jobId) return undefined
    job.value = await aiJobApi.job(jobId)
    return job.value
  }

  const applyEvent = (event: AiJobEvent) => {
    if (event.eventType === 'text_delta') {
      if (!event.textDelta || (event.deltaId != null && event.deltaId <= (lastDeltaId.value || 0))) return
      previewText.value += event.textDelta
      lastDeltaId.value = event.deltaId ?? lastDeltaId.value
      return
    }
    lastEventId.value = event.eventId ?? lastEventId.value
    job.value = {
      ...(job.value || { jobId: event.jobId, jobType: event.jobType, targetType: '', targetId: '', taskStatus: 'pending' }),
      ...event
    }
  }

  const stop = () => {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = undefined
    controller?.abort()
    controller = undefined
    connected.value = false
    reconnecting.value = false
  }

  const connect = (jobId: number) => {
    const connection = new AbortController()
    controller = connection
    connected.value = true
    reconnecting.value = false
    void subscribeAiJob(jobId, (event) => {
      applyEvent(event)
      if (terminalStatuses.has(event.taskStatus)) stop()
    }, connection.signal, lastEventId.value).catch(async () => {
      if (connection.signal.aborted || controller !== connection) return
      connected.value = false
      reconnecting.value = true
      previewText.value = ''
      lastDeltaId.value = undefined
      await refresh(jobId)
      if (!isTerminal.value && controller === connection) {
        reconnectTimer = setTimeout(() => connect(jobId), 1000)
      }
    })
  }

  const start = async (jobId: number) => {
    stop()
    lastEventId.value = undefined
    lastDeltaId.value = undefined
    previewText.value = ''
    await refresh(jobId)
    if (isTerminal.value) return
    connect(jobId)
  }

  const cancel = async () => {
    if (job.value) job.value = await aiJobApi.cancel(job.value.jobId)
  }

  const retry = async () => {
    if (!job.value) return undefined
    const created = await aiJobApi.retry(job.value.jobId)
    await start(created.jobId)
    return created
  }

  return { job, connected, reconnecting, previewText, isTerminal, refresh, start, stop, cancel, retry }
}
