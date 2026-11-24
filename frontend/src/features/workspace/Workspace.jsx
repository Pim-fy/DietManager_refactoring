import { useCallback, useState } from 'react'

import { request } from '../../shared/api/client'
import AccountWorkspace from '../account/AccountWorkspace'
import IntakePage from '../intakes/IntakePage'
import MeasurementWorkspace from '../measurements/MeasurementWorkspace'
import MealPlanPage from '../plans/MealPlanPage'
import TrendsPage from '../trends/TrendsPage'

const MENU = [
  ['intakes', '섭취 기록'],
  ['measurements', '신체 기록'],
  ['trends', '기록 추이'],
  ['plans', '식단 계획'],
  ['account', '내 정보'],
]

/** 로그인 후 화면. 메뉴와 인증된 API 호출(call)을 각 화면에 제공한다. */
export default function Workspace({ token, onLogout }) {
  const [page, setPage] = useState('measurements')

  const call = useCallback(
    async (path, options) => {
      try {
        return await request(path, { ...options, token })
      } catch (failure) {
        if (failure.status === 401) onLogout('로그인이 만료되었습니다. 다시 로그인해주세요.')
        throw failure
      }
    },
    [token, onLogout],
  )

  return (
    <>
      <nav aria-label="주 메뉴">
        <button className="brand" onClick={() => setPage('measurements')}>
          DietManager
        </button>
        <div className="nav-links">
          {MENU.map(([key, label]) => (
            <button key={key} aria-current={page === key ? 'page' : undefined} onClick={() => setPage(key)}>
              {label}
            </button>
          ))}
        </div>
        <button className="logout" onClick={() => onLogout()}>
          로그아웃
        </button>
      </nav>
      <main className="workspace">
        {page === 'intakes' ? <IntakePage call={call} /> : null}
        {page === 'plans' ? <MealPlanPage call={call} /> : null}
        {page === 'trends' ? <TrendsPage call={call} /> : null}
        {page === 'measurements' ? <MeasurementWorkspace call={call} /> : null}
        {page === 'account' ? (
          <AccountWorkspace call={call} onPassword={() => onLogout('비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.')} />
        ) : null}
      </main>
    </>
  )
}
