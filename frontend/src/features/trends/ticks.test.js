import { describe, expect, it } from 'vitest'

import { ticks } from './ticks'

function stepOf(values) {
  return Number((values[1] - values[0]).toFixed(6))
}

function isNice(step) {
  const mantissa = step / 10 ** Math.floor(Math.log10(step))
  return [1, 2, 5, 10].some(value => Math.abs(mantissa - value) < 1e-9)
}

describe('ticks', () => {
  it('범위가 0이면(min = max) 눈금은 그 값 하나뿐이다(차트는 항상 폭이 있는 범위로 호출)', () => {
    expect(ticks(0, 0)).toEqual([0])
    expect(ticks(12, 12)).toEqual([12])
  })

  it('0~100은 간격 50', () => {
    expect(ticks(0, 100)).toEqual([0, 50, 100])
  })

  it('0~2400은 간격 1000', () => {
    expect(ticks(0, 2400)).toEqual([0, 1000, 2000, 3000])
  })

  it('체중처럼 좁은 범위(59~62)는 간격 1', () => {
    expect(ticks(59, 62)).toEqual([59, 60, 61, 62])
  })

  it('아주 작은 범위는 소수 간격이고 유한한 값이다', () => {
    const values = ticks(0, 0.001)
    expect(values[0]).toBe(0)
    expect(values.at(-1)).toBeGreaterThanOrEqual(0.001)
    expect(values.every(Number.isFinite)).toBe(true)
  })

  it('아주 큰 값', () => {
    const values = ticks(0, 1e9)
    expect(values[0]).toBe(0)
    expect(values.at(-1)).toBeGreaterThanOrEqual(1e9)
    expect(isNice(stepOf(values))).toBe(true)
  })

  it('0에서 시작하면 음수 눈금이 없다', () => {
    expect(ticks(0, 17).every(value => value >= 0)).toBe(true)
  })

  it('음수 범위도 덮는다', () => {
    const values = ticks(-7, 3)
    expect(values[0]).toBeLessThanOrEqual(-7)
    expect(values.at(-1)).toBeGreaterThanOrEqual(3)
  })

  it.each([
    [0, 100],
    [0, 2400],
    [0, 3333],
    [58.4, 61.7],
    [0, 0.37],
    [0, 7],
    [3, 987654],
  ])('%s~%s: 범위를 덮고 간격이 1·2·5×10^n이며 일정하다', (min, max) => {
    const values = ticks(min, max)
    const step = stepOf(values)
    expect(values[0]).toBeLessThanOrEqual(min)
    expect(values.at(-1)).toBeGreaterThanOrEqual(max)
    expect(isNice(step)).toBe(true)
    values.slice(1).forEach((value, index) => expect(value - values[index]).toBeCloseTo(step, 5))
  })
})
