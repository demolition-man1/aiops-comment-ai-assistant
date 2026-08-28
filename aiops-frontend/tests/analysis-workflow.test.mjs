import { readFile } from 'node:fs/promises'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import ts from 'typescript'

async function loadWorkflow() {
  const source = await readFile(new URL('../src/utils/analysisWorkflow.ts', import.meta.url), 'utf8')
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

test('complete workflow runs analysis steps before generating the report', async () => {
  const { runAnalysisWorkflow } = await loadWorkflow()
  const calls = []
  const taskUpdates = []
  const loadedAnalyses = []
  const createdTask = { taskId: 11, taskStatus: 'pending' }
  const completedTask = { taskId: 11, taskStatus: 'success', progress: 100 }
  const analysis = { totalCount: 18, negativeRate: 0.2 }
  const report = { reportId: 31, operationSuggestions: 'Improve packaging' }

  const result = await runAnalysisWorkflow({
    productId: 'product-a',
    includeReport: true,
    dependencies: {
      createTask: async (productId) => {
        calls.push(`create:${productId}`)
        return createdTask
      },
      waitForTask: async (taskId, onTaskUpdate) => {
        calls.push(`wait:${taskId}`)
        onTaskUpdate(completedTask)
        return completedTask
      },
      loadAnalysis: async (productId) => {
        calls.push(`analysis:${productId}`)
        return analysis
      },
      generateReport: async (productId) => {
        calls.push(`report:${productId}`)
        return report
      }
    },
    callbacks: {
      onTaskUpdate: (task) => taskUpdates.push(task.taskStatus),
      onAnalysisLoaded: (value) => loadedAnalyses.push(value)
    }
  })

  assert.deepEqual(calls, ['create:product-a', 'wait:11', 'analysis:product-a', 'report:product-a'])
  assert.deepEqual(taskUpdates, ['pending', 'success'])
  assert.deepEqual(loadedAnalyses, [analysis])
  assert.deepEqual(result, { task: completedTask, analysis, report })
})

test('analysis-only workflow does not call the report provider', async () => {
  const { runAnalysisWorkflow } = await loadWorkflow()
  let reportCalls = 0
  const completedTask = { taskId: 12, taskStatus: 'success', progress: 100 }
  const analysis = { totalCount: 9, negativeRate: 0.1 }

  const result = await runAnalysisWorkflow({
    productId: 'product-b',
    includeReport: false,
    dependencies: {
      createTask: async () => ({ taskId: 12, taskStatus: 'pending' }),
      waitForTask: async () => completedTask,
      loadAnalysis: async () => analysis,
      generateReport: async () => {
        reportCalls += 1
        return { reportId: 32 }
      }
    }
  })

  assert.equal(reportCalls, 0)
  assert.deepEqual(result, { task: completedTask, analysis })
})

test('analysis failure stops the workflow before report generation', async () => {
  const { AnalysisWorkflowError, runAnalysisWorkflow } = await loadWorkflow()
  let reportCalls = 0

  await assert.rejects(
    runAnalysisWorkflow({
      productId: 'product-c',
      includeReport: true,
      dependencies: {
        createTask: async () => ({ taskId: 13, taskStatus: 'pending' }),
        waitForTask: async () => {
          throw new Error('analysis task failed')
        },
        loadAnalysis: async () => ({ totalCount: 0 }),
        generateReport: async () => {
          reportCalls += 1
          return { reportId: 33 }
        }
      }
    }),
    (error) => error instanceof AnalysisWorkflowError
      && error.stage === 'wait-task'
      && error.cause.message === 'analysis task failed'
  )

  assert.equal(reportCalls, 0)
})

test('report failure preserves the analysis callback result', async () => {
  const { AnalysisWorkflowError, runAnalysisWorkflow } = await loadWorkflow()
  const loadedAnalyses = []
  const analysis = { totalCount: 7, negativeRate: 0.4 }

  await assert.rejects(
    runAnalysisWorkflow({
      productId: 'product-d',
      includeReport: true,
      dependencies: {
        createTask: async () => ({ taskId: 14, taskStatus: 'pending' }),
        waitForTask: async () => ({ taskId: 14, taskStatus: 'success', progress: 100 }),
        loadAnalysis: async () => analysis,
        generateReport: async () => {
          throw new Error('provider unavailable')
        }
      },
      callbacks: {
        onAnalysisLoaded: (value) => loadedAnalyses.push(value)
      }
    }),
    (error) => error instanceof AnalysisWorkflowError
      && error.stage === 'generate-report'
      && error.cause.message === 'provider unavailable'
  )

  assert.deepEqual(loadedAnalyses, [analysis])
})
