import { fetchEventSource } from '@microsoft/fetch-event-source'

import type { AiJobEvent } from './types'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

export function subscribeAiJob(
  jobId: number,
  onEvent: (event: AiJobEvent) => void,
  signal: AbortSignal,
  lastEventId?: number
): Promise<void> {
  const token = localStorage.getItem('aiops_token')
  const headers: Record<string, string> = { Accept: 'text/event-stream' }
  if (token) headers.Authorization = `Bearer ${token}`
  if (lastEventId != null) headers['Last-Event-ID'] = String(lastEventId)

  return fetchEventSource(`${apiBaseUrl}/ai/jobs/${jobId}/events`, {
    method: 'GET',
    headers,
    signal,
    openWhenHidden: true,
    async onopen(response) {
      if (!response.ok || !response.headers.get('content-type')?.includes('text/event-stream')) {
        throw new Error(`AI job stream unavailable (${response.status})`)
      }
    },
    onmessage(message) {
      if (!message.data) return
      try {
        onEvent({
          ...(JSON.parse(message.data) as AiJobEvent),
          eventType: message.event || 'stage',
          eventId: Number(message.id || 0) || undefined
        })
      } catch {
        // MySQL snapshot refresh remains authoritative when a transient event is malformed.
      }
    },
    onerror(error) {
      if (!signal.aborted) throw error
    }
  })
}
