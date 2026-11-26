import { dateString } from '../utils/form'

export function shift(value, days) {
  const date = new Date(`${value}T12:00:00`)
  date.setDate(date.getDate() + days)
  return dateString(date)
}

/** 달 단위로 이동한다. 도착한 달에 같은 날짜가 없으면 그 달의 말일로 맞춘다(1/31 + 1달 = 2/28). */
export function shiftMonth(value, months) {
  const [year, month, day] = value.split('-').map(Number)
  const target = new Date(year, month - 1 + months, 1, 12)
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
  target.setDate(Math.min(day, lastDay))
  return dateString(target)
}
