import { Field, SaveButton } from '../../components/Forms'
import { today } from '../../form-utils'
import { meals } from '../../food-display'
function MealField({ value = 'BREAKFAST' }) {
  return <label className="field"><span>끼니</span><select name="mealType" defaultValue={value} required>
    {Object.entries(meals).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
  </select></label>
}
export function RecordForm({ food, editing, date, busy, onSubmit, onCancel }) {
  return <section className="panel"><h2>{editing ? '섭취 기록 수정' : '섭취 기록 추가'}</h2>
    <p><strong>{(food?.foodName || editing.foodName).replaceAll('_', ' ')}</strong></p>
    <p className="help">{food ? `영양성분 기준: ${food.servingSize}` : '이 기록에 저장된 기준 영양정보로 계산합니다.'}</p>
    <form onSubmit={onSubmit}><fieldset disabled={busy} className="intake-fields">
      <Field label="섭취일" name="eatenDate" type="date" max={today()} defaultValue={editing?.eatenDate || date} />
      <MealField value={editing?.mealType} />
      <Field label={`섭취량 (${food?.baseUnit || editing.unit})`} name="amount" type="number" min="0.01" max="1000000" step="0.01" defaultValue={editing?.amount || ''} autoFocus />
    </fieldset><div className="form-actions"><SaveButton busy={busy}>{editing ? '수정 저장' : '섭취 기록 저장'}</SaveButton>
      <button className="outline" type="button" disabled={busy} onClick={onCancel}>취소</button></div></form>
  </section>
}
export function NutritionForm({ editing, date, busy, onSubmit, onCancel }) {
  return <section className="panel"><h2>{editing ? '음식명·영양정보 수정' : '음식 직접 입력'}</h2>
    <p className="help">영양값은 아래 기준량에 해당하는 값을 입력하세요. 모르는 값은 비워두세요. 실제 섭취량에 비례해 합계를 계산합니다.
      {editing ? ' 수정은 이 기록에만 적용되며 저장 당시 원본은 보관합니다. 날짜·섭취량 변경은 일반 수정 버튼을 사용하세요.' : ''}</p>
    <form onSubmit={onSubmit}><fieldset disabled={busy} className="intake-fields">
      <Field label="음식명" name="foodName" maxLength={500} defaultValue={editing?.foodName || ''} />
      {!editing ? <><Field label="섭취일" name="eatenDate" type="date" max={today()} defaultValue={date} /><MealField />
        <label className="field"><span>단위</span><select name="unit" defaultValue="g"><option value="g">g</option><option value="ml">ml</option></select></label>
        <Field label="섭취량" name="amount" type="number" min="0.01" max="1000000" step="0.01" /></> : null}
      <Field label={`영양정보 기준량${editing ? ` (${editing.unit})` : ' (선택한 단위)'}`} name="baseAmount" type="number" min="0.01" max="1000000" step="0.01" defaultValue={editing?.baseAmount || 100} />
      {[['calories', '기준 칼로리 (kcal)', 'baseCalories'], ['carbohydrate', '기준 탄수화물 (g)', 'baseCarbohydrate'], ['protein', '기준 단백질 (g)', 'baseProtein'], ['fat', '기준 지방 (g)', 'baseFat']].map(([name, label, key]) =>
        <Field key={name} label={label} name={name} type="number" required={false} min="0" max="1000000" step="0.000001" defaultValue={editing?.[key] ?? ''} />)}
    </fieldset><div className="form-actions"><SaveButton busy={busy}>{editing ? '영양정보 저장' : '직접 입력 저장'}</SaveButton>
      <button className="outline" type="button" disabled={busy} onClick={onCancel}>취소</button></div></form>
  </section>
}
