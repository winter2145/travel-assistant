import http from './http'

export interface RouteResult {
  origin: string
  destination: string
  originCoordinate: string
  destinationCoordinate: string
  mode: 'driving' | 'walking' | 'transit'
  route: Record<string, unknown>
}

export function queryRoute(request: { origin: string; destination: string; city: string; mode: RouteResult['mode'] }) {
  return http.post<RouteResult>('/routes/query', request)
}
