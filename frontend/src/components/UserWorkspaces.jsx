import { userApi } from '../features/api'
import { useCallback, useMemo } from 'react'
import useResource from '../hooks/useResource'
import AccountPage from './AccountPage'
import MeasurementPage from './MeasurementPage'
function ResourceState({ resource, children }) {
  return resource.error ? <div className="panel" role="alert"><p>{resource.error}</p><button className="primary" onClick={resource.retry}>다시 불러오기</button></div>
    : !resource.data ? <p role="status">내 정보를 불러오는 중…</p> : children
}
export function AccountWorkspace({ call, onPassword }) {
  const api = useMemo(() => userApi(call), [call])
  const load = useCallback(async signal => {
    const [account, profile] = await Promise.all([api.account(signal), api.profile(signal)])
    return { account, profile }
  }, [api])
  const resource = useResource(load)
  return <ResourceState resource={resource}>{resource.data && <AccountPage {...resource.data} call={call}
    onAccount={account => resource.setData(previous => ({ ...previous, account }))}
    onProfile={profile => resource.setData(previous => ({ ...previous, profile }))} onPassword={onPassword} />}</ResourceState>
}
export function MeasurementWorkspace({ call }) {
  const api = useMemo(() => userApi(call), [call])
  const load = useCallback(async signal => {
    const [measurements, latest] = await Promise.all([api.measurements(signal), api.latest(signal).catch(failure => {
      if (failure.status === 404 && failure.code === 'BODY_MEASUREMENT_NOT_FOUND') return null
      throw failure
    })])
    return { measurements, latest }
  }, [api])
  const resource = useResource(load)
  return <ResourceState resource={resource}>{resource.data && <MeasurementPage {...resource.data} call={call} onSaved={resource.refresh} />}</ResourceState>
}
