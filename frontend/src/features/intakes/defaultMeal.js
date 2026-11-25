import { dateString } from '../../shared/utils/form'

/** 현재 시각으로 정한 기본 끼니. 오늘이 아닌 날짜를 보고 있으면 아침. */
export function defaultMeal(date, now = new Date()) {
  if (date !== dateString(now)) return 'BREAKFAST'
  const hour = now.getHours()
  if (hour < 10) return 'BREAKFAST'
  if (hour < 15) return 'LUNCH'
  if (hour < 21) return 'DINNER'
  return 'SNACK'
}
