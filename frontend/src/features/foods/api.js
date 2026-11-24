export const foodApi = call => ({
  search: (params, signal) => call(`/foods?${params}`, { signal }),
  usage: signal => call('/foods/usage', { signal }),
  favorites: signal => call('/foods/favorites', { signal }),
  recent: signal => call('/foods/recent', { signal }),
  favorite: (foodCode, favorite) => call('/foods/favorites', { method: 'PUT', body: { foodCode, favorite } }),
})
