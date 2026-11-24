export const planApi = call => ({
  daily: (date, signal) => call(`/meal-plans?${new URLSearchParams({ date })}`, { signal }),
  save: (id, body) => call(id ? `/meal-plans/${id}` : '/meal-plans', { method: id ? 'PATCH' : 'POST', body }),
  /** command: delete | restore | consume */
  command: (id, command) =>
    call(`/meal-plans/${id}${command === 'delete' ? '' : `/${command}`}`, { method: command === 'delete' ? 'DELETE' : 'POST' }),
})
