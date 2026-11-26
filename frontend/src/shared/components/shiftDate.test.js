import { describe, expect, it } from 'vitest'

import { shift, shiftMonth } from './shiftDate'

describe('shift', () => {
  it('하루 앞뒤로 이동', () => {
    expect(shift('2026-11-25', 1)).toBe('2026-11-26')
    expect(shift('2026-11-25', -1)).toBe('2026-11-24')
  })

  it('월말·월초를 넘긴다', () => {
    expect(shift('2026-11-30', 1)).toBe('2026-12-01')
    expect(shift('2026-12-01', -1)).toBe('2026-11-30')
    expect(shift('2026-03-01', -1)).toBe('2026-02-28')
  })

  it('연말·연초를 넘긴다', () => {
    expect(shift('2026-12-31', 1)).toBe('2027-01-01')
    expect(shift('2027-01-01', -1)).toBe('2026-12-31')
  })

  it('윤년 2월', () => {
    expect(shift('2028-02-28', 1)).toBe('2028-02-29')
    expect(shift('2028-02-29', 1)).toBe('2028-03-01')
    expect(shift('2028-03-01', -1)).toBe('2028-02-29')
    expect(shift('2027-02-28', 1)).toBe('2027-03-01')
  })

  it('0일 이동은 그대로', () => {
    expect(shift('2026-11-25', 0)).toBe('2026-11-25')
  })
})

describe('shiftMonth', () => {
  it('한 달 앞뒤로 이동', () => {
    expect(shiftMonth('2026-11-25', 1)).toBe('2026-12-25')
    expect(shiftMonth('2026-11-25', -1)).toBe('2026-10-25')
  })

  it('연말·연초를 넘긴다', () => {
    expect(shiftMonth('2026-12-15', 1)).toBe('2027-01-15')
    expect(shiftMonth('2027-01-15', -1)).toBe('2026-12-15')
  })

  it('도착한 달에 같은 날짜가 없으면 말일로 맞춘다', () => {
    expect(shiftMonth('2026-01-31', 1)).toBe('2026-02-28')
    expect(shiftMonth('2028-01-31', 1)).toBe('2028-02-29')
    expect(shiftMonth('2026-03-31', -1)).toBe('2026-02-28')
    expect(shiftMonth('2026-05-31', 1)).toBe('2026-06-30')
  })

  it('0달 이동은 그대로', () => {
    expect(shiftMonth('2026-11-25', 0)).toBe('2026-11-25')
  })
})
