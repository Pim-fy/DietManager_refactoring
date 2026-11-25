import { describe, expect, it } from 'vitest'

import { dates } from './range'

const week = anchor => dates(anchor, 'week')
const month = anchor => dates(anchor, 'month')

describe('dates - 주간(월~일)', () => {
  it('월요일이 기준이면 그날부터 시작한다', () => {
    expect(week('2026-11-23')).toEqual({ start: '2026-11-23', end: '2026-11-29' })
  })

  it('일요일이 기준이면 그 주 월요일부터 그날까지다', () => {
    expect(week('2026-11-29')).toEqual({ start: '2026-11-23', end: '2026-11-29' })
  })

  it('주중 날짜도 같은 주를 가리킨다', () => {
    expect(week('2026-11-25')).toEqual({ start: '2026-11-23', end: '2026-11-29' })
  })

  it('연말 주는 다음 해로 넘어간다', () => {
    expect(week('2026-12-31')).toEqual({ start: '2026-12-28', end: '2027-01-03' })
  })

  it('연초 주는 지난해에서 시작한다', () => {
    expect(week('2027-01-01')).toEqual({ start: '2026-12-28', end: '2027-01-03' })
  })

  it('월말을 넘기는 주', () => {
    expect(week('2026-11-30')).toEqual({ start: '2026-11-30', end: '2026-12-06' })
  })
})

describe('dates - 월간', () => {
  it('31일인 달', () => {
    expect(month('2026-01-15')).toEqual({ start: '2026-01-01', end: '2026-01-31' })
  })

  it('30일인 달', () => {
    expect(month('2026-11-30')).toEqual({ start: '2026-11-01', end: '2026-11-30' })
  })

  it('윤년 2월은 29일까지', () => {
    expect(month('2028-02-10')).toEqual({ start: '2028-02-01', end: '2028-02-29' })
  })

  it('평년 2월은 28일까지', () => {
    expect(month('2027-02-10')).toEqual({ start: '2027-02-01', end: '2027-02-28' })
  })

  it('31일 기준에서 30일인 달로 넘어가지 않는다', () => {
    expect(month('2026-10-31')).toEqual({ start: '2026-10-01', end: '2026-10-31' })
  })

  it('12월', () => {
    expect(month('2026-12-31')).toEqual({ start: '2026-12-01', end: '2026-12-31' })
  })
})
