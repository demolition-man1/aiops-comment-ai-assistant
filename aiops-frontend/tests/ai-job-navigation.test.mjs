import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const modules = await readFile(new URL('../src/api/modules.ts', import.meta.url), 'utf8')
const types = await readFile(new URL('../src/api/types.ts', import.meta.url), 'utf8')
const compareView = await readFile(new URL('../src/views/ProductCompareView.vue', import.meta.url), 'utf8')
const workbenchView = await readFile(new URL('../src/views/CommentWorkbenchView.vue', import.meta.url), 'utf8')
const contentView = await readFile(new URL('../src/views/AiContentView.vue', import.meta.url), 'utf8')
const taskCenterView = await readFile(new URL('../src/views/TaskCenterView.vue', import.meta.url), 'utf8')

test('report and comparison submissions use idempotent AI job APIs', () => {
  assert.match(types, /export interface AiJobCreated/)
  assert.match(types, /export interface AiJob/)
  assert.match(modules, /export const aiJobApi/)
  assert.match(modules, /Idempotency-Key/)
  assert.match(modules, /\/ai\/jobs\/reports/)
  assert.match(modules, /\/ai\/jobs\/product-comparisons/)
  assert.match(compareView, /aiJobApi\.createProductCompare/)
  assert.match(workbenchView, /aiJobApi\.createReport/)
  assert.match(compareView, /AiJobProgressPanel/)
  assert.match(workbenchView, /AiJobProgressPanel/)
  assert.match(compareView, /currentJob\.value = await aiJobApi\.job/)
  assert.match(workbenchView, /reportJob\.value = await aiJobApi\.job/)
})

test('negative replies and content generation also use durable job APIs', () => {
  assert.match(modules, /\/ai\/jobs\/negative-replies/)
  assert.match(modules, /\/ai\/jobs\/content/)
  assert.match(workbenchView, /aiJobApi\.createNegativeReply/)
  assert.match(contentView, /aiJobApi\.createContent/)
  assert.match(workbenchView, /AiJobProgressPanel/)
  assert.match(contentView, /AiJobProgressPanel/)
})

test('task center recognizes durable AI task states', () => {
  assert.match(taskCenterView, /operation_report/)
  assert.match(taskCenterView, /product_compare/)
  assert.match(taskCenterView, /timed_out/)
  assert.match(taskCenterView, /cancelled/)
})
