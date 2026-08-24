import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const layout = await readFile(new URL('../src/layouts/MainLayout.vue', import.meta.url), 'utf8')
const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
const dataImportView = await readFile(new URL('../src/views/DataImportView.vue', import.meta.url), 'utf8')

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

test('layout uses i18n keys and exposes language selector', () => {
  assert.match(layout, /useI18n/)
  assert.match(layout, /useLocaleStore/)
  assert.match(layout, /localeStore\.languages/)
  assert.ok(!layout.includes("label: '商家驾驶舱'"))
})

test('topbar action buttons are wired to visible destinations', () => {
  assert.match(layout, /goScheduledSync/)
  assert.match(layout, /goTaskCenter/)
  assert.match(layout, /goDataReports/)
  assert.match(layout, /@click="goScheduledSync"/)
  assert.match(layout, /@click="goTaskCenter"/)
  assert.match(layout, /@click="goDataReports"/)
  assert.match(layout, /#task-center/)
})

test('csv files are selected first and uploaded only when import starts', () => {
  assert.match(dataImportView, /:auto-upload="false"/)
  assert.doesNotMatch(dataImportView, /:http-request="uploadCsv"/)
  assert.match(dataImportView, /selectedFile/)
  assert.match(dataImportView, /uploadSelectedCsv/)
  assert.match(dataImportView, /await uploadSelectedCsv\(\)/)
})

test('single csv upload validates required review columns before OSS upload', () => {
  assert.match(dataImportView, /validateSingleCsvFile/)
  assert.match(dataImportView, /readSingleCsvHeader/)
  assert.match(dataImportView, /missingSingleCsvColumns/)
  assert.match(dataImportView, /singleCsvSchemaError/)
  assert.match(dataImportView, /uploadRef\.value\?\.clearFiles\(\)/)
})
