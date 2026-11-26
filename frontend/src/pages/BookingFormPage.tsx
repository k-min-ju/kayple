import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api'
import { Banner, Button, Field, PageTitle, UserLayout } from '../components/ui'
import { dateTimeLabel, PHONE_REGEX } from '../format'
import type { Selection } from './BookingPage'

type Errors = { name?: string; phone?: string }

function validate(name: string, phone: string): Errors {
  const errors: Errors = {}
  if (!name.trim()) errors.name = '이름을 입력해 주세요'
  else if (name.trim().length > 20) errors.name = '이름은 20자 이하로 입력해 주세요'
  if (!PHONE_REGEX.test(phone.trim())) errors.phone = '연락처 형식이 올바르지 않습니다'
  return errors
}

export default function BookingFormPage() {
  const navigate = useNavigate()
  const selection = useLocation().state as Selection | null
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [errors, setErrors] = useState<Errors>({})
  const [banner, setBanner] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (!selection?.date || selection.hour == null) return <Navigate to="/" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    const found = validate(name, phone)
    setErrors(found)
    setBanner('')
    if (Object.keys(found).length > 0) return
    setSubmitting(true)
    try {
      const reservation = await api.reserve({ ...selection!, name: name.trim(), phone: phone.trim() })
      navigate('/done', { replace: true, state: reservation })
    } catch (err) {
      const apiErr = err as ApiError
      if (apiErr.field === 'name' || apiErr.field === 'phone') setErrors({ [apiErr.field]: apiErr.message })
      else setBanner(apiErr.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <UserLayout>
      {banner && <Banner message={banner} />}
      <PageTitle title="예약 정보 입력" subtitle="예약자 정보를 입력해 주세요" />
      <div className="mb-8 flex items-center rounded-xl bg-surface px-5 py-4 text-sm">
        <span className="mr-4 text-gray-500">예약 일시</span>
        <span className="font-bold">{dateTimeLabel(selection.date, selection.hour)}</span>
        <Link to="/" state={selection} className="ml-auto text-primary">변경</Link>
      </div>
      <form onSubmit={submit} noValidate>
        <Field id="name" label="이름" placeholder="홍길동" value={name} maxLength={20}
          onChange={(e) => setName(e.target.value)} error={errors.name} />
        <Field id="phone" label="연락처" placeholder="010-1234-5678" inputMode="tel" value={phone}
          onChange={(e) => setPhone(e.target.value)} error={errors.phone} />
        <Button type="submit" className="mt-4 w-full" disabled={submitting}>예약하기</Button>
      </form>
    </UserLayout>
  )
}
