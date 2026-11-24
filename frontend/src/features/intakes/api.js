export const intakeApi = call => ({
  daily: (date, signal) => call(`/intakes?${new URLSearchParams({ date })}`, { signal }),
  save: (id, body) => call(id ? `/intakes/${id}` : '/intakes', { method: id ? 'PATCH' : 'POST', body }),
  remove: id => call(`/intakes/${id}`, { method: 'DELETE' }),
  restore: id => call(`/intakes/${id}/restore`, { method: 'POST' }),
  /** id가 있으면 기록의 영양정보 수정, 없으면 직접 입력 생성 */
  nutrition: (id, body) => call(id ? `/intakes/${id}/nutrition` : '/intakes/manual', { method: id ? 'PATCH' : 'POST', body }),
})
