import { useCallback, useMemo, useRef, useSyncExternalStore } from 'react'

import { applyParams } from '../utils/dateParam'
import { NAVIGATE_EVENT } from './usePathRoute'

function subscribe(callback) {
  window.addEventListener('popstate', callback)
  window.addEventListener(NAVIGATE_EVENT, callback)
  return () => {
    window.removeEventListener('popstate', callback)
    window.removeEventListener(NAVIGATE_EVENT, callback)
  }
}

const getSearch = () => window.location.search

/**
 * 주소의 쿼리(?a=1)를 읽고 쓴다. [params(URLSearchParams), replace(updates)]를 돌려준다.
 * replace는 방문 기록을 쌓지 않고(replaceState) 쿼리만 바꾸며, 값이 null인 키는 지운다.
 * history.state는 그대로 두므로 열려 있는 창(Dialog)의 기록 단계가 깨지지 않는다.
 * 훅을 쓰는 화면이 열릴 때의 경로에서만 쓰며, 이미 다른 경로로 옮겨 갔다면 주소를 건드리지 않는다.
 */
export default function useQueryParams() {
  const search = useSyncExternalStore(subscribe, getSearch)
  const pathname = useRef(window.location.pathname)
  const params = useMemo(() => new URLSearchParams(search), [search])

  const replace = useCallback(updates => {
    if (window.location.pathname !== pathname.current) return
    const next = applyParams(window.location.search, updates)
    if (next === window.location.search) return
    window.history.replaceState(window.history.state, '', `${window.location.pathname}${next}`)
    window.dispatchEvent(new Event(NAVIGATE_EVENT))
  }, [])

  return [params, replace]
}
