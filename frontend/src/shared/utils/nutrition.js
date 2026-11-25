export const meals = { BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식' }

/** [필드명, 표시 이름, 단위] */
export const nutrients = [
  ['calories', '칼로리', 'kcal'],
  ['carbohydrate', '탄수화물', 'g'],
  ['protein', '단백질', 'g'],
  ['fat', '지방', 'g'],
]

/**
 * 1일 영양성분 기준치 (식품 표시 기준의 고정값, 원본 프로젝트와 같은 값).
 * 개인별 권장량과 다르다. [표시 이름, 기준치, 단위]
 */
export const DAILY_REFERENCE_VALUES = [
  ['탄수화물', 324, 'g'],
  ['당류', 100, 'g'],
  ['식이섬유', 25, 'g'],
  ['단백질', 55, 'g'],
  ['지방', 54, 'g'],
  ['포화지방', 15, 'g'],
  ['콜레스테롤', 300, 'mg'],
  ['나트륨', 2000, 'mg'],
]

export function formatNutrition(value) {
  return value == null ? '정보 없음' : Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
}

/** 식약처 식품명은 단어 사이가 _로 들어온다. */
export function foodLabel(name) {
  return name.replaceAll('_', ' ')
}
