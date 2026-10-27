import http from './http'
import type { CreatePdfExportRequest } from '@/types/pdf'

/** Backend renders a real PDF and returns it as an attachment. */
export async function createPdfExport(planId: string, req: CreatePdfExportRequest): Promise<Blob> {
  const response = await http.post<Blob>(`/plans/${planId}/pdf-exports`, req, { responseType: 'blob' })
  return response
}
