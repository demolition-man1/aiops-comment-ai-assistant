import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'

const i18nIndex = await readFile(new URL('../src/i18n/index.ts', import.meta.url), 'utf8')
const localeStore = await readFile(new URL('../src/stores/locale.ts', import.meta.url), 'utf8')
const zh = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')
const en = await readFile(new URL('../src/i18n/locales/en-US.ts', import.meta.url), 'utf8')
const pt = await readFile(new URL('../src/i18n/locales/pt-BR.ts', import.meta.url), 'utf8')
const dashboardView = await readFile(new URL('../src/views/DashboardView.vue', import.meta.url), 'utf8')
const loginView = await readFile(new URL('../src/views/LoginView.vue', import.meta.url), 'utf8')
const settingsView = await readFile(new URL('../src/views/SettingsView.vue', import.meta.url), 'utf8')
const httpClient = await readFile(new URL('../src/api/http.ts', import.meta.url), 'utf8')
const commentWorkbenchView = await readFile(new URL('../src/views/CommentWorkbenchView.vue', import.meta.url), 'utf8')
const alertCenterView = await readFile(new URL('../src/views/AlertCenterView.vue', import.meta.url), 'utf8')
const dataImportView = await readFile(new URL('../src/views/DataImportView.vue', import.meta.url), 'utf8')
const aiContentView = await readFile(new URL('../src/views/AiContentView.vue', import.meta.url), 'utf8')
const productCompareView = await readFile(new URL('../src/views/ProductCompareView.vue', import.meta.url), 'utf8')

test('i18n supports chinese english and portuguese locales', () => {
  for (const locale of ['zh-CN', 'en-US', 'pt-BR']) {
    assert.ok(i18nIndex.includes(locale))
  }
  assert.match(i18nIndex, /normalizeLocale/)
  assert.match(i18nIndex, /elementPlusLocales/)
})

test('locale store persists selected language', () => {
  assert.match(localeStore, /defineStore\('locale'/)
  assert.match(localeStore, /localStorage\.setItem/)
  assert.match(localeStore, /localeStorageKey/)
  assert.match(i18nIndex, /aiops_locale/)
})

test('locale dictionaries expose the same root sections', () => {
  for (const section of [
    'common',
    'layout',
    'dashboard',
    'comments',
    'importCenter',
    'compare',
    'aiContent',
    'alerts',
    'settings',
    'login'
  ]) {
    assert.ok(zh.includes(`${section}:`), `zh missing ${section}`)
    assert.ok(en.includes(`${section}:`), `en missing ${section}`)
    assert.ok(pt.includes(`${section}:`), `pt missing ${section}`)
  }
})

test('core pages and request fallbacks use i18n', () => {
  for (const source of [dashboardView, loginView, settingsView]) {
    assert.match(source, /useI18n/)
  }
  assert.ok(!dashboardView.includes('今日运营概览'))
  assert.ok(!loginView.includes('商家运营后台'))
  assert.ok(!settingsView.includes('系统设置'))
  assert.match(httpClient, /i18n\.global\.t/)
})

test('enum translations include sentiment and problem type values', () => {
  for (const value of ['positive', 'neutral', 'negative', 'quality', 'logistics', 'price', 'service', 'size', 'other']) {
    assert.ok(zh.includes(value), `zh missing enum ${value}`)
    assert.ok(en.includes(value), `en missing enum ${value}`)
    assert.ok(pt.includes(value), `pt missing enum ${value}`)
  }
})

test('review operations pages render labels through i18n', () => {
  for (const source of [commentWorkbenchView, alertCenterView, dataImportView]) {
    assert.match(source, /useI18n/)
  }
  assert.match(commentWorkbenchView, /displaySentiment/)
  assert.match(commentWorkbenchView, /displayProblemType/)
  assert.match(alertCenterView, /displayProblemType/)
  assert.ok(!commentWorkbenchView.includes('评论智能工作台'))
  assert.ok(!alertCenterView.includes('告警中心'))
  assert.ok(!dataImportView.includes('数据导入中心'))
})

test('ai request pages no longer hard-code zh-CN language', () => {
  for (const source of [aiContentView, commentWorkbenchView, productCompareView]) {
    assert.ok(!source.includes("language: 'zh-CN'"))
    assert.match(source, /localeStore\.locale/)
  }
})

test('settings hides development connection details from production merchants', () => {
  assert.match(settingsView, /showRuntimeStatus/)
  assert.match(settingsView, /import\.meta\.env\.DEV/)
  assert.match(settingsView, /auth\.user\?\.role === 'admin'/)
  assert.match(settingsView, /v-if="showRuntimeStatus"/)
  assert.ok(!settingsView.includes('VITE_BACKEND_URL'))
  assert.ok(!settingsView.includes('backendUrl'))
  assert.ok(!settingsView.includes('localhost:8080'))
  assert.ok(!zh.includes('联调状态'))
  assert.ok(!zh.includes('联调参数'))
  assert.ok(!zh.includes('后端代理地址'))
})
