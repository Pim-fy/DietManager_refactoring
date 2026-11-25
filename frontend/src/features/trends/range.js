import { dateString } from '../../shared/utils/form'

/** 기준 날짜가 속한 주(월~일) 또는 달의 시작·끝 날짜. */
export function dates(anchor, period) {
  const start = new Date(`${anchor}T12:00:00`)
  const end = new Date(start)
  if (period === 'week') {
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7))
    end.setTime(start.getTime())
    end.setDate(end.getDate() + 6)
  } else {
    start.setDate(1)
    end.setMonth(end.getMonth() + 1, 0)
  }
  return { start: dateString(start), end: dateString(end) }
}
