import { describe, expect, it } from 'vitest'

import { dateString } from './form'

describe('dateString', () => {
  it('월·일을 두 자리로 맞춘다', () => {
    expect(dateString(new Date(2026, 0, 5))).toBe('2026-01-05')
    expect(dateString(new Date(2026, 10, 25))).toBe('2026-11-25')
  })

  it('로컬 날짜 기준이다(자정 직전·직후)', () => {
    expect(dateString(new Date(2026, 11, 31, 23, 59, 59))).toBe('2026-12-31')
    expect(dateString(new Date(2027, 0, 1, 0, 0, 0))).toBe('2027-01-01')
  })

  it('윤일', () => {
    expect(dateString(new Date(2028, 1, 29))).toBe('2028-02-29')
  })
})
