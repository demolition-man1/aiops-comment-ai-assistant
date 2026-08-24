import { http, uploadFile } from './http'
import type {
  AiContent,
  AnalysisResult,
  Comment,
  CommentTranslation,
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
  Task
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
  importCsv: (payload: {
    fileId?: number
    objectKey?: string
    fileUrl?: string
    dataPath?: string
    dataSource?: string
    importMode?: string
  }) => http.post<Task, Task>('/data/import/csv', payload),
  importCrawler: (payload: Record<string, unknown>) => http.post<Task, Task>('/data/import/crawler', payload),
  task: (taskId: number, importType?: string) =>
    http.get<Task, Task>(`/data/import/tasks/${taskId}`, {
      params: importType ? { importType } : undefined
    })
}

export const aiApi = {
  productReport: (payload: { productId: string; language?: string }) =>
    http.post<OperationReport, OperationReport>('/ai/reports/product', payload),
  sellerReport: (payload: { sellerId: string; language?: string }) =>
    http.post<OperationReport, OperationReport>('/ai/reports/seller', payload),
  reports: (params: Record<string, unknown>) => http.get<PageResult<OperationReport>, PageResult<OperationReport>>('/ai/reports', { params }),
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
