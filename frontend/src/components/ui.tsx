import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'
import { Link } from 'react-router-dom'

export function Header({ title, right }: { title: string; right?: ReactNode }) {
  return (
    <header className="flex h-16 items-center justify-between border-b border-line px-6 sm:px-10">
      <span className="font-bold">{title}</span>
      <div className="text-sm text-gray-500">{right}</div>
    </header>
  )
}

export function UserLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen">
      <Header title="상담 예약" right={<Link to="/my" className="hover:text-gray-800">내 예약 확인</Link>} />
      <main className="mx-auto w-full max-w-md px-4 py-14">{children}</main>
    </div>
  )
}

export function PageTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-8">
      <h1 className="text-2xl font-bold">{title}</h1>
      {subtitle && <p className="mt-2 text-sm text-gray-500">{subtitle}</p>}
    </div>
  )
}

type Variant = 'primary' | 'outline' | 'danger' | 'dangerOutline'
const VARIANTS: Record<Variant, string> = {
  primary: 'bg-primary text-white hover:bg-primary-dark disabled:bg-gray-300',
  outline: 'border border-line bg-white text-gray-900 hover:bg-gray-50',
  danger: 'bg-danger text-white hover:opacity-90',
  dangerOutline: 'border border-danger bg-white text-danger hover:bg-red-50',
}

export function Button({ variant = 'primary', className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      {...props}
      className={`h-12 rounded-lg px-4 font-semibold transition disabled:cursor-not-allowed ${VARIANTS[variant]} ${className}`}
    />
  )
}

export function Field({ label, error, id, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; id: string }) {
  return (
    <div className="mb-5">
      <label htmlFor={id} className="mb-2 block text-sm font-semibold">{label}</label>
      <input
        id={id}
        {...props}
        aria-invalid={!!error}
        className={`h-12 w-full rounded-lg border px-4 outline-none focus:border-primary ${error ? 'border-danger' : 'border-line'}`}
      />
      {error && <p className="mt-2 text-sm text-danger">{error}</p>}
    </div>
  )
}

export function Banner({ message }: { message: string }) {
  return <div role="alert" className="mb-8 rounded-lg bg-red-50 px-5 py-4 text-sm text-danger">{message}</div>
}

export function Badge({ status }: { status: 'BOOKED' | 'CANCELLED' }) {
  return status === 'BOOKED'
    ? <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-primary">예약됨</span>
    : <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs text-gray-500">취소됨</span>
}
