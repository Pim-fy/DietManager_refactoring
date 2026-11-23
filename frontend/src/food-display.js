export const meals = { BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식' }
export const nutrients = [['calories', '칼로리', 'kcal'], ['carbohydrate', '탄수화물', 'g'], ['protein', '단백질', 'g'], ['fat', '지방', 'g']]
export const formatNutrition = value => value == null ? '정보 없음' : Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })
