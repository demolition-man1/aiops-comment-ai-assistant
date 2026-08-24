export function toPercentValue(value?: number | string | null) {
  const numeric = Number(value || 0)
  if (!Number.isFinite(numeric)) {
    return 0
  }
  const percent = Math.abs(numeric) <= 1 ? numeric * 100 : numeric
  return Number(percent.toFixed(4))
}

export function toProgressPercent(value?: number | string | null) {
  return Math.min(100, Math.max(0, toPercentValue(value)))
}

export function formatPercent(value?: number | string | null, digits = 1) {
  return `${toPercentValue(value).toFixed(digits)}%`
}
