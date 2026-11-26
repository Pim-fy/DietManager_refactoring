/** 최소~최대를 덮는 보기 좋은 눈금 간격(1·2·5 단위). */
export function ticks(min, max, count = 4) {
  const raw = (max - min) / count || 1
  const power = 10 ** Math.floor(Math.log10(raw))
  const step = [1, 2, 5, 10].map(value => value * power).find(value => value >= raw)
  const start = Math.floor(min / step) * step
  const end = Math.ceil(max / step) * step
  const values = []
  for (let value = start; value <= end + step / 2; value += step) values.push(Number(value.toFixed(6)))
  return values
}
