/** PDF 攻略导出（对应后端 P4 规划，接口待接入） */

/** 导出任务状态 */
export type PdfExportStatus = 'pending' | 'generating' | 'success' | 'failed'

/** PDF 导出记录 */
export interface PdfExportRecord {
  id: string
  planId: string
  /** COS 文件 Key */
  fileKey: string | null
  /** 可访问下载链接（COS 临时/永久 URL） */
  fileUrl: string | null
  status: PdfExportStatus
  createdAt: number
  /** 失败原因 */
  errorMessage?: string
}

/** 发起 PDF 生成请求 */
export interface CreatePdfExportRequest {
  planId: string
  /** 攻略风格：简洁版 simple / 图文杂志版 magazine */
  style: 'simple' | 'magazine'
  /** 是否包含封面 */
  includeCover: boolean
  includeOverview: boolean
  /** 是否包含景点图片 */
  includePhotos: boolean
  /** 是否包含预算表 */
  includeBudget: boolean
  /** 是否包含每日行程 */
  includeItinerary: boolean
}
