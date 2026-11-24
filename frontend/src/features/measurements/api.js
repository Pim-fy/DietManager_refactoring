export const measurementApi = call => ({
  list: signal => call('/users/body-measurements', { signal }),
  latest: signal => call('/users/body-measurements/latest', { signal }),
  deleted: signal => call('/users/body-measurements/deleted', { signal }),
  save: (id, body) =>
    call(id ? `/users/body-measurements/${id}` : '/users/body-measurements', { method: id ? 'PATCH' : 'POST', body }),
  /** command: delete | restore */
  command: (id, command) =>
    call(`/users/body-measurements/${id}${command === 'delete' ? '' : '/restore'}`, { method: command === 'delete' ? 'DELETE' : 'POST' }),
})
