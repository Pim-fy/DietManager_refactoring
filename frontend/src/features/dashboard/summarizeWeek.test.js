import { describe, expect, it } from 'vitest'

import { summarizeWeek } from './summarizeWeek'

const day = (records, value, missingCount = 0) => ({ records, calories: { value, missingCount } })

describe('summarizeWeek', () => {
  it('기록 없는 날은 제외한다', () => {
    const result = summarizeWeek([day(0, 0), day(2, 1800), day(1, 2200), day(0, 0)])
    expect(result.recorded).toHaveLength(2)
    expect(result.withCalories).toHaveLength(2)
    expect(result.average).toBe(2000)
  })

  it('칼로리 정보가 없는 날(기록 수 = 누락 수)은 평균에서 제외한다', () => {
    const result = summarizeWeek([day(2, 0, 2), day(1, 1500), day(3, 900, 1)])
    expect(result.recorded).toHaveLength(3)
    expect(result.withCalories).toHaveLength(2)
    expect(result.average).toBe(1200)
  })

  it('칼로리 정보가 있는 날이 없으면 평균은 NaN', () => {
    const result = summarizeWeek([day(1, 0, 1), day(2, 0, 2)])
    expect(result.recorded).toHaveLength(2)
    expect(result.withCalories).toHaveLength(0)
    expect(result.average).toBeNaN()
  })

  it('기록이 전혀 없으면 비어 있다', () => {
    const result = summarizeWeek([day(0, 0), day(0, 0)])
    expect(result.recorded).toEqual([])
    expect(result.withCalories).toEqual([])
  })
})
