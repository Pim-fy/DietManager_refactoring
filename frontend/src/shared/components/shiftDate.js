import { dateString } from '../utils/form'

export function shift(value, days) {
  const date = new Date(`${value}T12:00:00`)
  date.setDate(date.getDate() + days)
  return dateString(date)
}
