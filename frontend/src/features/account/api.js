export const accountApi = call => ({
  account: signal => call('/users/account', { signal }),
  profile: signal => call('/users/profile', { signal }),
  /** section: account | profile | password */
  update: (section, body) => call(`/users/${section}`, { method: 'PATCH', body }),
})
