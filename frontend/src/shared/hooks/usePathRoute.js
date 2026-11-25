import { useEffect, useState } from 'react'

/** 주소가 바뀌었음을 알리는 이벤트. popstate는 방문 기록 이동에만 오므로, 코드가 바꾼 주소는 이 이벤트로 알린다. */
export const NAVIGATE_EVENT = 'app:navigate'

function read(routes, fallback) {
  const key = window.location.pathname.split('/')[1]
  return routes.includes(key) ? key : fallback
}

/** 주소를 path(쿼리 포함 가능)로 바꾸고 모든 usePathRoute 사용처에 알린다. 같은 주소면 아무것도 하지 않는다. */
export function navigate(path) {
  if (path === `${window.location.pathname}${window.location.search}`) return
  window.history.pushState(null, '', path)
  window.dispatchEvent(new Event(NAVIGATE_EVENT))
  window.scrollTo(0, 0)
}

/** 주소의 첫 경로(/<화면>)를 현재 화면으로 쓴다. 알 수 없는 주소는 fallback 화면으로 보이고 주소도 맞춰 바꾼다. */
export default function usePathRoute(routes, fallback) {
  const [route, setRoute] = useState(() => read(routes, fallback))

  useEffect(() => {
    const update = () => setRoute(read(routes, fallback))
    window.addEventListener('popstate', update)
    window.addEventListener(NAVIGATE_EVENT, update)
    return () => {
      window.removeEventListener('popstate', update)
      window.removeEventListener(NAVIGATE_EVENT, update)
    }
  }, [routes, fallback])

  useEffect(() => {
    if (window.location.pathname !== `/${route}`) {
      window.history.replaceState(window.history.state, '', `/${route}${window.location.search}`)
    }
  }, [route])

  return route
}
