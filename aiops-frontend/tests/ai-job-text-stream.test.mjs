import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('AI job stream keeps text deltas in a temporary deduplicated buffer', () => {
  const source = read('composables/useAiJob.ts')
  assert.match(source, /previewText/)
  assert.match(source, /event\.eventType === 'text_delta'/)
  assert.match(source, /event\.deltaId <= \(lastDeltaId\.value \|\| 0\)/)
  assert.match(source, /previewText\.value = ''/)
})

test('only the job panel renders an in-memory preview state', () => {
  const panel = read('components/AiJobProgressPanel.vue')
  const types = read('api/types.ts')
  assert.match(panel, /jobs\.preview/)
  assert.match(types, /'text_delta'/)
  assert.match(types, /textDelta\?: string/)
  assert.match(panel, /status === 'success'/)
  assert.match(panel, /emit\('result', resolvedJob\)/)
})
