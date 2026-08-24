export interface ApiResult<T> {
  code: number
  msg?: string
  data?: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginUser {
  token: string
  userId: number
  username: string
  nickname?: string
  role?: string
}

export interface Overview {
  productCount: number
  sellerCount: number
  commentCount: number
  avgScore: number
  negativeRate: number
}

export interface DistributionItem {
  name: string
  count: number
  rate?: number
}

export interface KeywordItem {
  keyword: string
  count: number
}

export interface TrendItem {
  timeBucket: string
  commentCount: number
  negativeCount: number
  negativeRate: number
  avgScore: number
}

export interface DashboardData {
  scoreDistribution: DistributionItem[]
  sentimentDistribution: DistributionItem[]
  categoryDistribution: DistributionItem[]
  keywordRank: KeywordItem[]
  negativeKeywordRank: KeywordItem[]
  problemDistribution: DistributionItem[]
  customTagDistribution: DistributionItem[]
  trendDistribution: TrendItem[]
}

export interface Product {
  productId: string
  categoryName?: string
  categoryNameEn?: string
  reviewCount?: number
  avgScore?: number
  negativeRate?: number
}

export interface Comment {
  id: number
  reviewId?: string
  productId?: string
  sellerId?: string
  reviewScore?: number
  reviewTitle?: string
  reviewContent?: string
  cleanContent?: string
  sentiment?: string
  systemProblemType?: string
  manualProblemType?: string
  effectiveProblemType?: string
  customTags?: string[]
  isNegative?: number
  reviewTime?: string
}

export interface Task {
  taskId: number
  taskStatus: 'pending' | 'processing' | 'success' | 'failed' | string
  importType?: string
  taskType?: string
  progress?: number
  targetType?: string
  targetId?: string
  successCount?: number
  failCount?: number
  errorMessage?: string
}

export interface AnalysisResult {
  targetType: string
  targetId: string
  totalCount: number
  positiveCount: number
  neutralCount: number
  negativeCount: number
  positiveRate: number
  negativeRate: number
  topKeywords: KeywordItem[]
  negativeKeywords: KeywordItem[]
  scoreDistribution: DistributionItem[]
  problemDistribution: DistributionItem[]
  customTagDistribution: DistributionItem[]
  trendDistribution: TrendItem[]
  summary?: string
  createTime?: string
}

export interface FileUploadResult {
  fileId: number
  originalName: string
  objectKey: string
  fileUrl: string
  fileSize: number
}

export interface OperationReport {
  reportId: number
  reportTitle: string
  consumerPainPoints: string
  productAdvantages: string
  productDisadvantages: string
  operationSuggestions: string
  copywritingSuggestions: string
  serviceSuggestions: string
  fullReport: string
  modelName: string
}

export interface AiContent {
  recordId: number
  generatedContent: string
  modelName: string
}

export interface NegativeReply {
  replyId: number
  commentId?: number
  productId?: string
  sellerId?: string
  problemType?: string
  commentContent?: string
  toneType?: string
  replyContent: string
  modelName?: string
  effectTag?: string
  useCount?: number
  favoriteFlag?: number
  createTime?: string
  updateTime?: string
}

export interface CommentTranslation {
  commentId: number
  productId?: string
  originalContent?: string
  sourceLanguage?: string
  targetLanguage: string
  translatedContent: string
  modelName?: string
  cached?: boolean
}

export interface ProductCompareReport {
  reportId: number
  leftProductId: string
  rightProductId: string
  metricSnapshot?: string
  compareSummary?: string
  advantageAnalysis?: string
  riskAnalysis?: string
  operationSuggestions?: string
  modelName?: string
  createTime?: string
}
