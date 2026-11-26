import { describe, expect, it } from 'vitest'

import { foodLabel, formatNutrition } from './nutrition'

describe('formatNutrition', () => {
  it('null·undefined는 정보 없음', () => {
    expect(formatNutrition(null)).toBe('정보 없음')
    expect(formatNutrition(undefined)).toBe('정보 없음')
  })

  it('0은 정보 없음이 아니라 0', () => {
    expect(formatNutrition(0)).toBe('0')
  })

  it('기본은 소수 둘째 자리까지 반올림', () => {
    expect(formatNutrition(12.3456)).toBe('12.35')
    expect(formatNutrition(12.5)).toBe('12.5')
  })

  it('digits로 자릿수를 지정한다', () => {
    expect(formatNutrition(1234.56, 0)).toBe('1,235')
    expect(formatNutrition(12.34, 1)).toBe('12.3')
  })

  it('천 단위 구분자를 넣는다', () => {
    expect(formatNutrition(1234567)).toBe('1,234,567')
  })

  it('숫자 문자열도 받는다', () => {
    expect(formatNutrition('12.5')).toBe('12.5')
  })
})

describe('foodLabel', () => {
  it('_를 공백으로 바꾼다', () => {
    expect(foodLabel('닭가슴살_샐러드_소스포함')).toBe('닭가슴살 샐러드 소스포함')
  })

  it('_가 없으면 그대로', () => {
    expect(foodLabel('김치찌개')).toBe('김치찌개')
  })
})
