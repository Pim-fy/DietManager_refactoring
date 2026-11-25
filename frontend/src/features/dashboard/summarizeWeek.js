/** 기록한 날, 그중 칼로리 정보가 있는 날, 그 날들의 평균 칼로리. 칼로리 정보가 있는 날이 없으면 평균은 NaN. */
export function summarizeWeek(days) {
  const recorded = days.filter(day => day.records > 0)
  const withCalories = recorded.filter(day => day.records > day.calories.missingCount)
  const average = withCalories.reduce((sum, day) => sum + day.calories.value, 0) / withCalories.length
  return { recorded, withCalories, average }
}
