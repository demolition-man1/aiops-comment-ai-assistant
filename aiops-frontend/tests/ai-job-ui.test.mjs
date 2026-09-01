import { readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const component = await readFile(new URL('../src/components/AiJobProgressPanel.vue', import.meta.url), 'utf8')
const taskCenter = await readFile(new URL('../src/views/TaskCenterView.vue', import.meta.url), 'utf8')
const zhLocale = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')

test('progress panel renders lifecycle and control affordances', () => {
  assert.match(component, /AiJobProgressPanel/)
  assert.match(component, /cancel/)
  assert.match(component, /retry/)
  assert.match(component, /el-progress/)
})

test('task center embeds durable AI job progress', () => {
  assert.match(taskCenter, /AiJobProgressPanel/)
  assert.match(taskCenter, /operation_report/)
  assert.match(taskCenter, /product_compare/)
})

test('Chinese locale contains live job state labels', () => {
  assert.match(zhLocale, /reconnecting/)
  assert.match(zhLocale, /cancellationRequested/)
  assert.match(zhLocale, /resultReady/)
})
