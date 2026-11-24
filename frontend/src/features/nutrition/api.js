export const nutritionApi = call => ({
  preferences: signal => call('/nutrition/preferences', { signal }),
  recommendation: (date, signal) => call(`/nutrition/recommendation?${new URLSearchParams({ date })}`, { signal }),
  save: activityLevel => call('/nutrition/preferences', { method: 'PUT', body: { activityLevel } }),
})
