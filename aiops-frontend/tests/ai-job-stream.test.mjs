import { readFile } from 'node:fs/promises'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const streamClient = await readFile(new URL('../src/api/aiJobStream.ts', import.meta.url), 'utf8')
const composable = await readFile(new URL('../src/composables/useAiJob.ts', import.meta.url), 'utf8')
const apiModules = await readFile(new URL('../src/api/modules.ts', import.meta.url), 'utf8')

test('AI job SSE uses the existing bearer token and reconnect cursor', () => {
  assert.match(streamClient, /fetchEventSource/)
  assert.match(streamClient, /Authorization/)
  assert.match(streamClient, /aiops_token/)
  assert.match(streamClient, /Last-Event-ID/)
  assert.match(streamClient, /signal/)
})

test('AI job composable refreshes authoritative state and exposes controls', () => {
  assert.match(composable, /aiJobApi\.job/)
  assert.match(composable, /aiJobApi\.cancel/)
  assert.match(composable, /aiJobApi\.retry/)
  assert.match(composable, /subscribeAiJob/)
  assert.match(composable, /AbortController/)
  assert.match(composable, /setTimeout/)
  assert.match(composable, /lastEventId\.value/)
})

test('AI job API exposes cancel and retry operations', () => {
  assert.match(apiModules, /cancel:\s*\(jobId: number\)/)
  assert.match(apiModules, /retry:\s*\(jobId: number\)/)
})
