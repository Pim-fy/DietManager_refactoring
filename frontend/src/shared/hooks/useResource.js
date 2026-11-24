import { useCallback, useEffect, useRef, useState } from 'react'

/** load(signal)로 데이터를 불러오고, 다시 시도와 늦게 도착한 응답 무시를 처리한다. */
export default function useResource(load) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const generation = useRef(0)
  const lifecycle = useRef({ active: false })

  const refresh = useCallback(
    async signal => {
      const current = ++generation.current
      const result = await load(signal)
      if (lifecycle.current.active && current === generation.current && !signal?.aborted) {
        setData(result)
        setError('')
      }
      return result
    },
    [load],
  )

  useEffect(() => {
    const state = lifecycle.current
    state.active = true
    const controller = new AbortController()
    refresh(controller.signal).catch(failure => {
      if (!controller.signal.aborted) setError(failure.message)
    })
    return () => {
      state.active = false
      controller.abort()
    }
  }, [refresh, retry])

  return { data, setData, error, refresh, retry: () => setRetry(value => value + 1) }
}
