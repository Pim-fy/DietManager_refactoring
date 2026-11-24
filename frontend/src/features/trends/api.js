export const statisticsApi = call => ({
  range: (start, end, signal) => call(`/statistics?${new URLSearchParams({ start, end })}`, { signal }),
})
