import { describe, expect, it } from 'vitest'

import { defaultMeal } from './defaultMeal'

const TODAY = '2026-11-25'
const at = (hour, minute) => new Date(2026, 10, 25, hour, minute)

describe('defaultMeal', () => {
  it.each([
    [0, 0, 'BREAKFAST'],
    [9, 59, 'BREAKFAST'],
    [10, 0, 'LUNCH'],
    [14, 59, 'LUNCH'],
    [15, 0, 'DINNER'],
    [20, 59, 'DINNER'],
    [21, 0, 'SNACK'],
    [23, 59, 'SNACK'],
  ])('오늘 %i:%i는 %s', (hour, minute, meal) => {
    expect(defaultMeal(TODAY, at(hour, minute))).toBe(meal)
  })

  it('오늘이 아닌 날짜면 시각과 상관없이 아침', () => {
    expect(defaultMeal('2026-11-24', at(22, 0))).toBe('BREAKFAST')
    expect(defaultMeal('2026-11-26', at(12, 0))).toBe('BREAKFAST')
  })

  it('현재 시각을 생략해도 동작한다', () => {
    expect(defaultMeal('1999-01-01')).toBe('BREAKFAST')
  })
})
