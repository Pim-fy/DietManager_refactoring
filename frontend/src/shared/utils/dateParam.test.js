import { describe, expect, it } from 'vitest'

import { applyParams, dateParamValue, isValidDate, periodParamValue, resolveDate, resolvePeriod } from './dateParam'

const now = '2026-11-25'

describe('isValidDate', () => {
  it('실제 있는 날짜만 통과한다', () => {
    expect(isValidDate('2026-11-25')).toBe(true)
    expect(isValidDate('2024-02-29')).toBe(true)
    expect(isValidDate('2026-02-30')).toBe(false)
    expect(isValidDate('2026-13-01')).toBe(false)
    expect(isValidDate('2026-1-1')).toBe(false)
    expect(isValidDate('abc')).toBe(false)
    expect(isValidDate('')).toBe(false)
    expect(isValidDate(null)).toBe(false)
  })
})

describe('resolveDate', () => {
  it('값이 없거나 틀리면 오늘이다', () => {
    expect(resolveDate(null, { now })).toBe(now)
    expect(resolveDate('', { now })).toBe(now)
    expect(resolveDate('2026-02-30', { now })).toBe(now)
  })

  it('지난 날짜는 그대로 쓴다', () => {
    expect(resolveDate('2026-11-01', { now })).toBe('2026-11-01')
  })

  it('미래 날짜는 기본적으로 오늘로 보정하고, future면 허용한다', () => {
    expect(resolveDate('2026-12-01', { now })).toBe(now)
    expect(resolveDate('2026-12-01', { now, future: true })).toBe('2026-12-01')
  })
})

describe('주소에 쓸 값', () => {
  it('오늘과 기본 기간은 생략한다', () => {
    expect(dateParamValue(now, now)).toBeNull()
    expect(dateParamValue('2026-11-01', now)).toBe('2026-11-01')
    expect(periodParamValue('week')).toBeNull()
    expect(periodParamValue('month')).toBe('month')
  })

  it('기간은 week/month 외에는 week다', () => {
    expect(resolvePeriod('month')).toBe('month')
    expect(resolvePeriod('year')).toBe('week')
    expect(resolvePeriod(null)).toBe('week')
  })
})

describe('applyParams', () => {
  it('값을 넣고 지우며 다른 키는 유지한다', () => {
    expect(applyParams('', { date: '2026-11-01' })).toBe('?date=2026-11-01')
    expect(applyParams('?period=month', { date: '2026-11-01' })).toBe('?period=month&date=2026-11-01')
    expect(applyParams('?period=month&date=2026-11-01', { date: null })).toBe('?period=month')
    expect(applyParams('?date=2026-11-01', { date: null })).toBe('')
  })
})
