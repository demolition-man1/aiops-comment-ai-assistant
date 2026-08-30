import { access, readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
const layout = await readFile(new URL('../src/layouts/MainLayout.vue', import.meta.url), 'utf8')
const modules = await readFile(new URL('../src/api/modules.ts', import.meta.url), 'utf8')
const types = await readFile(new URL('../src/api/types.ts', import.meta.url), 'utf8')
const zh = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')
const en = await readFile(new URL('../src/i18n/locales/en-US.ts', import.meta.url), 'utf8')
const pt = await readFile(new URL('../src/i18n/locales/pt-BR.ts', import.meta.url), 'utf8')

test('comment AI evaluation workspace is navigable and backed by public Java APIs', async () => {
  assert.match(router, /path:\s*'ai-evaluation'/)
  assert.match(router, /CommentAiEvaluationView\.vue/)
  assert.match(layout, /layout\.nav\.aiEvaluation/)
  assert.match(modules, /export const commentAiShadowApi/)
  assert.match(modules, /\/analysis\/ai-shadow\/tasks/)
  assert.match(modules, /\/analysis\/ai-shadow\/runs/)
  assert.match(modules, /\/annotation/)
  assert.match(modules, /\/evaluation/)
  assert.match(modules, /\/hybrid-readiness/)
  assert.match(modules, /\/hybrid-activation/)
  assert.match(types, /interface CommentAiShadowRun/)
  assert.match(types, /interface CommentAiEvaluation/)
  assert.match(types, /interface CommentAiHybridReadiness/)
  await access(new URL('../src/views/CommentAiEvaluationView.vue', import.meta.url))
})

test('evaluation workspace keeps task execution explicit and polling bounded to active tasks', async () => {
  const view = await readFile(new URL('../src/views/CommentAiEvaluationView.vue', import.meta.url), 'utf8')
  assert.match(view, /commentAiShadowApi\.createTask/)
  assert.match(view, /@click="startRun"/)
  assert.match(view, /pollActiveTask/)
  assert.match(view, /isTerminalTask/)
  assert.match(view, /commentAiShadowApi\.upsertAnnotation/)
  assert.match(view, /commentAiShadowApi\.evaluation/)
  assert.match(view, /commentAiShadowApi\.hybridReadiness/)
  assert.match(view, /commentAiShadowApi\.activateHybrid/)
  assert.match(view, /activationConfirm/)
  assert.doesNotMatch(view, /internal\/analysis/)
})

test('all supported locales provide evaluation workspace labels', () => {
  for (const locale of [zh, en, pt]) {
    assert.match(locale, /aiEvaluation:\s*{/)
    assert.match(locale, /startRun:/)
    assert.match(locale, /qualityReady:/)
  }
})
