import type { BudgetCheckResult, Itinerary, ItineraryActivity } from '@/types/plan'

/**
 * 行程/预算解析工具：从后端 Manus 的 SSE Step 文本中提取结构化结果。
 *
 * 后端真实输出格式（实测）：
 *   data:Step 1: 工具 searchAttractions 返回的结果：[{"text":"{\"success\":true,...}"}]
 * 工具返回体是 ToolResult { success, code, message, data }，data 可能被二次转义为字符串。
 */

/** 解析结果：识别出的结构化数据 */
export interface ParsedToolPayload {
  toolName: string | null
  /** 原始 ToolResult data（已反序列化） */
  data: unknown
  itinerary: Itinerary | null
  budget: BudgetCheckResult | null
  conflict: { hasConflict: boolean; conflictCount: number; conflicts: unknown[] } | null
}

/** 从 Step 文本中提取工具名，如 "工具 searchAttractions 返回的结果" */
export function extractToolName(stepText: string): string | null {
  const m = stepText.match(/工具\s+([\w.]+)\s+返回的结果/)
  return m ? m[1] : null
}

/** 从任意文本中提取第一个平衡的 JSON 值（对象或数组） */
export function extractFirstJson(text: string): unknown {
  let start = -1
  let depth = 0
  let inString = false
  let escape = false
  let openChar = ''

  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (inString) {
      if (escape) {
        escape = false
      } else if (ch === '\\') {
        escape = true
      } else if (ch === '"') {
        inString = false
      }
      continue
    }
    if (ch === '"') {
      inString = true
      continue
    }
    if (ch === '{' || ch === '[') {
      if (start < 0) {
        start = i
        openChar = ch
      }
      depth++
    } else if (ch === '}' || ch === ']') {
      depth--
      if (depth === 0 && start >= 0) {
        const candidate = text.slice(start, i + 1)
        try {
          return JSON.parse(candidate)
        } catch {
          // 尝试去掉前后包裹后继续扫描
          start = -1
        }
      }
    }
  }
  return null
}

/** 递归解包：ToolResult 的 data 可能又是 JSON 字符串 */
function unwrap(value: unknown): unknown {
  let current = value
  for (let i = 0; i < 4; i++) {
    if (Array.isArray(current)) {
      const textParts = current.flatMap((item) => {
        if (!isRecord(item)) return []
        if (typeof item.text === 'string') return [item.text]
        if (Array.isArray(item.content)) return item.content.filter((part): part is string => typeof part === 'string')
        return []
      })
      if (textParts.length > 0) {
        current = textParts.join('\n')
        continue
      }
      return current
    }
    if (typeof current === 'string') {
      const trimmed = current.trim()
      if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
        try {
          current = JSON.parse(trimmed)
          continue
        } catch {
          return current
        }
      }
      return current
    }
    if (current && typeof current === 'object') {
      const obj = current as Record<string, unknown>
      if (typeof obj.text === 'string') {
        current = obj.text
        continue
      }
      if (typeof obj.success === 'boolean' && 'data' in obj) {
        current = obj.data
        continue
      }
    }
    return current
  }
  return current
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

function toActivity(item: unknown): ItineraryActivity | null {
  if (!isRecord(item) || typeof item.name !== 'string') return null
  // Older generated plans could have stored the successful MCP text in
  // photoSearchError before the MCP content-array wrapper was handled.
  const savedPhotoError = typeof item.photoSearchError === 'string' ? item.photoSearchError : ''
  const recoveredPhoto = recoverPexelsPhoto(savedPhotoError)
  return {
    name: item.name,
    location: typeof item.location === 'string' ? item.location : undefined,
    startTime: typeof item.startTime === 'string' ? item.startTime : '',
    endTime: typeof item.endTime === 'string' ? item.endTime : '',
    estimatedCost: typeof item.estimatedCost === 'number' ? item.estimatedCost : 0,
    travelMinutesFromPrevious:
      typeof item.travelMinutesFromPrevious === 'number' ? item.travelMinutesFromPrevious : undefined,
    notes: typeof item.notes === 'string' ? item.notes : undefined,
    photoUrl: typeof item.photoUrl === 'string' ? item.photoUrl : recoveredPhoto?.photoUrl,
    photoPageUrl: typeof item.photoPageUrl === 'string' ? item.photoPageUrl : recoveredPhoto?.photoPageUrl,
    photoPhotographer: typeof item.photoPhotographer === 'string' ? item.photoPhotographer : recoveredPhoto?.photoPhotographer,
    photoSource: typeof item.photoSource === 'string' ? item.photoSource : recoveredPhoto ? 'Pexels' : undefined,
    photoSearchError: recoveredPhoto ? undefined : savedPhotoError || undefined,
  }
}

function recoverPexelsPhoto(value: string): Pick<ItineraryActivity, 'photoUrl' | 'photoPageUrl' | 'photoPhotographer'> | null {
  if (!value) return null
  const text = value.replace(/\\r\\n/g, '\n').replace(/\\n/g, '\n').replace(/\\r/g, '\n')
    .replace(/\\(?=[:./])/g, '')
  const photoUrl = text.match(/图片地址：\s*(https:\/\/images\.pexels\.com\/\S+)/)?.[1]
  if (!photoUrl) return null
  return {
    photoUrl,
    photoPageUrl: text.match(/Pexels 页面：\s*(https:\/\/www\.pexels\.com\/\S+)/)?.[1],
    photoPhotographer: text.match(/摄影师：\s*([^\r\n]+)/)?.[1]?.trim(),
  }
}

function parseItinerary(data: unknown): Itinerary | null {
  if (!isRecord(data)) return null
  const candidate = unwrap(data.itinerary ?? data)
  if (!isRecord(candidate)) return null
  const daysValue = candidate.days
  if (!Array.isArray(daysValue)) return null
  const days = daysValue
    .map((day) => {
      if (!isRecord(day) || !Array.isArray(day.activities)) return null
      return {
        day: typeof day.day === 'number' ? day.day : 0,
        date: typeof day.date === 'string' ? day.date : '',
        activities: day.activities
          .map(toActivity)
          .filter((a): a is ItineraryActivity => a !== null),
      }
    })
    .filter((d): d is NonNullable<typeof d> => d !== null)
  if (days.length === 0) return null
  return {
    city: typeof candidate.city === 'string' ? candidate.city : '',
    startDate: typeof candidate.startDate === 'string' ? candidate.startDate : '',
    days,
    budget: typeof candidate.budget === 'number' ? candidate.budget : 0,
    pace: typeof candidate.pace === 'string' ? candidate.pace : '',
    estimatedActivityCost: typeof candidate.estimatedActivityCost === 'number' ? candidate.estimatedActivityCost : 0,
    notes: Array.isArray(candidate.notes) ? candidate.notes.filter((n): n is string => typeof n === 'string') : undefined,
  }
}

/** Normalize itineraries coming directly from the plan API as well as tool SSE payloads. */
export function normalizeItinerary(value: unknown): Itinerary | null {
  return parseItinerary(value)
}

function parseBudget(data: unknown): BudgetCheckResult | null {
  if (!isRecord(data)) return null
  if (typeof data.remainingBudget !== 'number') return null
  return {
    budget: typeof data.budget === 'number' ? data.budget : 0,
    knownActivityCosts: typeof data.knownActivityCosts === 'number' ? data.knownActivityCosts : 0,
    additionalCosts: typeof data.additionalCosts === 'number' ? data.additionalCosts : 0,
    estimatedTotal: typeof data.estimatedTotal === 'number' ? data.estimatedTotal : 0,
    remainingBudget: data.remainingBudget,
    overBudget: data.overBudget === true,
    activitiesWithCost: typeof data.activitiesWithCost === 'number' ? data.activitiesWithCost : 0,
    suggestions: Array.isArray(data.suggestions)
      ? data.suggestions.filter((s): s is string => typeof s === 'string')
      : [],
    warning: typeof data.warning === 'string' ? data.warning : undefined,
  }
}

function parseConflict(
  data: unknown,
): { hasConflict: boolean; conflictCount: number; conflicts: unknown[] } | null {
  if (!isRecord(data)) return null
  if (typeof data.hasConflict !== 'boolean') return null
  return {
    hasConflict: data.hasConflict,
    conflictCount: typeof data.conflictCount === 'number' ? data.conflictCount : 0,
    conflicts: Array.isArray(data.conflicts) ? data.conflicts : [],
  }
}

/** 解析一条 Step 文本，返回结构化载荷 */
export function parseToolStep(stepText: string): ParsedToolPayload {
  const toolName = extractToolName(stepText)
  const raw = extractFirstJson(stepText)
  const data = unwrap(raw)
  return {
    toolName,
    data,
    itinerary: parseItinerary(data),
    budget: parseBudget(data),
    conflict: parseConflict(data),
  }
}
