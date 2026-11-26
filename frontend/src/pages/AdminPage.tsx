import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiError, type Reservation } from '../api'
import { Badge, Banner, Button, Field, Header } from '../components/ui'
import { createdAtLabel, dateLabel, hourLabel } from '../format'

type AuthState = 'checking' | 'out' | 'in'

export default function AdminPage() {
  const [auth, setAuth] = useState<AuthState>('checking')

  useEffect(() => {
    api.adminMe().then(() => setAuth('in')).catch(() => setAuth('out'))
  }, [])

  async function logout() {
    await api.adminLogout().catch(() => undefined)
    setAuth('out')
  }

  return (
    <div className="min-h-screen">
      <Header
        title="상담 예약 관리자"
        right={auth === 'in' && <button onClick={logout} className="hover:text-gray-800">로그아웃</button>}
      />
      {auth === 'out' && <LoginForm onSuccess={() => setAuth('in')} />}
      {auth === 'in' && <ReservationTable onUnauthorized={() => setAuth('out')} />}
    </div>
  )
}

function LoginForm({ onSuccess }: { onSuccess: () => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await api.adminLogin(username, password)
      onSuccess()
    } catch (err) {
      setError((err as ApiError).message)
    }
  }

  return (
    <main className="mx-auto w-full max-w-sm px-4 py-14">
      <h1 className="mb-8 text-2xl font-bold">관리자 로그인</h1>
      <form onSubmit={submit} noValidate>
        <Field id="username" label="아이디" placeholder="admin" autoComplete="username"
          value={username} onChange={(e) => setUsername(e.target.value)} />
        <Field id="password" label="비밀번호" type="password" placeholder="••••••••" autoComplete="current-password"
          value={password} onChange={(e) => setPassword(e.target.value)} error={error} />
        <Button type="submit" className="mt-4 w-full">로그인</Button>
      </form>
    </main>
  )
}

function ReservationTable({ onUnauthorized }: { onUnauthorized: () => void }) {
  const [dates, setDates] = useState<string[]>([])
  const [date, setDate] = useState('')
  const [items, setItems] = useState<Reservation[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    api.dates().then((ds) => { setDates(ds); setDate(ds[0]) }).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    if (!date) return
    api.adminReservations(date)
      .then(setItems)
      .catch((e: ApiError) => (e.status === 401 ? onUnauthorized() : setError(e.message)))
  }, [date, onUnauthorized])

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-14">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">예약 관리</h1>
        <select value={date} onChange={(e) => setDate(e.target.value)} aria-label="날짜 선택"
          className="h-12 w-48 rounded-lg border border-line bg-white px-4">
          {dates.map((d) => <option key={d} value={d}>{dateLabel(d)}</option>)}
        </select>
      </div>
      {error && <Banner message={error} />}
      <div className="overflow-x-auto rounded-xl border border-line">
        <table className="w-full min-w-[600px] text-left text-sm">
          <thead className="bg-surface text-gray-500">
            <tr>{['시간', '이름', '연락처', '상태', '예약 일시'].map((h) => <th key={h} className="px-6 py-4 font-normal">{h}</th>)}</tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr><td colSpan={5} className="px-6 py-10 text-center text-gray-500">예약이 없습니다</td></tr>
            ) : items.map((r) => (
              <tr key={r.id} className="border-t border-line">
                <td className="px-6 py-4">{hourLabel(r.hour)}</td>
                <td className="px-6 py-4">{r.name}</td>
                <td className="px-6 py-4">{r.phone}</td>
                <td className="px-6 py-4"><Badge status={r.status} /></td>
                <td className="px-6 py-4 text-gray-500">{createdAtLabel(r.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  )
}
