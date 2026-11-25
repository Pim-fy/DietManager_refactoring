import { useCallback, useEffect } from 'react'

import { dateParamValue, resolveDate } from '../utils/dateParam'
import useQueryParams from './useQueryParams'

/**
 * 주소의 ?date=YYYY-MM-DD를 화면의 조회 날짜로 쓴다. [date, setDate]를 돌려준다.
 * 값이 없거나 틀리면 오늘(future가 아니면 미래도 오늘)로 열고, 주소도 바로잡는다. 오늘이면 주소에서 생략한다.
 */
export default function useDateParam({ future = false } = {}) {
  const [params, replace] = useQueryParams()
  const raw = params.get('date')
  const date = resolveDate(raw, { future })
  const canonical = dateParamValue(date)

  useEffect(() => {
    if (raw !== canonical) replace({ date: canonical })
  }, [raw, canonical, replace])

  const setDate = useCallback(
    value => replace({ date: dateParamValue(resolveDate(value, { future })) }),
    [replace, future],
  )
  return [date, setDate]
}
