import { useState, type FormEvent } from 'react'
import { api, type Reservation } from '../api'
import { Badge, Banner, Button, PageTitle, UserLayout } from '../components/ui'
import { dateTimeLabel, PHONE_REGEX } from '../format'

export default function MyReservationsPage() {
  const [phone, setPhone] = useState('')
  const [searched, setSearched] = useState<string | null>(null)
  const [items, setItems] = useState<Reservation[]>([])
  const [error, setError] = useState('')
  const [target, setTarget] = useState<Reservation | null>(null)

  async function load(p: string) {
    setItems(await api.myReservations(p))
    setSearched(p)
  }

  async function search(e: FormEvent) {
    e.preventDefault()
    setError('')
    const p = phone.trim()
    if (!PHONE_REGEX.test(p)) return setError('연락처 형식이 올바르지 않습니다')
    await load(p).catch((err) => setError(err.message))
  }

  async function confirmCancel() {
    if (!target || !searched) return
    try {
      await api.cancel(target.id, searched)
      await load(searched)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setTarget(null)
    }
  }

  return (
    <UserLayout>
      <PageTitle title="내 예약 확인" subtitle="예약할 때 입력한 연락처로 조회할 수 있습니다" />
      <form onSubmit={search} className="mb-10" noValidate>
        <label htmlFor="phone" className="mb-2 block text-sm font-semibold">연락처</label>
        <div className="flex gap-2">
          <input id="phone" value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel"
            placeholder="010-1234-5678"
            className="h-12 flex-1 rounded-lg border border-line px-4 outline-none focus:border-primary" />
          <Button type="submit" className="w-24">조회</Button>
        </div>
      </form>
      {error && <Banner message={error} />}

      <section>
          <h2 className="mb-4 font-semibold">예약 내역</h2>
          {items.length === 0 ? (
            <div className="rounded-xl bg-surface py-14 text-center text-sm text-gray-500">예약 내역이 없습니다</div>
          ) : (
            <ul className="max-h-[420px] space-y-3 overflow-y-auto pr-1">
              {items.map((r) => (
                <li key={r.id} className="flex items-center rounded-xl bg-surface px-6 py-5">
                  <div>
                    <div className="flex items-center gap-2 font-bold">
                      {dateTimeLabel(r.date, r.hour)} <Badge status={r.status} />
                    </div>
                    <div className="mt-2 text-sm text-gray-500">{r.name}</div>
                  </div>
                  {r.cancellable && (
                    <Button variant="dangerOutline" className="ml-auto w-20" onClick={() => setTarget(r)}>취소</Button>
                  )}
                </li>
              ))}
            </ul>
          )}
      </section>

      {target && (
        <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/40 px-4" role="dialog" aria-modal="true">
          <div className="w-full max-w-sm rounded-xl bg-white p-6">
            <p className="mb-6 font-semibold">{dateTimeLabel(target.date, target.hour)} 예약을 취소할까요?</p>
            <div className="grid grid-cols-2 gap-2">
              <Button variant="outline" onClick={() => setTarget(null)}>닫기</Button>
              <Button variant="danger" onClick={confirmCancel}>취소하기</Button>
            </div>
          </div>
        </div>
      )}
    </UserLayout>
  )
}
