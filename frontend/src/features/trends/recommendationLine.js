import { ticks } from './ticks'

/** 날짜별 권장 칼로리. 계산할 수 없거나 필드가 없는(구버전) 응답이면 null. */
export function recommendedValues(days) {
  return days.map(day => {
    const value = day.recommendedCalories == null ? NaN : Number(day.recommendedCalories)
    return Number.isFinite(value) ? value : null
  })
}

/**
 * 계단형 기준선 구간. 값이 있는 날이 이어지면 한 구간으로 묶고, 값이 없는 날에서 끊는다.
 * 각 날짜는 자기 위치 ±0.5칸을 수평으로 차지하고(양 끝은 그래프 범위로 자름), 값이 바뀌는 경계에서 수직으로 잇는다.
 * 반환: 구간 배열. 구간은 [{ position, value }] 꼭짓점 목록이고 position은 날짜 순번 기준 위치다.
 */
export function stepSegments(values) {
  const last = values.length - 1
  const clamp = position => Math.min(Math.max(position, 0), last)
  const segments = []
  let current = null
  values.forEach((value, index) => {
    if (value == null) {
      current = null
      return
    }
    const from = clamp(index - 0.5)
    const to = clamp(index + 0.5)
    if (!current) {
      current = []
      segments.push(current)
    } else if (current.at(-1).value === value) {
      current.at(-1).position = to
      return
    }
    current.push({ position: from, value }, { position: to, value })
  })
  return segments
}

/** 칼로리 그래프 세로축 눈금. 섭취 값과 권장량을 모두 덮고 0부터 시작한다. */
export function calorieTicks(intakes, recommended) {
  const known = [...intakes, ...recommended].filter(value => value != null)
  return ticks(0, Math.max(...known, 100))
}
