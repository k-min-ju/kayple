import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import type { Reservation } from '../api'
import { Button, UserLayout } from '../components/ui'
import { dateTimeLabel } from '../format'

export default function BookingDonePage() {
  const navigate = useNavigate()
  const r = useLocation().state as Reservation | null
  if (!r) return <Navigate to="/" replace />

  const rows: [string, string][] = [
    ['예약 번호', `#${r.id}`],
    ['날짜 · 시간', dateTimeLabel(r.date, r.hour)],
    ['이름', r.name],
    ['연락처', r.phone],
  ]
  return (
    <UserLayout>
      <div className="mb-8 flex flex-col items-center text-center">
        <div className="mb-6 flex size-16 items-center justify-center rounded-full bg-success text-3xl text-white">✓</div>
        <h1 className="text-2xl font-bold">예약이 완료되었습니다</h1>
        <p className="mt-2 text-sm text-gray-500">아래 내용으로 예약되었습니다</p>
      </div>
      <dl className="mb-8 space-y-4 rounded-xl bg-surface px-6 py-6 text-sm">
        {rows.map(([k, v]) => (
          <div key={k} className="flex justify-between">
            <dt className="text-gray-500">{k}</dt>
            <dd>{v}</dd>
          </div>
        ))}
      </dl>
      <div className="grid grid-cols-2 gap-2">
        <Button variant="outline" onClick={() => navigate('/my')}>내 예약 확인</Button>
        <Button onClick={() => navigate('/')}>처음으로</Button>
      </div>
    </UserLayout>
  )
}
