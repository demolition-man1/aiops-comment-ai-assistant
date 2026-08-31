import { readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const apiModules = await readFile(new URL('../src/api/modules.ts', import.meta.url), 'utf8')
const solutionView = await readFile(new URL('../src/views/ProblemSolutionView.vue', import.meta.url), 'utf8')
const commentView = await readFile(new URL('../src/views/CommentWorkbenchView.vue', import.meta.url), 'utf8')
const zh = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')
const en = await readFile(new URL('../src/i18n/locales/en-US.ts', import.meta.url), 'utf8')
const pt = await readFile(new URL('../src/i18n/locales/pt-BR.ts', import.meta.url), 'utf8')

test('rag controls use public Java APIs only', () => {
  assert.match(apiModules, /export const ragKnowledgeApi/)
  assert.match(apiModules, /'\/ai\/rag\/status'/)
  assert.match(apiModules, /'\/ai\/rag\/reindex'/)
  assert.doesNotMatch(apiModules, /\/internal\/ai\/rag/)
})

test('solution library provides explicit rebuild with bounded polling', () => {
  assert.match(solutionView, /ragKnowledgeApi\.status/)
  assert.match(solutionView, /ragKnowledgeApi\.reindex/)
  assert.match(solutionView, /startRagPolling/)
  assert.match(solutionView, /stopRagPolling/)
  assert.match(solutionView, /ragStatus\.value\?\.state === 'building'/)
  assert.match(solutionView, /:disabled="ragReindexing \|\| ragStatus\?\.state === 'building'"/)
})

test('comment workspace displays durable reply references without private historical text', () => {
  assert.match(commentView, /ragReferences/)
  assert.match(commentView, /isProblemSolutionReference/)
  assert.match(commentView, /name: 'solutions'/)
  assert.match(commentView, /replyHistory/)
  assert.doesNotMatch(commentView, /reference\.commentContent/)
})

test('rag labels are localized in all supported languages', () => {
  for (const locale of [zh, en, pt]) {
    assert.match(locale, /ragStatus:/)
    assert.match(locale, /ragReferences:/)
    assert.match(locale, /rebuildRagIndex:/)
  }
})
