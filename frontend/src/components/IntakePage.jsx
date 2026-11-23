import IntakeRecordList from '../features/intakes/IntakeRecordList'
import { today } from '../form-utils'
import useIntakes from '../hooks/useIntakes'
import { Field, Message } from './Forms'
import { RecordForm, NutritionForm } from '../features/intakes/IntakeForms'
import FoodSearch from './FoodSearch'
import FoodShelf from './FoodShelf'
import NutritionTarget from './NutritionTarget'
import { nutrients, formatNutrition as format } from '../food-display'
export default function IntakePage({ call }) {
  const { date, setDate, daily, loading, busy, error, message, selected, setSelected, editing, setEditing, manual, setManual, nutritionEditing, setNutritionEditing, setMessage, setRetry, favoriteRevision, setFavoriteRevision, restore, save, remove, saveNutrition, selectFood } = useIntakes(call)
  return <>
    <div className="page-heading"><h1>섭취 기록</h1><p>먹은 음식을 기록하고 하루 영양 섭취량을 확인하세요.</p></div>
    <div className="date-toolbar"><Field label="조회 날짜" name="date" type="date" max={today()} value={date} disabled={busy}
      onChange={event => { if (event.target.value) { setDate(event.target.value); setSelected(null); setEditing(null); setManual(false); setNutritionEditing(null); setMessage(null) } }} /></div>
    {error ? <div className="message error" role="alert"><p>{error}</p><button className="outline" onClick={() => setRetry(value => value + 1)} disabled={busy || loading}>다시 불러오기</button></div> : null}
    <Message message={message} />
    {loading ? <p role="status">섭취 기록을 불러오는 중…</p> : daily && daily.date === date && !error ? <div className="nutrition-totals" aria-label="일별 영양 합계">
      {nutrients.map(([key, label, unit]) => <div key={key}><span>{label}</span>
        <strong>{format(daily[key].value)} <small>{unit}</small></strong>
        {daily[key].missingCount > 0 ? <p className="unit-warning">정보 없는 {daily[key].missingCount}건 제외한 합계</p> : null}</div>)}
    </div> : null}
    <NutritionTarget call={call} date={date} daily={!loading && !error ? daily : null} />
    <button className="outline" disabled={busy} onClick={() => { setManual(true); setNutritionEditing(null); setSelected(null); setEditing(null); setMessage(null) }}>음식 직접 입력</button>
    {manual || nutritionEditing ? <NutritionForm key={nutritionEditing?.id || 'manual'} editing={nutritionEditing} date={date} busy={busy} onSubmit={saveNutrition} onCancel={() => { setManual(false); setNutritionEditing(null) }} /> : null}
    <FoodSearch call={call} disabled={busy} onFavorite={() => setFavoriteRevision(value => value + 1)} onSelect={selectFood} />
    <FoodShelf call={call} disabled={busy} revision={`${favoriteRevision}:${daily?.items.map(item => item.id).join(',') || ''}`} onSelect={selectFood} />
    {selected || editing ? <RecordForm key={editing ? `edit-${editing.id}` : `food-${selected.foodCode}`} food={selected} editing={editing} date={date} busy={busy} onSubmit={save}
      onCancel={() => { setSelected(null); setEditing(null) }} /> : null}
    <IntakeRecordList date={date} daily={daily} loading={loading} error={error} busy={busy}
      onEdit={item => { setEditing(item); setSelected(null); setManual(false); setNutritionEditing(null); setMessage(null) }}
      onNutritionEdit={item => { setNutritionEditing(item); setManual(false); setSelected(null); setEditing(null); setMessage(null) }}
      onDelete={remove} onRestore={restore} />
  </>
}
