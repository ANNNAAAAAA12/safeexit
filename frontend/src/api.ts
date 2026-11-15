import type { Session } from './types'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export class ApiError extends Error {
  readonly status: number

  constructor(
    message: string,
    status: number,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function request<T>(
  path: string,
  session: Pick<Session, 'token'> | null,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (session) {
    headers.set('Authorization', `Bearer ${session.token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const raw = await response.text()
    let message = raw || `La solicitud falló (${response.status})`
    try {
      const body = JSON.parse(raw) as {
        message?: unknown
        mensaje?: unknown
        error?: unknown
      }
      const candidate = body.message ?? body.mensaje ?? body.error
      if (typeof candidate === 'string') {
        message = candidate
      }
    } catch {
      if (raw.startsWith('<')) {
        message = `La solicitud falló (${response.status})`
      }
    }
    throw new ApiError(message, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function requestBlob(
  path: string,
  session: Pick<Session, 'token'>,
): Promise<Blob> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      Accept: 'image/*',
      Authorization: `Bearer ${session.token}`,
    },
  })
  if (!response.ok) {
    throw new ApiError('No fue posible obtener la imagen de auditoría.', response.status)
  }
  return response.blob()
}

export function jsonBody(value: unknown): string {
  return JSON.stringify(value)
}
