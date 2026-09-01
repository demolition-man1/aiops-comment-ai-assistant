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

export interface ProductRank {
  hotProducts: Product[]
  highRiskProducts: Product[]
  topRatedProducts: Product[]
}

export interface CategoryAnalysis {
  categoryName: string
  productCount: number
  commentCount: number
  avgScore: number
  negativeCount: number
  negativeRate: number
  topProblemType?: string
  topProblemCount?: number
  riskLevel: 'none' | 'low' | 'medium' | 'high' | string
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

export interface AiJobCreated {
  jobId: number
  taskStatus: 'pending' | 'processing' | 'success' | 'failed' | 'timed_out' | 'cancelled' | string
  reused: boolean
}

export interface AiJob {
  jobId: number
  jobType: 'operation_report' | 'product_compare' | 'negative_reply' | 'content' | string
  targetType: string
  targetId: string
  taskStatus: 'pending' | 'processing' | 'success' | 'failed' | 'timed_out' | 'cancelled' | string
  jobStage?: 'preparing' | 'retrieving' | 'generating' | 'validating' | 'persisting' | string
  progress?: number
  resultType?: string
  resultId?: number
  attemptCount?: number
  cancelRequested?: boolean
  queueLatencyMs?: number
  providerLatencyMs?: number
  totalLatencyMs?: number
  errorCode?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
  createTime?: string
  updateTime?: string
}

export interface AiJobEvent {
  eventId?: number
  eventType: 'snapshot' | 'stage' | 'completed' | 'failed' | 'timed_out' | 'cancelled' | string
  jobId: number
  jobType: AiJob['jobType']
  taskStatus: AiJob['taskStatus']
  jobStage?: AiJob['jobStage']
  progress?: number
  resultType?: string
  resultId?: number
  occurredAt?: string
}

export interface CommentAiShadowTask {
  taskId: number
  runId?: number
  taskStatus: 'pending' | 'processing' | 'success' | 'partial' | 'budget_stopped' | 'failed' | string
  progress: number
  actualSampleSize: number
  errorMessage?: string
}

export interface CommentAiShadowRun {
  runId: number
  taskId: number
  targetType: 'product' | 'seller' | string
  targetId: string
  sampleSeed: number
  requestedSampleSize: number
  actualSampleSize: number
  maxTotalTokens: number
  totalCalls: number
  successCount: number
  failureCount: number
  totalTokens: number
  latencyMs: number
  runStatus: 'processing' | 'success' | 'partial' | 'budget_stopped' | 'failed' | string
  errorMessage?: string
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface CommentAiShadowResult {
  resultId: number
  runId: number
  commentId: number
  sampleOrder: number
  reviewScore?: number
  reviewContent?: string
  ruleSentiment?: string
  ruleProblemType?: string
  aiSentiment?: string
  aiSentimentConfidence?: number
  aiPrimaryProblem?: string
  aiProblems?: string[]
  aiEvidence?: string
  jsonValid: number
  evidenceValid: number
  callStatus: 'pending' | 'success' | 'failed' | string
  modelName?: string
  tokenUsage: number
  tokenUsageEstimated: number
  latencyMs: number
  errorMessage?: string
  manualSentiment?: string
  manualProblemTypes?: string[]
  annotationNote?: string
  annotationTime?: string
}

export interface CommentAiMetricBlock {
  sentimentAccuracy?: number | null
  problemMicroF1?: number | null
  problemMacroF1?: number | null
}

export interface CommentAiEvaluation {
  qualityReady: boolean
  sampleCount: number
  annotatedCount: number
  attemptedCallCount: number
  successfulCallCount: number
  failedCallCount: number
  annotationCoverage: number
  jsonValidRate: number
  evidenceValidRate: number
  callSuccessRate: number
  totalTokens: number
  estimatedTokenRowCount: number
  averageLatencyMs: number
  budgetStopped: boolean
  rule: CommentAiMetricBlock
  ai: CommentAiMetricBlock
  delta: CommentAiMetricBlock
}

export interface CommentAiHybridReadiness {
  ready: boolean
  failures: string[]
  eligibleDecisionCount: number
  activeDecisionCount: number
  mode: 'rule' | 'hybrid' | string
}

export interface SyncConfig {
  id: number
  syncName: string
  sourceType: string
  dataSource?: string
  importMode?: string
  dataPath?: string
  fileId?: number
  objectKey?: string
  fileUrl?: string
  platform?: string
  targetUrl?: string
  targetType?: string
  maxCount?: number
  delaySeconds?: number
  cronExpression: string
  autoAnalysis?: number
  enabled: number
  remark?: string
  lastRunTime?: string
  nextRunTime?: string
  createTime?: string
  updateTime?: string
}

export interface SyncExecution {
  id: number
  configId: number
  syncName?: string
  triggerType?: string
  executionStatus: string
  linkedTaskId?: number
  linkedTaskType?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface CustomTag {
  id: number
  tagName: string
  tagGroup?: string
  color?: string
  description?: string
  sortOrder?: number
  enabled: number
  createTime?: string
  updateTime?: string
}

export interface ProblemSolution {
  id: number
  problemType: string
  categoryNameEn?: string
  solutionTitle: string
  solutionContent: string
  keywords?: string
  sourceType?: string
  priority?: number
  useCount?: number
  enabled: number
  createTime?: string
  updateTime?: string
}

export interface PromptTemplate {
  id: number
  templateName: string
  businessType: string
  language: string
  templateContent: string
  variableSchema?: string
  defaultFlag: number
  enabled: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface AiCallLog {
  id: number
  userId?: number
  businessType: string
  targetType?: string
  targetId?: string
  promptTemplateId?: number
  modelName?: string
  callStatus: string
  tokenUsage?: number
  estimatedCost?: number
  latencyMs?: number
  errorMessage?: string
  createTime?: string
}

export interface AiCallLogOverview {
  totalCalls: number
  successCalls: number
  failedCalls: number
  successRate: number
  totalTokens: number
  totalCost: number
  avgLatencyMs: number
}

export interface TaskRecord {
  recordKey: string
  sourceId: number
  sourceTable: string
  taskName: string
  taskType: string
  taskStatus: string
  progress?: number
  targetType?: string
  targetId?: string
  errorMessage?: string
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface ReportOverview extends Overview {
  trendDistribution: TrendItem[]
  sentimentDistribution: DistributionItem[]
  problemDistribution: DistributionItem[]
  highRiskProducts: Product[]
  hotProducts: Product[]
  topRatedProducts: Product[]
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

export interface CsvImportPreflight {
  ready: boolean
  requiredFields: string[]
  estimatedRows: number
  duplicateLikely: boolean
  duplicateMessage?: string
  lastTaskId?: number
}

export interface OperationReport {
  reportId: number
  targetType?: string
  targetId?: string
  reportTitle: string
  consumerPainPoints: string
  productAdvantages: string
  productDisadvantages: string
  operationSuggestions: string
  copywritingSuggestions: string
  serviceSuggestions: string
  fullReport: string
  modelName: string
  createTime?: string
  evidence?: ReportEvidence[]
}

export interface ReportEvidence {
  sourceType: 'review_evidence' | 'problem_solution' | string
  sourceId: number
  title?: string
  score?: number
  retrievalVersion?: string
}

export interface RagReference {
  sourceType: 'problem_solution' | 'historical_reply' | string
  sourceId: number
  title?: string
  score?: number
}

export interface RagIndexStatus {
  enabled: boolean
  ready: boolean
  state: 'disabled' | 'empty' | 'building' | 'ready' | 'failed' | string
  collection?: string
  documentCount?: number
  problemSolutionCount?: number
  historicalReplyCount?: number
  reviewEvidenceCount?: number
  embeddingModel?: string
  lastReindexAt?: string
  lastError?: string
}

export interface ReportArchive {
  archiveId: number
  sourceReportId: number
  taskId?: number
  targetType?: string
  targetId?: string
  reportTitle?: string
  consumerPainPoints?: string
  productAdvantages?: string
  productDisadvantages?: string
  operationSuggestions?: string
  copywritingSuggestions?: string
  serviceSuggestions?: string
  riskTips?: string
  fullReport?: string
  modelName?: string
  reportCreateTime?: string
  archiveStatus: 'archived' | 'restored'
  archiveRemark?: string
  archivedBy?: number
  archiveTime?: string
  createTime?: string
  updateTime?: string
  evidence?: ReportEvidence[]
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
  ragUsed?: boolean
  ragReferences?: RagReference[]
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
