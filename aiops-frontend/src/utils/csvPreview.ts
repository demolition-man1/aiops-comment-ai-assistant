export interface CsvPreviewResult {
  columns: string[]
  rows: Record<string, string>[]
  estimatedRows: number
}

export interface CsvColumnMapping {
  product_id?: string
  review_score?: string
  review_content?: string
  review_title?: string
  review_id?: string
  order_id?: string
  seller_id?: string
  review_time?: string
}

const REQUIRED_FIELDS: Array<keyof CsvColumnMapping> = ['product_id', 'review_score']

const COLUMN_ALIASES: Record<keyof CsvColumnMapping, string[]> = {
  product_id: ['product_id', 'product id', 'productid', '商品id', '商品ID', '商品编号', 'sku', 'sku_id', 'item_id'],
  review_score: ['review_score', 'review score', 'score', 'rating', '评分', '评价分数', '星级'],
  review_content: [
    'review_content',
    'review_comment',
    'review_comment_message',
    'comment',
    'content',
    '评价内容',
    '评论内容',
    '用户评论'
  ],
  review_title: ['review_title', 'review_comment_title', 'title', '评价标题', '评论标题'],
  review_id: ['review_id', 'review id', 'comment_id', '评价id', '评论id'],
  order_id: ['order_id', 'order id', '订单id', '订单编号'],
  seller_id: ['seller_id', 'seller id', 'shop_id', '商家id', '店铺id'],
  review_time: ['review_time', 'review_creation_date', 'created_at', '评价时间', '评论时间']
}

export function estimateCsvRows(csvText: string): number {
  const nonEmptyLines = csvText.split(/\r?\n/).filter((line) => line.trim().length > 0)
  return Math.max(nonEmptyLines.length - 1, 0)
}

export function parseCsvPreview(csvText: string, maxRows = 20): CsvPreviewResult {
  const lines = csvText.split(/\r?\n/).filter((line) => line.trim().length > 0)
  if (lines.length === 0) {
    return { columns: [], rows: [], estimatedRows: 0 }
  }

  const columns = parseCsvLine(lines[0]).map(normalizeCsvColumn)
  const rows = lines.slice(1, maxRows + 1).map((line) => {
    const values = parseCsvLine(line)
    return columns.reduce<Record<string, string>>((row, column, index) => {
      row[column] = values[index] || ''
      return row
    }, {})
  })

  return {
    columns,
    rows,
    estimatedRows: estimateCsvRows(csvText)
  }
}

export function suggestColumnMapping(columns: string[]): CsvColumnMapping {
  return (Object.keys(COLUMN_ALIASES) as Array<keyof CsvColumnMapping>).reduce<CsvColumnMapping>((mapping, field) => {
    const matched = columns.find((column) => aliasMatches(column, COLUMN_ALIASES[field]))
    if (matched) {
      mapping[field] = matched
    }
    return mapping
  }, {})
}

export function getMissingMappedFields(mapping: CsvColumnMapping): string[] {
  return REQUIRED_FIELDS.filter((field) => !mapping[field])
}

function aliasMatches(column: string, aliases: string[]): boolean {
  const normalizedColumn = normalizeAlias(column)
  return aliases.some((alias) => normalizeAlias(alias) === normalizedColumn)
}

function normalizeAlias(value: string): string {
  return normalizeCsvColumn(value).toLowerCase().replace(/[\s_\-]/g, '')
}

function normalizeCsvColumn(column: string): string {
  return column.trim().replace(/^\uFEFF/, '').replace(/^"|"$/g, '')
}

function parseCsvLine(line: string): string[] {
  const values: string[] = []
  let current = ''
  let quoted = false

  for (let index = 0; index < line.length; index += 1) {
    const char = line[index]
    const next = line[index + 1]
    if (char === '"' && quoted && next === '"') {
      current += '"'
      index += 1
      continue
    }
    if (char === '"') {
      quoted = !quoted
      continue
    }
    if (char === ',' && !quoted) {
      values.push(current.trim())
      current = ''
      continue
    }
    current += char
  }
  values.push(current.trim())
  return values
}
