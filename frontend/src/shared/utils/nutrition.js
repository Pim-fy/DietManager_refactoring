export const meals = { BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식' }

/** [필드명, 표시 이름, 단위] */
export const nutrients = [
  ['calories', '칼로리', 'kcal'],
  ['carbohydrate', '탄수화물', 'g'],
  ['protein', '단백질', 'g'],
  ['fat', '지방', 'g'],
]

export function formatNutrition(value) {
  return value == null ? '정보 없음' : Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

/** 식약처 식품명은 단어 사이가 _로 들어온다. */
export function foodLabel(name) {
  return name.replaceAll('_', ' ')
}
