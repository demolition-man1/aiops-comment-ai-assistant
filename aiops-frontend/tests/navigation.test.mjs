import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const layout = await readFile(new URL('../src/layouts/MainLayout.vue', import.meta.url), 'utf8')
const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')

test('sidebar actionable entries use router routes instead of placeholders', () => {
  assert.ok(!layout.includes('href="javascript:void(0)"'))
  assert.ok(layout.includes("path: '/alerts'"))
  assert.ok(layout.includes("path: '/settings'"))
})

test('alert center and settings routes are registered', () => {
  assert.match(router, /path:\s*'alerts'/)
  assert.match(router, /path:\s*'settings'/)
  assert.match(router, /AlertCenterView\.vue/)
  assert.match(router, /SettingsView\.vue/)
})
