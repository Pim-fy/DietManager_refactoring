import DateStepper from '../../shared/components/DateStepper'
import Dialog from '../../shared/components/Dialog'
import { Field, Message, RetryButton, SaveButton } from '../../shared/components/Forms'
import { today } from '../../shared/utils/form'
import { foodLabel, formatNutrition as format, meals } from '../../shared/utils/nutrition'
import FoodPicker from '../foods/FoodPicker'
import useMealPlans from './useMealPlans'

function MealPlanForm({ food, editing, date, busy, onSubmit, onCancel }) {
  return (
    <div className="record-form">
      <p className="picked-food">
        <strong>{foodLabel(food.foodName)}</strong>
        {food.servingSize ? <span>{food.servingSize} 기준 영양정보</span> : null}
      </p>
      <form key={editing?.id || food.foodCode} onSubmit={onSubmit}>
        <fieldset disabled={busy} className="intake-fields">
          <Field label="계획 날짜" name="plannedDate" type="date" defaultValue={editing?.plannedDate || date} />
          <label className="field">
            <span>끼니</span>
            <select name="mealType" defaultValue={editing?.mealType || 'BREAKFAST'}>
              {Object.entries(meals).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <Field
            label={`계획량 (${food.baseUnit})`}
            name="amount"
            type="number"
            min="0.01"
            max="1000000"
            step="0.01"
            defaultValue={editing?.amount || ''}
            autoFocus
          />
        </fieldset>
        <div className="form-actions">
          <SaveButton busy={busy}>계획 저장</SaveButton>
          <button className="outline" type="button" disabled={busy} onClick={onCancel}>
            {editing ? '취소' : '다른 식품 고르기'}
          </button>
        </div>
      </form>
    </div>
  )
}

function MealPlanItem({ item, busy, onEdit, onCommand }) {
  return (
    <li className={item.deleted ? 'is-deleted' : undefined}>
      <div className="item-main">
        <strong>{foodLabel(item.food.foodName)}</strong>
        <p>
          {item.amount} {item.food.baseUnit}
          {item.intakeId ? <span className="source done">섭취 기록으로 옮김</span> : null}
          {item.deleted ? <span className="source">삭제한 계획</span> : null}
        </p>
      </div>
      <div className="item-side">
        <p className="item-kcal">
          {item.nutrition.calories == null ? (
            <small>칼로리 정보 없음</small>
          ) : (
            <>
              {format(item.nutrition.calories, 0)}
              <small> kcal</small>
            </>
          )}
        </p>
        <div className="row-actions">
          {item.deleted ? (
            <button className="text-button" disabled={busy} onClick={() => onCommand(item.id, 'restore', '계획을 복구했습니다.')}>
              복구
            </button>
          ) : (
            <>
              {!item.intakeId ? (
                <>
                  <button
                    className="text-button strong"
                    disabled={busy || item.plannedDate > today()}
                    title={item.plannedDate > today() ? '계획한 날짜가 되면 섭취로 기록할 수 있습니다.' : undefined}
                    onClick={() => onCommand(item.id, 'consume', '섭취 기록으로 저장했습니다.')}
                  >
                    섭취로 기록
                  </button>
                  <button className="text-button" disabled={busy} onClick={() => onEdit(item)}>
                    수정
                  </button>
                </>
              ) : null}
              <button className="text-button danger" disabled={busy} onClick={() => onCommand(item.id, 'delete', '계획을 삭제했습니다.')}>
                삭제
              </button>
            </>
          )}
        </div>
      </div>
    </li>
  )
}

export default function MealPlanPage({ call }) {
  const plans = useMealPlans(call)
  const { date, busy, message, editing } = plans
  const food = plans.selected || editing?.food
  const loaded = plans.plans?.date === date
  const dialogOpen = plans.picking || Boolean(editing)

  return (
    <>
      <div className="page-heading with-tools">
        <h1>식단 계획</h1>
        <div className="page-tools">
          <DateStepper label="계획 조회 날짜" value={date} disabled={busy} onChange={plans.changeDate} future />
          <button className="primary" disabled={busy} onClick={plans.startAdd}>
            계획 추가
          </button>
        </div>
      </div>
      {dialogOpen ? null : <Message message={message} />}
      {message?.error && !dialogOpen ? <RetryButton onRetry={plans.reload} disabled={busy} /> : null}
      {!loaded ? (
        <p className="loading" role="status">
          계획을 불러오는 중…
        </p>
      ) : plans.plans.items.length === 0 ? (
        <div className="empty">
          <p>이 날짜에 세운 계획이 없습니다.</p>
          <button className="outline" disabled={busy} onClick={plans.startAdd}>
            계획 추가
          </button>
        </div>
      ) : (
        <section className="meals" aria-label={`${date} 계획`}>
          {Object.entries(meals).map(([meal, label]) => {
            const items = plans.plans.items.filter(item => item.mealType === meal)
            if (!items.length) return null
            return (
              <div key={meal} className="meal-group">
                <div className="meal-head">
                  <h2>{label}</h2>
                </div>
                <ul className="intake-list">
                  {items.map(item => (
                    <MealPlanItem key={item.id} item={item} busy={busy} onEdit={plans.startEdit} onCommand={plans.command} />
                  ))}
                </ul>
              </div>
            )
          })}
        </section>
      )}
      <p className="help plan-note">
        계획은 하루 섭취 합계에 들어가지 않습니다. 같은 계획은 한 번만 섭취로 기록할 수 있고, 계획을 삭제해도 이미 옮긴 섭취 기록은 남습니다.
      </p>
      <Dialog open={dialogOpen} title={editing ? '계획 수정' : '계획 추가'} onClose={plans.cancel}>
        {dialogOpen ? <Message message={message} /> : null}
        {food ? (
          <MealPlanForm
            food={food}
            editing={editing}
            date={date}
            busy={busy}
            onSubmit={plans.save}
            onCancel={editing ? plans.cancel : plans.backToPicker}
          />
        ) : dialogOpen ? (
          <FoodPicker call={call} disabled={busy} revision={plans.favoriteRevision} onSelect={plans.selectFood} onFavorite={plans.favoriteAdded} />
        ) : null}
      </Dialog>
    </>
  )
}
