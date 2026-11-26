import { describe, expect, it } from 'vitest'

import { calorieTicks, recommendedValues, stepSegments } from './recommendationLine'

describe('recommendedValues', () => {
  it('권장 칼로리를 숫자로, 없거나 null이면 null로 바꾼다', () => {
    expect(recommendedValues([{ recommendedCalories: 2100.5 }, { recommendedCalories: null }, {}, { recommendedCalories: '1800.00' }])).toEqual([
      2100.5,
      null,
      null,
      1800,
    ])
  })

  it('필드가 없는(구버전) 응답이면 모두 null이고 기준선 구간도 없다', () => {
    const values = recommendedValues([{ date: '2026-01-01' }, { date: '2026-01-02' }])
    expect(values).toEqual([null, null])
    expect(stepSegments(values)).toEqual([])
  })
})

describe('stepSegments', () => {
  it('같은 값이 이어지면 한 수평 구간이고 양 끝은 그래프 범위로 자른다', () => {
    expect(stepSegments([2000, 2000, 2000])).toEqual([
      [
        { position: 0, value: 2000 },
        { position: 2, value: 2000 },
      ],
    ])
  })

  it('값이 바뀌면 날짜 경계에서 수직으로 이어 계단이 된다', () => {
    expect(stepSegments([2000, 2000, 1700, 1700])).toEqual([
      [
        { position: 0, value: 2000 },
        { position: 1.5, value: 2000 },
        { position: 1.5, value: 1700 },
        { position: 3, value: 1700 },
      ],
    ])
  })

  it('값이 없는 날에서 끊는다', () => {
    expect(stepSegments([null, 2000, 2000, null, 1800])).toEqual([
      [
        { position: 0.5, value: 2000 },
        { position: 2.5, value: 2000 },
      ],
      [
        { position: 3.5, value: 1800 },
        { position: 4, value: 1800 },
      ],
    ])
  })

  it('하루만 있는 값도 반 칸씩 수평으로 그린다', () => {
    expect(stepSegments([null, 1900, null])).toEqual([
      [
        { position: 0.5, value: 1900 },
        { position: 1.5, value: 1900 },
      ],
    ])
  })

  it('빈 배열은 구간이 없다', () => {
    expect(stepSegments([])).toEqual([])
  })
})

describe('calorieTicks', () => {
  it('권장량이 섭취보다 크면 권장량까지 덮는다', () => {
    const values = calorieTicks([800, null, 1200], [2600, 2600, null])
    expect(values[0]).toBe(0)
    expect(values.at(-1)).toBeGreaterThanOrEqual(2600)
  })

  it('권장량이 없으면 섭취 값만으로 정한다(기존과 같음)', () => {
    expect(calorieTicks([2400], [null])).toEqual([0, 1000, 2000, 3000])
  })

  it('값이 모두 작으면 최소 100까지 덮는다', () => {
    expect(calorieTicks([10], [])).toEqual([0, 50, 100])
  })
})
