import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import ts from 'typescript'

async function loadHelper() {
  const source = await readFile(new URL('../src/utils/metricFormat.ts', import.meta.url), 'utf8')
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

test('metric formatter converts ratio rates to percentages for display', async () => {
  const { formatPercent, toPercentValue } = await loadHelper()

  assert.equal(toPercentValue(0.1543), 15.43)
  assert.equal(formatPercent(0.1543), '15.4%')
})

test('metric formatter keeps values that are already percentages', async () => {
  const { formatPercent, toPercentValue } = await loadHelper()

  assert.equal(toPercentValue(16.27), 16.27)
  assert.equal(formatPercent(16.27), '16.3%')
})

test('metric formatter clamps progress percentages to valid range', async () => {
  const { toProgressPercent } = await loadHelper()

  assert.equal(toProgressPercent(0.1212), 12.12)
  assert.equal(toProgressPercent(120), 100)
})
