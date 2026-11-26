const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

// 'yyyy-MM-dd' 를 로컬 달력 날짜로 해석 (요일 계산용, 시간대 영향 없음)
function parts(date: string) {
  const [y, m, d] = date.split('-').map(Number)
  return { m, d, weekday: WEEKDAYS[new Date(y, m - 1, d).getDay()] }
}

export const hourLabel = (hour: number) => `${String(hour).padStart(2, '0')}:00`

/** 11월 18일 (수) */
export function dateLabel(date: string) {
  const { m, d, weekday } = parts(date)
  return `${m}월 ${d}일 (${weekday})`
}

/** 11월 18일 (수) 14:00 */
export const dateTimeLabel = (date: string, hour: number) => `${dateLabel(date)} ${hourLabel(hour)}`

/** { weekday: '수', short: '11.18' } */
export function dateChip(date: string) {
  const { m, d, weekday } = parts(date)
  return { weekday, short: `${m}.${String(d).padStart(2, '0')}` }
}

/** 2026-11-15T10:24:00 → 2026.11.15 10:24 */
export const createdAtLabel = (iso: string) => `${iso.slice(0, 10).replaceAll('-', '.')} ${iso.slice(11, 16)}`

export const PHONE_REGEX = /^01[016789]-?\d{3,4}-?\d{4}$/
