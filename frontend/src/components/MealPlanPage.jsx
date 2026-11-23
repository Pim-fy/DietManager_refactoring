import { today } from '../form-utils'
import useMealPlans from '../hooks/useMealPlans'
import { Field, Message, SaveButton } from './Forms'
import FoodSearch from './FoodSearch'
import FoodShelf from './FoodShelf'
import { meals } from '../food-display'
export default function MealPlanPage({ call }) {
  const { date, setDate, plans, selected, setSelected, editing, setEditing, busy, message, setRetry, favoriteRevision, setFavoriteRevision, command, save } = useMealPlans(call)
  const food = selected || editing?.food
  return <><div className="page-heading"><h1>식단 계획</h1><p>미리 계획하고 먹은 뒤 섭취 기록으로 옮기세요.</p></div>
    <div className="date-toolbar"><Field label="계획 조회 날짜" type="date" value={date} disabled={busy} onChange={event => { if (event.target.value) { setDate(event.target.value); setSelected(null); setEditing(null) } }} /></div>
    <Message message={message} />{message?.error ? <button className="outline" onClick={() => setRetry(value => value + 1)} disabled={busy}>다시 불러오기</button> : null}
    <FoodSearch call={call} disabled={busy} onFavorite={() => setFavoriteRevision(value => value + 1)} onSelect={value => { setSelected(value); setEditing(null) }} />
    <FoodShelf call={call} disabled={busy} revision={favoriteRevision} onSelect={value => { setSelected(value); setEditing(null) }} />
    {food ? <section className="panel"><h2>{editing ? '계획 수정' : '계획 추가'} · {food.foodName.replaceAll('_', ' ')}</h2><form key={editing?.id || food.foodCode} onSubmit={save}><fieldset disabled={busy} className="intake-fields">
      <Field label="계획 날짜" name="plannedDate" type="date" defaultValue={editing?.plannedDate || date} />
      <label className="field"><span>끼니</span><select name="mealType" defaultValue={editing?.mealType || 'BREAKFAST'}>{Object.entries(meals).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <Field label={`계획량 (${food.baseUnit})`} name="amount" type="number" min="0.01" max="1000000" step="0.01" defaultValue={editing?.amount || ''} autoFocus /></fieldset>
      <div className="form-actions"><SaveButton busy={busy}>계획 저장</SaveButton><button className="outline" type="button" disabled={busy} onClick={() => { setSelected(null); setEditing(null) }}>취소</button></div></form></section> : null}
    <section className="panel"><h2>{date} 계획</h2><p className="help">계획은 섭취 합계에 포함되지 않습니다. 같은 계획은 한 번만 섭취로 기록할 수 있습니다. 계획을 삭제해도 이미 저장한 섭취 기록은 유지됩니다.</p>
      {plans?.date !== date ? <p role="status">계획을 불러오는 중…</p> : plans.items.length === 0 ? <p className="empty">등록된 계획이 없습니다.</p> : <ul className="intake-list">{plans.items.map(item => <li key={item.id}><div><span className="meal-label">{meals[item.mealType]}</span><strong>{item.food.foodName.replaceAll('_', ' ')}</strong>
        <p>{item.amount} {item.food.baseUnit} · {item.nutrition.calories ?? '정보 없음'} kcal</p>{item.intakeId ? <p>섭취 기록으로 저장됨 (기록 #{item.intakeId})</p> : null}{item.deleted ? <p>삭제된 계획</p> : null}</div>
        <div className="row-actions">{item.deleted ? <button className="outline" disabled={busy} onClick={() => command(item.id, 'restore', '계획을 복구했습니다.')}>복구</button> : <>
          {!item.intakeId ? <><button className="outline" disabled={busy} onClick={() => { setEditing(item); setSelected(null) }}>수정</button><button className="outline" disabled={busy || item.plannedDate > today()} onClick={() => command(item.id, 'consume', '섭취 기록으로 저장했습니다.')}>섭취로 기록</button></> : null}
          <button className="outline danger" disabled={busy} onClick={() => command(item.id, 'delete', '계획을 삭제했습니다.')}>삭제</button></>}</div></li>)}</ul>}
    </section></>
}
