import { useCallback, useState } from 'react'
import { request } from '../api'
import { AccountWorkspace, MeasurementWorkspace } from './UserWorkspaces'
import IntakePage from './IntakePage'
import MealPlanPage from './MealPlanPage'
import TrendsPage from './TrendsPage'

export default function Workspace({ token, onLogout }) {
  const [page, setPage] = useState('measurements')
  const foodCall = useCallback(async (path, options) => {
    try { return await request(path, { ...options, token, base: '/api' }) }
    catch (failure) {
      if (failure.status === 401) onLogout('로그인이 만료되었습니다. 다시 로그인해주세요.')
      throw failure
    }
  }, [token, onLogout])
  const call = useCallback(async (path, options) => {
    try { return await request(path, { ...options, token }) }
    catch (failure) {
      if (failure.status === 401) onLogout('로그인이 만료되었습니다. 다시 로그인해주세요.')
      throw failure
    }
  }, [token, onLogout])
  return <>
    <nav aria-label="주 메뉴"><button className="brand" onClick={() => setPage('measurements')}>DietManager</button>
      <div className="nav-links"><button aria-current={page === 'intakes' ? 'page' : undefined} onClick={() => setPage('intakes')}>섭취 기록</button>
        <button aria-current={page === 'measurements' ? 'page' : undefined} onClick={() => setPage('measurements')}>신체 기록</button>
        <button aria-current={page === 'trends' ? 'page' : undefined} onClick={() => setPage('trends')}>기록 추이</button>
        <button aria-current={page === 'plans' ? 'page' : undefined} onClick={() => setPage('plans')}>식단 계획</button>
        <button aria-current={page === 'account' ? 'page' : undefined} onClick={() => setPage('account')}>내 정보</button></div>
      <button className="logout" onClick={() => onLogout()}>로그아웃</button></nav>
    <main className="workspace">
      {page === 'intakes' ? <IntakePage call={foodCall} />
        : page === 'plans' ? <MealPlanPage call={foodCall} />
        : page === 'trends' ? <TrendsPage call={foodCall} />
        : page === 'measurements' ? <MeasurementWorkspace call={call} />
        : <AccountWorkspace call={call} onPassword={() => onLogout('비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.')} />}

    </main>
  </>
}
