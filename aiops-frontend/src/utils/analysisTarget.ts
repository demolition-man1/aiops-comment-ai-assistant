export interface AnalysisTargetInput {
  queryProductId?: string
  selectedProductId?: string
  firstVisibleProductId?: string
}

export function resolveAnalysisProductId(input: AnalysisTargetInput) {
  return (
    input.selectedProductId?.trim()
    || input.firstVisibleProductId?.trim()
    || input.queryProductId?.trim()
    || ''
  )
}
