import { useCallback } from 'react'

import { request } from '../../shared/api/client'
import AppLink from '../../shared/components/AppLink'
import usePathRoute from '../../shared/hooks/usePathRoute'
import AccountWorkspace from '../account/AccountWorkspace'
import DashboardPage from '../dashboard/DashboardPage'
import IntakePage from '../intakes/IntakePage'
import MeasurementWorkspace from '../measurements/MeasurementWorkspace'
import MealPlanPage from '../plans/MealPlanPage'
import TrendsPage from '../trends/TrendsPage'

// 메뉴 아이콘: 24×24 선 아이콘(장식용, 글자와 함께 표시)
const ICONS = {
  dashboard: 'M4 4h7v7H4zM13 4h7v4h-7zM13 10h7v10h-7zM4 13h7v7H4z',
  intakes: 'M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10M16 21V3c-2 1.5-3 4-3 7v4h3',
  plans: 'M4 6h16v14H4zM4 10h16M8 3v5M16 3v5',
  measurements: 'M5 4h14l-1.5 16h-11zM9 9a3 3 0 0 1 6 0M12 9l1.5-2',
  trends: 'M4 4v16h16M7 15l4-4 3 3 5-6',
  account: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c1-4 4-6 8-6s7 2 8 6',
}
const MENU = [
  ['dashboard', '대시보드'],
  ['intakes', '섭취 기록'],
  ['plans', '식단 계획'],
  ['measurements', '신체 기록'],
  ['trends', '기록 추이'],
  ['account', '내 정보'],
]
const ROUTES = MENU.map(([key]) => key)

function MenuIcon({ name }) {
  return (
    <svg className="menu-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={ICONS[name]} />
    </svg>
  )
}

/** 로그인 후 화면. 메뉴와 인증된 API 호출(call)을 각 화면에 제공한다. */
export default function Workspace({ token, onLogout }) {
  const page = usePathRoute(ROUTES, 'dashboard')

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
    <div className="shell">
      {/* 넓은 화면: 왼쪽 사이드바. 좁은 화면: 위쪽 바(브랜드·로그아웃) + 아래 고정 탭 바(메뉴). 메뉴는 하나만 두고 배치만 바꾼다. */}
      <header className="shell-header">
        <AppLink className="brand" to="/dashboard">
          DietManager
        </AppLink>
        <nav aria-label="주 메뉴" className="shell-nav">
          {MENU.map(([key, label]) => (
            <AppLink key={key} to={`/${key}`} aria-current={page === key ? 'page' : undefined}>
              <MenuIcon name={key} />
              <span>{label}</span>
            </AppLink>
          ))}
        </nav>
        <button className="logout" onClick={() => onLogout()}>
          로그아웃
        </button>
      </header>
      <main className="workspace">
        {page === 'dashboard' ? <DashboardPage call={call} /> : null}
        {page === 'intakes' ? <IntakePage call={call} /> : null}
        {page === 'plans' ? <MealPlanPage call={call} /> : null}
        {page === 'trends' ? <TrendsPage call={call} /> : null}
        {page === 'measurements' ? <MeasurementWorkspace call={call} /> : null}
        {page === 'account' ? (
          <AccountWorkspace call={call} onPassword={() => onLogout('비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.')} />
        ) : null}
      </main>
    </div>
  )
}
