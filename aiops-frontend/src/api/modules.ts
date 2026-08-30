import { downloadFile, http, uploadFile } from './http'
import type {
  AiContent,
  CategoryAnalysis,
  AnalysisResult,
  Comment,
  CommentTranslation,
  CustomTag,
  CsvImportPreflight,
  DashboardData,
  FileUploadResult,
  LoginRequest,
  LoginUser,
  NegativeReply,
  OperationReport,
  Overview,
  PageResult,
  Product,
  ProductCompareReport,
  ProductRank,
  PromptTemplate,
  ProblemSolution,
  ReportArchive,
  ReportOverview,
  SyncConfig,
  SyncExecution,
  TaskRecord,
  Task,
  AiCallLog,
  AiCallLogOverview,
  CommentAiEvaluation,
  CommentAiShadowResult,
  CommentAiShadowRun,
  CommentAiShadowTask
} from './types'

export const authApi = {
  login: (payload: LoginRequest) => http.post<LoginUser, LoginUser>('/auth/login', payload),
  profile: () => http.get<LoginUser, LoginUser>('/user/profile')
}

export const dashboardApi = {
  overview: () => http.get<Overview, Overview>('/dashboard/overview'),
  product: (productId: string) => http.get<DashboardData, DashboardData>(`/dashboard/product/${productId}`),
  seller: (sellerId: string) => http.get<DashboardData, DashboardData>(`/dashboard/seller/${sellerId}`)
}

export const productApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<Product>, PageResult<Product>>('/products', { params }),
  detail: (productId: string) => http.get<Product, Product>(`/products/${productId}`)
}

export const commentApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<Comment>, PageResult<Comment>>('/comments', { params }),
  negative: (params: Record<string, unknown>) =>
    http.get<PageResult<Comment>, PageResult<Comment>>('/comments/negative', { params }),
  updateTags: (commentId: number, payload: { manualProblemType?: string; customTags: string[] }) =>
    http.put<Comment, Comment>(`/comments/${commentId}/tags`, payload),
  translate: (commentId: number, payload: { language?: string; forceRefresh?: boolean }) =>
    http.post<CommentTranslation, CommentTranslation>(`/comments/${commentId}/translate`, payload)
}

export const tagApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<CustomTag>, PageResult<CustomTag>>('/tags', { params }),
  active: () => http.get<CustomTag[], CustomTag[]>('/tags/active'),
  create: (payload: Partial<CustomTag>) => http.post<CustomTag, CustomTag>('/tags', payload),
  update: (tagId: number, payload: Partial<CustomTag>) => http.put<CustomTag, CustomTag>(`/tags/${tagId}`, payload),
  updateStatus: (tagId: number, enabled: number) =>
    http.put<CustomTag, CustomTag>(`/tags/${tagId}/status`, undefined, { params: { enabled } })
}

export const problemSolutionApi = {
  page: (params: Record<string, unknown>) =>
    http.get<PageResult<ProblemSolution>, PageResult<ProblemSolution>>('/problem-solutions', { params }),
  recommend: (params: Record<string, unknown>) =>
    http.get<ProblemSolution[], ProblemSolution[]>('/problem-solutions/recommend', { params }),
  create: (payload: Partial<ProblemSolution>) =>
    http.post<ProblemSolution, ProblemSolution>('/problem-solutions', payload),
  update: (solutionId: number, payload: Partial<ProblemSolution>) =>
    http.put<ProblemSolution, ProblemSolution>(`/problem-solutions/${solutionId}`, payload),
  updateStatus: (solutionId: number, enabled: number) =>
    http.put<ProblemSolution, ProblemSolution>(`/problem-solutions/${solutionId}/status`, undefined, { params: { enabled } })
}

export const promptTemplateApi = {
  page: (params: Record<string, unknown>) =>
    http.get<PageResult<PromptTemplate>, PageResult<PromptTemplate>>('/prompt-templates', { params }),
  active: (params: Record<string, unknown>) =>
    http.get<PromptTemplate[], PromptTemplate[]>('/prompt-templates/active', { params }),
  create: (payload: Partial<PromptTemplate>) =>
    http.post<PromptTemplate, PromptTemplate>('/prompt-templates', payload),
  update: (templateId: number, payload: Partial<PromptTemplate>) =>
    http.put<PromptTemplate, PromptTemplate>(`/prompt-templates/${templateId}`, payload),
  updateStatus: (templateId: number, enabled: number) =>
    http.put<PromptTemplate, PromptTemplate>(`/prompt-templates/${templateId}/status`, undefined, { params: { enabled } }),
  setDefault: (templateId: number) =>
    http.post<PromptTemplate, PromptTemplate>(`/prompt-templates/${templateId}/default`)
}

export const aiCallLogApi = {
  overview: (params: Record<string, unknown>) =>
    http.get<AiCallLogOverview, AiCallLogOverview>('/ai/call-logs/overview', { params }),
  page: (params: Record<string, unknown>) =>
    http.get<PageResult<AiCallLog>, PageResult<AiCallLog>>('/ai/call-logs', { params })
}

export const analysisApi = {
  createTask: (payload: { targetType: string; targetId: string; analysisType?: string }) =>
    http.post<Task, Task>('/analysis/tasks', payload),
  task: (taskId: number) => http.get<Task, Task>(`/analysis/tasks/${taskId}`),
  product: (productId: string) => http.get<AnalysisResult, AnalysisResult>(`/analysis/product/${productId}`),
  seller: (sellerId: string) => http.get<AnalysisResult, AnalysisResult>(`/analysis/seller/${sellerId}`),
  compare: (payload: { leftProductId: string; rightProductId: string; language?: string; forceRefresh?: boolean }) =>
    http.post<ProductCompareReport, ProductCompareReport>('/analysis/products/compare', payload),
  comparePage: (params: Record<string, unknown>) =>
    http.get<PageResult<ProductCompareReport>, PageResult<ProductCompareReport>>('/analysis/products/compare', { params })
}

export const fileApi = {
  uploadCsv: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'csv_import')
    return uploadFile<FileUploadResult>('/files/upload', formData)
  }
}

export const dataImportApi = {
  preflightCsv: (payload: {
    fileName?: string
    fileSize?: number
    fileHash?: string
    dataPath?: string
    dataSource?: string
    importMode?: string
    estimatedRows?: number
    columnMapping?: Record<string, string | undefined>
  }) => http.post<CsvImportPreflight, CsvImportPreflight>('/data/import/csv/preflight', payload),
  importCsv: (payload: {
    fileId?: number
    objectKey?: string
    fileUrl?: string
    dataPath?: string
    dataSource?: string
    importMode?: string
    fileHash?: string
    columnMapping?: Record<string, string | undefined>
    allowDuplicate?: boolean
  }) => http.post<Task, Task>('/data/import/csv', payload),
  importSample: () => http.post<Task, Task>('/data/import/sample'),
  importCrawler: (payload: Record<string, unknown>) => http.post<Task, Task>('/data/import/crawler', payload),
  task: (taskId: number, importType?: string) =>
    http.get<Task, Task>(`/data/import/tasks/${taskId}`, {
      params: importType ? { importType } : undefined
    })
}

export const syncApi = {
  configs: (params: Record<string, unknown>) =>
    http.get<PageResult<SyncConfig>, PageResult<SyncConfig>>('/sync/configs', { params }),
  createConfig: (payload: Partial<SyncConfig>) => http.post<SyncConfig, SyncConfig>('/sync/configs', payload),
  updateConfig: (configId: number, payload: Partial<SyncConfig>) =>
    http.put<SyncConfig, SyncConfig>(`/sync/configs/${configId}`, payload),
  enableConfig: (configId: number) => http.post<SyncConfig, SyncConfig>(`/sync/configs/${configId}/enable`),
  disableConfig: (configId: number) => http.post<SyncConfig, SyncConfig>(`/sync/configs/${configId}/disable`),
  trigger: (configId: number) => http.post<SyncExecution, SyncExecution>(`/sync/configs/${configId}/trigger`),
  executions: (params: Record<string, unknown>) =>
    http.get<PageResult<SyncExecution>, PageResult<SyncExecution>>('/sync/executions', { params })
}

export const taskCenterApi = {
  page: (params: Record<string, unknown>) => http.get<PageResult<TaskRecord>, PageResult<TaskRecord>>('/tasks', { params }),
  detail: (recordKey: string) => http.get<TaskRecord, TaskRecord>(`/tasks/${encodeURIComponent(recordKey)}`),
  retry: (recordKey: string) => http.post<Task, Task>(`/tasks/${encodeURIComponent(recordKey)}/retry`),
  exportCsv: (params: Record<string, unknown>) => downloadFile('/tasks/export', params)
}

export const reportApi = {
  overview: () => http.get<ReportOverview, ReportOverview>('/reports/overview'),
  trends: () => http.get<ReportOverview['trendDistribution'], ReportOverview['trendDistribution']>('/reports/trends'),
  distributions: () => http.get<DashboardData, DashboardData>('/reports/distributions'),
  productRank: (params: Record<string, unknown>) =>
    http.get<ProductRank, ProductRank>('/reports/product-rank', { params }),
  categories: (params: Record<string, unknown>) =>
    http.get<CategoryAnalysis[], CategoryAnalysis[]>('/reports/categories', { params }),
  exportCsv: () => downloadFile('/reports/export')
}

export const commentAiShadowApi = {
  createTask: (payload: {
    targetType: 'product' | 'seller'
    targetId: string
    sampleSize: number
    sampleSeed: number
    maxTotalTokens: number
    language: string
  }) => http.post<CommentAiShadowTask, CommentAiShadowTask>('/analysis/ai-shadow/tasks', payload),
  task: (taskId: number) => http.get<CommentAiShadowTask, CommentAiShadowTask>(`/analysis/ai-shadow/tasks/${taskId}`),
  runs: (params: Record<string, unknown>) =>
    http.get<PageResult<CommentAiShadowRun>, PageResult<CommentAiShadowRun>>('/analysis/ai-shadow/runs', { params }),
  results: (runId: number, params: Record<string, unknown>) =>
    http.get<PageResult<CommentAiShadowResult>, PageResult<CommentAiShadowResult>>(`/analysis/ai-shadow/runs/${runId}/results`, { params }),
  upsertAnnotation: (commentId: number, payload: {
    manualSentiment: 'positive' | 'neutral' | 'negative'
    manualProblemTypes: string[]
    annotationNote?: string
  }) => http.put<void, void>(`/analysis/ai-shadow/comments/${commentId}/annotation`, payload),
  evaluation: (runId: number) =>
    http.get<CommentAiEvaluation, CommentAiEvaluation>(`/analysis/ai-shadow/runs/${runId}/evaluation`)
}

export const reportArchiveApi = {
  page: (params: Record<string, unknown>) =>
    http.get<PageResult<ReportArchive>, PageResult<ReportArchive>>('/report-archives', { params }),
  detail: (archiveId: number) =>
    http.get<ReportArchive, ReportArchive>(`/report-archives/${archiveId}`),
  archive: (reportId: number, payload?: { remark?: string }) =>
    http.post<ReportArchive, ReportArchive>(`/report-archives/${reportId}`, payload),
  exportPdf: (archiveId: number, language: string) =>
    downloadFile(`/report-archives/${archiveId}/export/pdf`, { language }),
  updateStatus: (archiveId: number, archiveStatus: 'archived' | 'restored') =>
    http.put<ReportArchive, ReportArchive>(`/report-archives/${archiveId}/status`, { archiveStatus })
}

export const aiApi = {
  productReport: (payload: { productId: string; language?: string }) =>
    http.post<OperationReport, OperationReport>('/ai/reports/product', payload),
  sellerReport: (payload: { sellerId: string; language?: string }) =>
    http.post<OperationReport, OperationReport>('/ai/reports/seller', payload),
  reports: (params: Record<string, unknown>) => http.get<PageResult<OperationReport>, PageResult<OperationReport>>('/ai/reports', { params }),
  report: (reportId: number) => http.get<OperationReport, OperationReport>(`/ai/reports/${reportId}`),
  content: (payload: Record<string, unknown>) => http.post<AiContent, AiContent>('/ai/contents', payload),
  contents: (params: Record<string, unknown>) => http.get<PageResult<AiContent>, PageResult<AiContent>>('/ai/contents', { params }),
  negativeReply: (payload: { commentId: number; toneType?: string; language?: string }) =>
    http.post<NegativeReply, NegativeReply>('/ai/negative-replies', payload),
  negativeReplies: (params: Record<string, unknown>) =>
    http.get<PageResult<NegativeReply>, PageResult<NegativeReply>>('/ai/negative-replies', { params }),
  markReplyUsed: (replyId: number) => http.post<NegativeReply, NegativeReply>(`/ai/negative-replies/${replyId}/use`),
  updateReplyEffect: (replyId: number, effectTag?: string) =>
    http.put<NegativeReply, NegativeReply>(`/ai/negative-replies/${replyId}/effect`, { effectTag }),
  updateReplyFavorite: (replyId: number, favoriteFlag: number) =>
    http.put<NegativeReply, NegativeReply>(`/ai/negative-replies/${replyId}/favorite`, { favoriteFlag })
}

export function pollTask(
  loader: () => Promise<Task>,
  onUpdate: (task: Task) => void,
  intervalMs = 3000,
  onError?: (error: unknown) => void
) {
  let timer: number | undefined

  const run = async () => {
    try {
      const task = await loader()
      onUpdate(task)
      if (task.taskStatus === 'processing' || task.taskStatus === 'pending') {
        timer = window.setTimeout(run, intervalMs)
      }
    } catch (error) {
      onError?.(error)
    }
  }

  void run()

  return () => {
    if (timer) {
      window.clearTimeout(timer)
    }
  }
}
