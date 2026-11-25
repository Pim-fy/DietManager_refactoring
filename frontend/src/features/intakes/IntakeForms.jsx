import { Field, SaveButton } from '../../shared/components/Forms'
import { today } from '../../shared/utils/form'
import { foodLabel, meals } from '../../shared/utils/nutrition'

const BASE_NUTRIENTS = [
  ['calories', '기준 칼로리 (kcal)', 'baseCalories'],
  ['carbohydrate', '기준 탄수화물 (g)', 'baseCarbohydrate'],
  ['protein', '기준 단백질 (g)', 'baseProtein'],
  ['fat', '기준 지방 (g)', 'baseFat'],
]

function MealField({ value = 'BREAKFAST' }) {
  return (
    <label className="field">
      <span>끼니</span>
      <select name="mealType" defaultValue={value} required>
        {Object.entries(meals).map(([key, label]) => (
          <option key={key} value={key}>
            {label}
          </option>
        ))}
      </select>
    </label>
  )
}

function FormActions({ busy, submitLabel, cancelLabel = '취소', onCancel }) {
  return (
    <div className="form-actions">
      <SaveButton busy={busy}>{submitLabel}</SaveButton>
      <button className="outline" type="button" disabled={busy} onClick={onCancel}>
        {cancelLabel}
      </button>
    </div>
  )
}

/** 식품 DB 식품으로 섭취 기록 추가, 또는 기존 기록의 날짜·끼니·섭취량 수정. */
export function RecordForm({ food, editing, meal, date, busy, onSubmit, onCancel }) {
  return (
    <div className="record-form">
      <p className="picked-food">
        <strong>{foodLabel(food?.foodName || editing.foodName)}</strong>
        <span>{food ? `${food.servingSize} 기준 영양정보` : '이 기록에 저장된 기준 영양정보로 계산합니다.'}</span>
      </p>
      <form onSubmit={onSubmit}>
        <fieldset disabled={busy} className="intake-fields">
          <Field label="섭취일" name="eatenDate" type="date" max={today()} defaultValue={editing?.eatenDate || date} />
          <MealField value={editing?.mealType || meal || undefined} />
          <Field
            label={`섭취량 (${food?.baseUnit || editing.unit})`}
            name="amount"
            type="number"
            min="0.01"
            max="1000000"
            step="0.01"
            defaultValue={editing?.amount || ''}
            autoFocus
          />
        </fieldset>
        <FormActions busy={busy} submitLabel={editing ? '수정 저장' : '섭취 기록 저장'} cancelLabel={editing ? '취소' : '다른 식품 고르기'} onCancel={onCancel} />
      </form>
    </div>
  )
}

/** 음식 직접 입력(editing 없음), 또는 기존 기록의 음식명·기준 영양정보 수정. */
export function NutritionForm({ editing, meal, date, busy, onSubmit, onCancel }) {
  return (
    <div className="nutrition-form">
      <p className="help">
        영양값은 기준량에 해당하는 값을 입력하고, 모르는 값은 비워두세요. 실제 섭취량에 비례해 계산합니다.
        {editing ? ' 이 기록에만 적용되고, 처음 저장한 영양정보는 따로 보관합니다.' : ''}
      </p>
      <form onSubmit={onSubmit}>
        <fieldset disabled={busy} className="intake-fields">
          <Field label="음식명" name="foodName" maxLength={500} defaultValue={editing?.foodName || ''} />
          {!editing ? (
            <>
              <Field label="섭취일" name="eatenDate" type="date" max={today()} defaultValue={date} />
              <MealField value={meal || undefined} />
              <label className="field">
                <span>단위</span>
                <select name="unit" defaultValue="g">
                  <option value="g">g</option>
                  <option value="ml">ml</option>
                </select>
              </label>
              <Field label="섭취량" name="amount" type="number" min="0.01" max="1000000" step="0.01" />
            </>
          ) : null}
          <Field
            label={`영양정보 기준량${editing ? ` (${editing.unit})` : ' (선택한 단위)'}`}
            name="baseAmount"
            type="number"
            min="0.01"
            max="1000000"
            step="0.01"
            defaultValue={editing?.baseAmount || 100}
          />
          {BASE_NUTRIENTS.map(([name, label, key]) => (
            <Field
              key={name}
              label={label}
              name={name}
              type="number"
              required={false}
              min="0"
              max="1000000"
              step="0.000001"
              defaultValue={editing?.[key] ?? ''}
            />
          ))}
        </fieldset>
        <FormActions busy={busy} submitLabel={editing ? '영양정보 저장' : '직접 입력 저장'} onCancel={onCancel} />
      </form>
    </div>
  )
}
