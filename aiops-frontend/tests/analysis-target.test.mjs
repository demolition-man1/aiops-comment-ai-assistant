import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import ts from 'typescript'

async function loadHelper() {
  const source = await readFile(new URL('../src/utils/analysisTarget.ts', import.meta.url), 'utf8')
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  }).outputText
  const module = { exports: {} }
  const fn = new Function('exports', 'module', compiled)
  fn(module.exports, module)
  return module.exports
}

test('analysis target prefers full selected product id over query prefix', async () => {
  const { resolveAnalysisProductId } = await loadHelper()

  const productId = resolveAnalysisProductId({
    queryProductId: 'aca2eb7d00ea1a7b',
    selectedProductId: 'aca2eb7d00ea1a7b8ebd4e68314663af',
    firstVisibleProductId: 'aca2eb7d00ea1a7b8ebd4e68314663af'
  })

  assert.equal(productId, 'aca2eb7d00ea1a7b8ebd4e68314663af')
})

test('analysis target falls back to first visible full product id when query is a prefix', async () => {
  const { resolveAnalysisProductId } = await loadHelper()

  const productId = resolveAnalysisProductId({
    queryProductId: ' aca2eb7d00ea1a7b ',
    firstVisibleProductId: 'aca2eb7d00ea1a7b8ebd4e68314663af'
  })

  assert.equal(productId, 'aca2eb7d00ea1a7b8ebd4e68314663af')
})

test('analysis target uses trimmed query when no row product id is available', async () => {
  const { resolveAnalysisProductId } = await loadHelper()

  const productId = resolveAnalysisProductId({
    queryProductId: ' product-a '
  })

  assert.equal(productId, 'product-a')
})
