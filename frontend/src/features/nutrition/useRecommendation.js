import { useEffect, useMemo, useState } from 'react'

import { nutritionApi } from './api'

/** 날짜별 개인 권장 섭취량. 그 날짜에 적용 중이던 목표·활동량과 신체 기록으로 계산한다. */
export default function useRecommendation(call, date) {
  const api = useMemo(() => nutritionApi(call), [call])
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api
      .recommendation(date, controller.signal)
      .then(target => {
        if (!controller.signal.aborted) {
          setData({ date, target })
          setError('')
        }
      })
      .catch(failure => {
        if (!controller.signal.aborted) setError(failure.message)
      })
    return () => controller.abort()
  }, [api, date, retry])

  return { target: data?.date === date ? data.target : null, error, retry: () => setRetry(value => value + 1) }
}
