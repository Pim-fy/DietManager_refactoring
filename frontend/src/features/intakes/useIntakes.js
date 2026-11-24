import { useCallback, useEffect, useMemo, useState } from 'react'

import { formValues, today } from '../../shared/utils/form'
import { intakeApi } from './api'

/**
 * 섭취 기록 화면의 상태와 동작.
 * 화면에는 열려 있는 폼 상태(selected, editing, manual, nutritionEditing)와 그 폼을 여닫는 동작을 제공한다.
 */
export default function useIntakes(call) {
  const api = useMemo(() => intakeApi(call), [call])
  const [date, setDate] = useState(today)
  const [daily, setDaily] = useState(null)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState(null)
  const [selected, setSelected] = useState(null)
  const [editing, setEditing] = useState(null)
  const [manual, setManual] = useState(false)
  const [nutritionEditing, setNutritionEditing] = useState(null)
  const [retry, setRetry] = useState(0)
  const [favoriteRevision, setFavoriteRevision] = useState(0)

  const refresh = useCallback((targetDate, signal) => api.daily(targetDate, signal), [api])

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true)
      try {
        const data = await refresh(date, controller.signal)
        if (!controller.signal.aborted) {
          setDaily(data)
          setError('')
        }
      } catch (failure) {
        if (!controller.signal.aborted) setError(failure.message)
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    load()
    return () => controller.abort()
  }, [date, refresh, retry])

  async function mutate(action, success, onSuccess) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await action()
      onSuccess?.()
      setMessage({ text: success })
      try {
        setDaily(await refresh(date))
        setError('')
      } catch {
        setError('변경사항은 저장했지만 목록을 불러오지 못했습니다. 다시 불러오기를 눌러주세요.')
      }
    } catch (failure) {
      setMessage({ text: failure.message, error: true })
    } finally {
      setBusy(false)
    }
  }

  /** 열려 있는 폼을 모두 닫고, 인자로 받은 폼만 연다. 인자가 없으면 모두 닫는다. */
  function openForm({ food = null, record = null, manualInput = false, nutritionRecord = null } = {}) {
    setSelected(food)
    setEditing(record)
    setManual(manualInput)
    setNutritionEditing(nutritionRecord)
    setMessage(null)
  }

  function changeDate(value) {
    if (!value) return
    setDate(value)
    openForm()
  }

  function save(event) {
    event.preventDefault()
    const values = formValues(event)
    const body = { ...values, amount: Number(values.amount) }
    if (!editing) body.foodCode = selected.foodCode
    mutate(() => api.save(editing?.id, body), '섭취 기록을 저장했습니다.', () => {
      setSelected(null)
      setEditing(null)
    })
  }

  function saveNutrition(event) {
    event.preventDefault()
    const values = formValues(event)
    const nutrition = { foodName: values.foodName.trim(), baseAmount: Number(values.baseAmount) }
    for (const key of ['calories', 'carbohydrate', 'protein', 'fat']) {
      nutrition[key] = values[key] === '' ? null : Number(values[key])
    }
    const body = nutritionEditing
      ? nutrition
      : { nutrition, unit: values.unit, eatenDate: values.eatenDate, mealType: values.mealType, amount: Number(values.amount) }
    mutate(() => api.nutrition(nutritionEditing?.id, body), '음식명과 영양정보를 저장했습니다.', () => {
      setManual(false)
      setNutritionEditing(null)
    })
  }

  function remove(item) {
    mutate(() => api.remove(item.id), '기록을 삭제했습니다. 아래에서 복구할 수 있습니다.', () => {
      if (editing?.id === item.id) setEditing(null)
      if (nutritionEditing?.id === item.id) setNutritionEditing(null)
    })
  }

  function restore(item) {
    mutate(() => api.restore(item.id), '기록을 복구했습니다.')
  }

  return {
    date,
    changeDate,
    daily,
    loading,
    busy,
    error,
    message,
    reload: () => setRetry(value => value + 1),
    forms: { selected, editing, manual, nutritionEditing },
    selectFood: food => openForm({ food }),
    startEdit: record => openForm({ record }),
    startManual: () => openForm({ manualInput: true }),
    startNutritionEdit: record => openForm({ nutritionRecord: record }),
    cancelRecord: () => {
      setSelected(null)
      setEditing(null)
    },
    cancelNutrition: () => {
      setManual(false)
      setNutritionEditing(null)
    },
    favoriteRevision,
    favoriteAdded: () => setFavoriteRevision(value => value + 1),
    save,
    saveNutrition,
    remove,
    restore,
  }
}
