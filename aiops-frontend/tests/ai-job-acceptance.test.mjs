import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = (path) => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('the browser receives AI job events only through the Java API boundary', () => {
  const stream = read('api/aiJobStream.ts')
  const types = read('api/types.ts')

  assert.match(stream, /\/ai\/jobs\/\$\{jobId\}\/events/)
  assert.doesNotMatch(stream, /8001|python-service/)
  assert.match(types, /textDelta\?: string/)
  assert.match(types, /deltaId\?: number/)
})
