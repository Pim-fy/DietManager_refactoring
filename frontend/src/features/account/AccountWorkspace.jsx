import { useCallback, useMemo } from 'react'

import ActivityLevelSection from '../nutrition/ActivityLevelSection'
import ResourceState from '../../shared/components/ResourceState'
import useResource from '../../shared/hooks/useResource'
import AccountPage from './AccountPage'
import { accountApi } from './api'

/** 계정·프로필을 불러와 내 정보 화면에 넘긴다. */
export default function AccountWorkspace({ call, onPassword }) {
  const api = useMemo(() => accountApi(call), [call])
  const load = useCallback(
    async signal => {
      const [account, profile] = await Promise.all([api.account(signal), api.profile(signal)])
      return { account, profile }
    },
    [api],
  )
  const resource = useResource(load)

  return (
    <ResourceState resource={resource} loadingText="내 정보를 불러오는 중…">
      {resource.data && (
        <AccountPage
          {...resource.data}
          api={api}
          onAccount={account => resource.setData(previous => ({ ...previous, account }))}
          onProfile={profile => resource.setData(previous => ({ ...previous, profile }))}
          onPassword={onPassword}
        >
          <ActivityLevelSection call={call} />
        </AccountPage>
      )}
    </ResourceState>
  )
}
