import { useCallback, useMemo, useState } from 'react'

import AppLink from '../../shared/components/AppLink'
import { Message } from '../../shared/components/Forms'
import ResourceState from '../../shared/components/ResourceState'
import useResource from '../../shared/hooks/useResource'
import { foodLabel, formatNutrition as format, meals } from '../../shared/utils/nutrition'
import { planApi } from '../plans/api'

const MEAL_ORDER = Object.keys(meals)

/** 오늘 세운 식단 계획. 아직 옮기지 않은 계획은 바로 섭취로 기록할 수 있다. */
export default function TodayPlans({ call, date, onConsumed }) {
  const api = useMemo(() => planApi(call), [call])
  const load = useCallback(signal => api.daily(date, signal), [api, date])
  const plans = useResource(load)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null)

  async function consume(item) {
    if (busy) return
    setBusy(true)
    setMessage(null)
    try {
      await api.command(item.id, 'consume')
      onConsumed()
    } catch (failure) {
      setMessage({ text: failure.message, error: true })
    }
    try {
      await plans.refresh()
    } catch {
      plans.retry()
    } finally {
      setBusy(false)
    }
  }

  const items = (plans.data || [])
    .filter(item => !item.deleted)
    .sort((a, b) => MEAL_ORDER.indexOf(a.mealType) - MEAL_ORDER.indexOf(b.mealType))

  return (
    <section className="dash-section" aria-labelledby="dash-plans-title">
      <div className="dash-head">
        <h2 id="dash-plans-title">오늘 식단 계획</h2>
        <AppLink to="/plans">식단 계획으로</AppLink>
      </div>
      <Message message={message} />
      <ResourceState resource={plans} loadingText="오늘 계획을 불러오는 중…">
        {items.length ? (
          <ul className="dash-list">
            {items.map(item => (
              <li key={item.id}>
                <div className="dash-plan">
                  <strong>
                    <span className="dash-meal">{meals[item.mealType]}</span>
                    {foodLabel(item.food.foodName)}
                  </strong>
                  <span>
                    {item.amount} {item.food.baseUnit}
                    {item.nutrition.calories == null ? '' : ` · ${format(item.nutrition.calories, 0)} kcal`}
                  </span>
                </div>
                {item.intakeId ? (
                  <span className="dash-done">섭취 기록으로 옮김</span>
                ) : (
                  <button
                    type="button"
                    className="text-button strong"
                    disabled={busy}
                    aria-label={`${foodLabel(item.food.foodName)} 섭취로 기록`}
                    onClick={() => consume(item)}
                  >
                    섭취로 기록
                  </button>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p className="dash-empty">오늘 세운 계획이 없습니다. 먹을 음식을 미리 계획하면 여기서 바로 섭취로 기록할 수 있습니다.</p>
        )}
      </ResourceState>
    </section>
  )
}
