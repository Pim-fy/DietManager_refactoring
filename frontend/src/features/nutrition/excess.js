import { nutrients } from '../../shared/utils/nutrition'

/** 권장량을 계산할 수 없을 때 쓰는 1일 영양성분 기준치(칼로리는 기준치가 없어 제외). */
const REFERENCE_LIMITS = { carbohydrate: 324, protein: 55, fat: 54 }

/**
 * 하루 섭취 합계가 기준을 넘은 영양소 목록.
 * 개인 권장량을 계산할 수 있으면 권장량을, 아니면 1일 영양성분 기준치를 기준으로 쓴다.
 * @returns {{ basis: string, items: { key, label, unit, limit, consumed, over, percent }[] }}
 */
export function findExcess(daily, target) {
  const personal = Boolean(target?.available)
  const basis = personal ? '권장량' : '1일 기준치'
  const items = []
  for (const [key, label, unit] of nutrients) {
    const limit = personal ? target[key] : REFERENCE_LIMITS[key]
    const consumed = daily[key]?.value
    if (limit == null || consumed == null || consumed <= limit) continue
    items.push({ key, label, unit, limit, consumed, over: consumed - limit, percent: (consumed / limit) * 100 })
  }
  return { basis, items }
}
