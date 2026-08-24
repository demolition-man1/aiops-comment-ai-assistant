import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'

const i18nIndex = await readFile(new URL('../src/i18n/index.ts', import.meta.url), 'utf8')
const localeStore = await readFile(new URL('../src/stores/locale.ts', import.meta.url), 'utf8')
const zh = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')
const en = await readFile(new URL('../src/i18n/locales/en-US.ts', import.meta.url), 'utf8')
const pt = await readFile(new URL('../src/i18n/locales/pt-BR.ts', import.meta.url), 'utf8')

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
