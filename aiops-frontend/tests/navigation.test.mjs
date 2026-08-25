import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const layout = await readFile(new URL('../src/layouts/MainLayout.vue', import.meta.url), 'utf8')
const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
const dataImportView = await readFile(new URL('../src/views/DataImportView.vue', import.meta.url), 'utf8')
const syncCenterView = await readFile(new URL('../src/views/SyncCenterView.vue', import.meta.url), 'utf8')
const taskCenterView = await readFile(new URL('../src/views/TaskCenterView.vue', import.meta.url), 'utf8')
const reportsView = await readFile(new URL('../src/views/ReportsView.vue', import.meta.url), 'utf8')
const apiModules = await readFile(new URL('../src/api/modules.ts', import.meta.url), 'utf8')
const httpClient = await readFile(new URL('../src/api/http.ts', import.meta.url), 'utf8')

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
  assert.match(layout, /path:\s*'\/sync'/)
  assert.match(layout, /path:\s*'\/tasks'/)
  assert.match(layout, /path:\s*'\/reports'/)
  assert.match(router, /SyncCenterView\.vue/)
  assert.match(router, /TaskCenterView\.vue/)
  assert.match(router, /ReportsView\.vue/)
  assert.match(syncCenterView, /syncApi/)
  assert.match(taskCenterView, /taskCenterApi/)
  assert.match(reportsView, /reportApi/)
})

test('task center and report pages expose csv export downloads', () => {
  assert.match(apiModules, /downloadFile/)
  assert.match(apiModules, /taskCenterApi[\s\S]*exportCsv/)
  assert.match(apiModules, /reportApi[\s\S]*exportCsv/)
  assert.match(httpClient, /responseType:\s*'blob'/)
  assert.match(taskCenterView, /taskCenterApi\.exportCsv/)
  assert.match(reportsView, /reportApi\.exportCsv/)
  assert.match(taskCenterView, /tasks\.exportCsv/)
  assert.match(reportsView, /reports\.exportCsv/)
})

test('csv files are selected first and uploaded only when import starts', () => {
  assert.match(dataImportView, /:auto-upload="false"/)
  assert.doesNotMatch(dataImportView, /:http-request="uploadCsv"/)
  assert.match(dataImportView, /selectedFile/)
  assert.match(dataImportView, /uploadSelectedCsv/)
  assert.match(dataImportView, /await uploadSelectedCsv\(\)/)
})

test('single csv upload previews maps and preflights before OSS upload', () => {
  assert.match(dataImportView, /inspectSingleCsvFile/)
  assert.match(dataImportView, /parseCsvPreview/)
  assert.match(dataImportView, /suggestColumnMapping/)
  assert.match(dataImportView, /dataImportApi\.preflightCsv/)
  assert.match(dataImportView, /compactColumnMapping/)
  assert.match(dataImportView, /csvPreflight/)
  assert.match(dataImportView, /startSampleImport/)
  assert.match(apiModules, /preflightCsv/)
  assert.match(apiModules, /importSample/)
})
