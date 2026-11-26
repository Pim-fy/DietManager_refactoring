import { dateString, today } from './form'

/** 'YYYY-MM-DD' 형식이면서 실제로 있는 날짜인지(예: 2026-02-30은 아님). */
export function isValidDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = new Date(`${value}T12:00:00`)
  return !Number.isNaN(date.getTime()) && dateString(date) === value
}

/** 주소의 날짜 값을 화면에서 쓸 날짜로 바로잡는다. 없거나 틀리면 오늘, future가 아니면 미래도 오늘로 보정한다. */
export function resolveDate(raw, { future = false, now = today() } = {}) {
  if (!isValidDate(raw)) return now
  if (!future && raw > now) return now
  return raw
}

/** 주소에 쓸 날짜 값. 오늘이면 주소에서 생략(null)한다. */
export function dateParamValue(date, now = today()) {
  return date === now ? null : date
}

/** 기간 값. week/month 외에는 week. */
export function resolvePeriod(raw) {
  return raw === 'month' ? 'month' : 'week'
}

/** 기간 값을 주소에 쓸 값으로. 기본값(week)이면 생략(null)한다. */
export function periodParamValue(period) {
  return period === 'month' ? 'month' : null
}

/** search 문자열(예: '?a=1')에 updates를 적용해 새 search 문자열을 만든다. 값이 null이면 해당 키를 지운다. */
export function applyParams(search, updates) {
  const params = new URLSearchParams(search)
  for (const [key, value] of Object.entries(updates)) {
    if (value == null) params.delete(key)
    else params.set(key, value)
  }
  const text = params.toString()
  return text ? `?${text}` : ''
}
