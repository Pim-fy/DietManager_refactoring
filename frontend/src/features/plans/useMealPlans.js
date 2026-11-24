import { useEffect, useMemo, useState } from 'react'

import { formValues, today } from '../../shared/utils/form'
import { planApi } from './api'

export default function useMealPlans(call) {
  const api = useMemo(() => planApi(call), [call])
  const [date, setDate] = useState(today)
  const [plans, setPlans] = useState(null)
  const [selected, setSelected] = useState(null)
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)
  const [retry, setRetry] = useState(0)
  const [favoriteRevision, setFavoriteRevision] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    api
      .daily(date, controller.signal)
      .then(items => {
        if (!controller.signal.aborted) {
          setPlans({ date, items })
          setMessage(null)
        }
      })
      .catch(error => {
        if (!controller.signal.aborted) setMessage({ text: error.message, error: true })
      })
    return () => controller.abort()
  }, [api, date, retry])

  async function mutate(action, text) {
    if (busy) return
    setBusy(true)
    try {
      await action()
      setSelected(null)
      setEditing(null)
      setMessage({ text })
      try {
        setPlans({ date, items: await api.daily(date) })
      } catch {
        setMessage({ text: '변경사항은 저장했습니다. 목록을 다시 불러와주세요.', error: true })
      }
    } catch (error) {
      setMessage({ text: error.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  function save(event) {
    event.preventDefault()
    const body = { ...formValues(event), foodCode: (selected || editing.food).foodCode }
    body.amount = Number(body.amount)
    mutate(() => api.save(editing?.id, body), '식단 계획을 저장했습니다.')
  }

  function command(id, action, text) {
    return mutate(() => api.command(id, action), text)
  }

  return {
    date,
    changeDate: value => {
      if (!value) return
      setDate(value)
      setSelected(null)
      setEditing(null)
    },
    plans,
    selected,
    editing,
    busy,
    message,
    reload: () => setRetry(value => value + 1),
    selectFood: food => {
      setSelected(food)
      setEditing(null)
    },
    startEdit: plan => {
      setEditing(plan)
      setSelected(null)
    },
    cancel: () => {
      setSelected(null)
      setEditing(null)
    },
    favoriteRevision,
    favoriteAdded: () => setFavoriteRevision(value => value + 1),
    save,
    command,
  }
}
