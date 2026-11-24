import { useCallback, useMemo } from 'react'

import ResourceState from '../../shared/components/ResourceState'
import useResource from '../../shared/hooks/useResource'
import { measurementApi } from './api'
import MeasurementPage from './MeasurementPage'

/** 신체 기록 목록과 최신 기록을 불러와 신체 기록 화면에 넘긴다. */
export default function MeasurementWorkspace({ call }) {
  const api = useMemo(() => measurementApi(call), [call])
  const load = useCallback(
    async signal => {
      const [measurements, latest] = await Promise.all([
        api.list(signal),
        api.latest(signal).catch(failure => {
          // 기록이 하나도 없으면 최신 기록 조회는 404다.
          if (failure.status === 404 && failure.code === 'BODY_MEASUREMENT_NOT_FOUND') return null
          throw failure
        }),
      ])
      return { measurements, latest }
    },
    [api],
  )
  const resource = useResource(load)

  return (
    <ResourceState resource={resource}>
      {resource.data && <MeasurementPage {...resource.data} api={api} onSaved={resource.refresh} />}
    </ResourceState>
  )
}
