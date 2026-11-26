import { describe, expect, it } from 'vitest'

import { findExcess } from './excess'

const target = { available: true, calories: 2000, carbohydrate: 300, protein: 60, fat: 50 }
const daily = (calories, carbohydrate, protein, fat) => ({
  calories: { value: calories },
  carbohydrate: { value: carbohydrate },
  protein: { value: protein },
  fat: { value: fat },
})

describe('findExcess', () => {
  it('권장량이 있으면 권장량을 기준으로 한다', () => {
    const result = findExcess(daily(2500, 100, 30, 20), target)
    expect(result.basis).toBe('권장량')
    expect(result.items).toEqual([
      { key: 'calories', label: '칼로리', unit: 'kcal', limit: 2000, consumed: 2500, over: 500, percent: 125 },
    ])
  })

  it('권장량이 없으면 1일 기준치를 쓰고 칼로리는 제외한다', () => {
    const result = findExcess(daily(99999, 400, 56, 54), null)
    expect(result.basis).toBe('1일 기준치')
    expect(result.items.map(item => item.key)).toEqual(['carbohydrate', 'protein'])
    expect(result.items[0]).toMatchObject({ limit: 324, consumed: 400, over: 76 })
  })

  it('available가 false면 기준치를 쓴다', () => {
    const result = findExcess(daily(0, 325, 0, 0), { available: false, carbohydrate: 1 })
    expect(result.basis).toBe('1일 기준치')
    expect(result.items.map(item => item.key)).toEqual(['carbohydrate'])
  })

  it('기준과 정확히 같으면 초과가 아니다', () => {
    expect(findExcess(daily(2000, 300, 60, 50), target).items).toEqual([])
    expect(findExcess(daily(0, 324, 55, 54), null).items).toEqual([])
  })

  it('값이 null이면 건너뛴다', () => {
    const result = findExcess({ calories: { value: null }, carbohydrate: { value: null }, protein: {}, fat: undefined }, target)
    expect(result.items).toEqual([])
  })

  it('권장량 항목이 null이면 그 영양소는 건너뛴다', () => {
    const result = findExcess(daily(9999, 999, 999, 999), { available: true, calories: null, carbohydrate: 300, protein: null, fat: null })
    expect(result.items.map(item => item.key)).toEqual(['carbohydrate'])
  })

  it('여러 항목이 초과하면 모두 담고 퍼센트와 초과량을 계산한다', () => {
    const result = findExcess(daily(3000, 450, 90, 100), target)
    expect(result.items.map(item => item.key)).toEqual(['calories', 'carbohydrate', 'protein', 'fat'])
    expect(result.items.map(item => item.over)).toEqual([1000, 150, 30, 50])
    expect(result.items.map(item => item.percent)).toEqual([150, 150, 150, 200])
  })
})
