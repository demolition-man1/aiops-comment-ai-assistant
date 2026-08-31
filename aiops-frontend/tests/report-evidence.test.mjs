import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'

const reportsView = await readFile(new URL('../src/views/ReportsView.vue', import.meta.url), 'utf8')
const commentsView = await readFile(new URL('../src/views/CommentWorkbenchView.vue', import.meta.url), 'utf8')
const types = await readFile(new URL('../src/api/types.ts', import.meta.url), 'utf8')
const zh = await readFile(new URL('../src/i18n/locales/zh-CN.ts', import.meta.url), 'utf8')
const en = await readFile(new URL('../src/i18n/locales/en-US.ts', import.meta.url), 'utf8')
const pt = await readFile(new URL('../src/i18n/locales/pt-BR.ts', import.meta.url), 'utf8')

test('report and archive contracts expose optional evidence references', () => {
  assert.match(types, /export interface ReportEvidence/)
  assert.match(types, /evidence\?: ReportEvidence\[\]/)
})

test('new and archived reports render source references through localized navigation', () => {
  assert.match(reportsView, /reports\.evidenceTitle/)
  assert.match(reportsView, /openEvidenceReference/)
  assert.match(reportsView, /review_evidence/)
  assert.match(commentsView, /report\?\.evidence\?\.length/)
  assert.match(commentsView, /openReportEvidence/)
  assert.match(commentsView, /route\.query\.commentId/)
  for (const locale of [zh, en, pt]) {
    assert.ok(locale.includes('evidenceTitle:'))
    assert.ok(locale.includes('evidenceTypes:'))
  }
})
