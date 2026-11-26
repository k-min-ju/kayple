export type Status = 'BOOKED' | 'CANCELLED'

export interface Slot { hour: number; available: boolean }

export interface Reservation {
  id: number
  date: string // yyyy-MM-dd
  hour: number
  name: string
  phone: string // 사용자 API 는 마스킹, 관리자 API 는 전체
  status: Status
  createdAt: string // yyyy-MM-ddTHH:mm:ss
  cancellable: boolean // 예약 중이고 아직 지나지 않은 상담
}

export class ApiError extends Error {
  readonly status: number
  readonly field?: string
  constructor(status: number, message: string, field?: string) {
    super(message)
    this.status = status
    this.field = field
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response
  try {
    res = await fetch(path, {
      ...init,
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    })
  } catch {
    throw new ApiError(0, '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
  }
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.message ?? '요청을 처리하지 못했습니다.', body?.field ?? undefined)
  }
  return (res.status === 204 ? undefined : await res.json()) as T
}

const post = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })

export const api = {
  dates: () => request<string[]>('/api/dates'),
  slots: (date: string) => request<Slot[]>(`/api/slots?date=${date}`),
  reserve: (body: { date: string; hour: number; name: string; phone: string }) =>
    request<Reservation>('/api/reservations', post(body)),
  myReservations: (phone: string) =>
    request<Reservation[]>('/api/reservations/lookup', post({ phone })),
  cancel: (id: number, phone: string) => request<void>(`/api/reservations/${id}/cancel`, post({ phone })),
  adminLogin: (username: string, password: string) => request<void>('/api/admin/login', post({ username, password })),
  adminLogout: () => request<void>('/api/admin/logout', { method: 'POST' }),
  adminMe: () => request<void>('/api/admin/me'),
  adminReservations: (date: string) => request<Reservation[]>(`/api/admin/reservations?date=${date}`),
}
