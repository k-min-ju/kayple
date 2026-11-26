import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api, type Slot } from '../api'
import { Banner, Button, PageTitle, UserLayout } from '../components/ui'
import { dateChip, dateTimeLabel, hourLabel } from '../format'

export interface Selection { date: string; hour: number }

export default function BookingPage() {
  const navigate = useNavigate()
  const prev = (useLocation().state as Selection | null) ?? null
  const [dates, setDates] = useState<string[]>([])
  const [date, setDate] = useState<string | null>(prev?.date ?? null)
  const [slots, setSlots] = useState<Slot[]>([])
  const [hour, setHour] = useState<number | null>(prev?.hour ?? null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.dates()
      .then((ds) => {
        setDates(ds)
        setDate((d) => (d && ds.includes(d) ? d : ds[0]))
      })
      .catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    if (!date) return
    api.slots(date)
      .then((s) => {
        setSlots(s)
        setHour((h) => (s.some((x) => x.hour === h && x.available) ? h : null))
      })
      .catch((e) => setError(e.message))
  }, [date])

  return (
    <UserLayout>
      {error && <Banner message={error} />}
      <PageTitle title="상담 예약하기" subtitle="날짜와 시간을 선택해 주세요" />

      <h2 className="mb-4 font-semibold">날짜 선택</h2>
      <div className="mb-10 grid grid-cols-7 gap-2">
        {dates.map((d) => {
          const { weekday, short } = dateChip(d)
          const selected = d === date
          return (
            <button
              key={d}
              onClick={() => setDate(d)}
              aria-pressed={selected}
              className={`flex flex-col items-center rounded-lg border py-2.5 ${selected ? 'border-primary bg-primary text-white' : 'border-line hover:border-primary'}`}
            >
              <span className={`text-xs ${selected ? 'text-white' : 'text-gray-500'}`}>{weekday}</span>
              <span className="text-sm font-bold">{short}</span>
            </button>
          )
        })}
      </div>

      <h2 className="mb-4 font-semibold">시간 선택</h2>
      <div className="mb-8 grid grid-cols-4 gap-2">
        {slots.map((s) => {
          const selected = s.hour === hour
          return (
            <button
              key={s.hour}
              disabled={!s.available}
              onClick={() => setHour(s.hour)}
              aria-pressed={selected}
              className={`h-12 rounded-lg border text-sm ${
                !s.available
                  ? 'cursor-not-allowed border-transparent bg-gray-100 text-gray-400'
                  : selected ? 'border-primary bg-primary text-white' : 'border-line hover:border-primary'
              }`}
            >
              {hourLabel(s.hour)}
              {!s.available && <span className="ml-1.5 text-xs">마감</span>}
            </button>
          )
        })}
      </div>

      <div className="mb-4 flex items-center justify-between text-sm">
        <span className="text-gray-500">선택한 일정</span>
        <span className="font-bold">{date && hour !== null ? dateTimeLabel(date, hour) : '-'}</span>
      </div>
      <Button
        className="w-full"
        disabled={!date || hour === null}
        onClick={() => navigate('/reserve', { state: { date, hour } })}
      >
        다음
      </Button>
    </UserLayout>
  )
}
