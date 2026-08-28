export type AnalysisWorkflowStage =
  | 'create-task'
  | 'wait-task'
  | 'load-analysis'
  | 'generate-report'

export class AnalysisWorkflowError extends Error {
  constructor(
    public readonly stage: AnalysisWorkflowStage,
    public readonly cause: unknown
  ) {
    super(cause instanceof Error ? cause.message : String(cause))
    this.name = 'AnalysisWorkflowError'
  }
}

export interface AnalysisWorkflowDependencies<TTask extends { taskId: number }, TAnalysis, TReport> {
  createTask: (productId: string) => Promise<TTask>
  waitForTask: (taskId: number, onTaskUpdate: (task: TTask) => void) => Promise<TTask>
  loadAnalysis: (productId: string) => Promise<TAnalysis>
  generateReport: (productId: string) => Promise<TReport>
}

export interface AnalysisWorkflowCallbacks<TTask, TAnalysis> {
  onTaskUpdate?: (task: TTask) => void
  onAnalysisLoaded?: (analysis: TAnalysis) => void
}

export interface AnalysisWorkflowOptions<TTask extends { taskId: number }, TAnalysis, TReport> {
  productId: string
  includeReport: boolean
  dependencies: AnalysisWorkflowDependencies<TTask, TAnalysis, TReport>
  callbacks?: AnalysisWorkflowCallbacks<TTask, TAnalysis>
}

export interface AnalysisWorkflowResult<TTask, TAnalysis, TReport> {
  task: TTask
  analysis: TAnalysis
  report?: TReport
}

async function executeStage<T>(stage: AnalysisWorkflowStage, operation: () => Promise<T>): Promise<T> {
  try {
    return await operation()
  } catch (error) {
    throw new AnalysisWorkflowError(stage, error)
  }
}

export async function runAnalysisWorkflow<TTask extends { taskId: number }, TAnalysis, TReport>(
  options: AnalysisWorkflowOptions<TTask, TAnalysis, TReport>
): Promise<AnalysisWorkflowResult<TTask, TAnalysis, TReport>> {
  const { callbacks, dependencies, includeReport, productId } = options
  const createdTask = await executeStage('create-task', () => dependencies.createTask(productId))
  callbacks?.onTaskUpdate?.(createdTask)

  const completedTask = await executeStage('wait-task', () =>
    dependencies.waitForTask(createdTask.taskId, (task) => callbacks?.onTaskUpdate?.(task))
  )
  const analysis = await executeStage('load-analysis', () => dependencies.loadAnalysis(productId))
  callbacks?.onAnalysisLoaded?.(analysis)

  if (!includeReport) {
    return { task: completedTask, analysis }
  }

  const report = await executeStage('generate-report', () => dependencies.generateReport(productId))
  return { task: completedTask, analysis, report }
}
