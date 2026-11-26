import { useCallback, useEffect } from 'react'

import useQueryParams from '../../shared/hooks/useQueryParams'
import { dateParamValue, periodParamValue, resolveDate, resolvePeriod } from '../../shared/utils/dateParam'

/**
 * 기록 추이의 기간과 기준 날짜를 주소(?period=month&date=YYYY-MM-DD)에서 읽고 쓴다.
 * 없거나 틀리면 week와 오늘로 열고 주소도 바로잡는다. 기본값(week, 오늘)은 주소에서 생략한다.
 */
export default function useTrendsQuery() {
  const [params, replace] = useQueryParams()
  const rawPeriod = params.get('period')
  const rawDate = params.get('date')
  const period = resolvePeriod(rawPeriod)
  const anchor = resolveDate(rawDate, { future: true })
  const canonicalPeriod = periodParamValue(period)
  const canonicalDate = dateParamValue(anchor)

  useEffect(() => {
    if (rawPeriod !== canonicalPeriod || rawDate !== canonicalDate) replace({ period: canonicalPeriod, date: canonicalDate })
  }, [rawPeriod, rawDate, canonicalPeriod, canonicalDate, replace])

  const setPeriod = useCallback(value => replace({ period: periodParamValue(resolvePeriod(value)) }), [replace])
  const setAnchor = useCallback(value => replace({ date: dateParamValue(resolveDate(value, { future: true })) }), [replace])
  return { period, anchor, setPeriod, setAnchor }
}
