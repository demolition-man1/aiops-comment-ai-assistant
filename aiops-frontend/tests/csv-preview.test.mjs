import { test } from 'node:test'
import assert from 'node:assert/strict'

import {
  estimateCsvRows,
  getMissingMappedFields,
  parseCsvPreview,
  suggestColumnMapping
} from '../src/utils/csvPreview.ts'

test('parseCsvPreview reads headers and first rows without requiring fixed Olist column names', () => {
  const csv = [
    '商品ID,评分,评论内容',
    'sku-1,5,"great value, fast delivery"',
    'sku-2,1,bad packaging'
  ].join('\n')

  const preview = parseCsvPreview(csv, 20)

  assert.deepEqual(preview.columns, ['商品ID', '评分', '评论内容'])
  assert.equal(preview.rows.length, 2)
  assert.equal(preview.rows[0]['评论内容'], 'great value, fast delivery')
  assert.equal(preview.estimatedRows, 2)
})

test('suggestColumnMapping maps common merchant export columns to normalized import fields', () => {
  const mapping = suggestColumnMapping(['商品ID', '评分', '评论内容'])

  assert.equal(mapping.product_id, '商品ID')
  assert.equal(mapping.review_score, '评分')
  assert.equal(mapping.review_content, '评论内容')
  assert.deepEqual(getMissingMappedFields(mapping), [])
})

test('estimateCsvRows ignores empty trailing lines', () => {
  assert.equal(estimateCsvRows('product_id,review_score\np1,5\n\n'), 1)
})
