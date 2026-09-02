import { readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const types = await readFile(new URL('../src/api/types.ts', import.meta.url), 'utf8')
const logsView = await readFile(new URL('../src/views/AiCallLogView.vue', import.meta.url), 'utf8')
const taskView = await readFile(new URL('../src/views/TaskCenterView.vue', import.meta.url), 'utf8')
const locale = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')

test('AI observability contracts expose separate queue provider and total timing', () => {
  assert.match(types, /queueLatencyMs/)
  assert.match(types, /totalLatencyMs/)
  assert.match(logsView, /aiLogs\.queueLatency/)
  assert.match(logsView, /aiLogs\.providerLatency/)
  assert.match(logsView, /aiLogs\.totalLatency/)
  assert.match(taskView, /jobs\.queueLatency/)
})

test('AI observability renders safe error categories instead of raw failures', () => {
  assert.match(types, /errorCode/)
  assert.match(logsView, /errorCategory/)
  assert.match(locale, /provider_timeout/)
  assert.doesNotMatch(logsView, /row\.errorMessage/)
})
