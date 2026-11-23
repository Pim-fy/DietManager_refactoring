export const intakeApi = call => ({
  daily: (date, signal) => call(`/intakes?${new URLSearchParams({ date })}`, { signal }),
  save: (id, body) => call(id ? `/intakes/${id}` : '/intakes', { method: id ? 'PATCH' : 'POST', body }),
  remove: id => call(`/intakes/${id}`, { method: 'DELETE' }),
  restore: id => call(`/intakes/${id}/restore`, { method: 'POST' }),
  nutrition: (id, body) => call(id ? `/intakes/${id}/nutrition` : '/intakes/manual', { method: id ? 'PATCH' : 'POST', body }),
})
export const planApi = call => ({
  daily: (date, signal) => call(`/meal-plans?${new URLSearchParams({ date })}`, { signal }),
  save: (id, body) => call(id ? `/meal-plans/${id}` : '/meal-plans', { method: id ? 'PATCH' : 'POST', body }),
  command: (id, command) => call(`/meal-plans/${id}${command === 'delete' ? '' : `/${command}`}`, { method: command === 'delete' ? 'DELETE' : 'POST' }),
})
export const measurementApi = call => ({
  deleted: signal => call('/body-measurements/deleted', { signal }),
  save: (id, body) => call(id ? `/body-measurements/${id}` : '/body-measurements', { method: id ? 'PATCH' : 'POST', body }),
  command: (id, command) => call(`/body-measurements/${id}${command === 'delete' ? '' : '/restore'}`, { method: command === 'delete' ? 'DELETE' : 'POST' }),
})

export const foodApi = call => ({
  search: (params, signal) => call(`/foods?${params}`, { signal }),
  usage: signal => call('/foods/usage', { signal }),
  favorites: signal => call('/foods/favorites', { signal }),
  recent: signal => call('/foods/recent', { signal }),
  favorite: (foodCode, favorite) => call('/foods/favorites', { method: 'PUT', body: { foodCode, favorite } }),
})
export const nutritionApi = call => ({
  preferences: signal => call('/nutrition/preferences', { signal }),
  recommendation: (date, signal) => call(`/nutrition/recommendation?${new URLSearchParams({ date })}`, { signal }),
  save: activityLevel => call('/nutrition/preferences', { method: 'PUT', body: { activityLevel } }),
})
export const statisticsApi = call => ({
  range: (start, end, signal) => call(`/statistics?${new URLSearchParams({ start, end })}`, { signal }),
})
export const userApi = call => ({
  account: signal => call('/account', { signal }),
  profile: signal => call('/profile', { signal }),
  measurements: signal => call('/body-measurements', { signal }),
  latest: signal => call('/body-measurements/latest', { signal }),
  update: (section, body) => call(`/${section}`, { method: 'PATCH', body }),
})
