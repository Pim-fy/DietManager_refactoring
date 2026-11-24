import { useEffect, useState } from 'react'

import { formValues } from '../../shared/utils/form'

export default function useMeasurements(api, onSaved) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  const [editing, setEditing] = useState(null)
  const [deleted, setDeleted] = useState([])
  const [retry, setRetry] = useState(0)
  const [page, setPage] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api
      .deleted(controller.signal)
      .then(value => {
        if (!controller.signal.aborted) setDeleted(value)
      })
      .catch(error => {
        if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
      })
    return () => controller.abort()
  }, [api, retry])

  async function mutate(action, text) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await action()
      setEditing(null)
      setPage(0)
      try {
        await onSaved()
        setRetry(value => value + 1)
        setMessage({ text })
      } catch {
        setMessage({ text: '변경사항은 저장했습니다. 목록 새로고침이 필요합니다.', error: true })
      }
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  function submit(event) {
    event.preventDefault()
    const values = formValues(event)
    const body = { ...values, height: Number(values.height), weight: Number(values.weight) }
    mutate(() => api.save(editing?.measurementId, body), '신체 기록을 저장했습니다.')
  }

  function command(id, action, text) {
    return mutate(() => api.command(id, action), text)
  }

  return { busy, message, editing, setEditing, deleted, retry, page, setPage, submit, command }
}
