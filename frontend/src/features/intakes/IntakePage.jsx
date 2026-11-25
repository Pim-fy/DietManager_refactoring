import { Field, Message } from '../../shared/components/Forms'
import { today } from '../../shared/utils/form'
import FoodSearch from '../foods/FoodSearch'
import FoodShelf from '../foods/FoodShelf'
import DailyReferenceTable from '../nutrition/DailyReferenceTable'
import NutritionTarget from '../nutrition/NutritionTarget'
import { NutritionForm, RecordForm } from './IntakeForms'
import IntakeRecordList from './IntakeRecordList'
import NutritionTotals from './NutritionTotals'
import useIntakes from './useIntakes'

export default function IntakePage({ call }) {
  const intakes = useIntakes(call)
  const { date, daily, loading, busy, error, message, forms } = intakes
  const { selected, editing, manual, nutritionEditing } = forms
  const loaded = daily && daily.date === date && !error
  const shelfRevision = `${intakes.favoriteRevision}:${daily?.items.map(item => item.id).join(',') || ''}`

  return (
    <>
      <div className="page-heading">
        <h1>섭취 기록</h1>
        <p>먹은 음식을 기록하고 하루 영양 섭취량을 확인하세요.</p>
      </div>
      <div className="date-toolbar">
        <Field
          label="조회 날짜"
          name="date"
          type="date"
          max={today()}
          value={date}
          disabled={busy}
          onChange={event => intakes.changeDate(event.target.value)}
        />
      </div>
      {error ? (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button className="outline" onClick={intakes.reload} disabled={busy || loading}>
            다시 불러오기
          </button>
        </div>
      ) : null}
      <Message message={message} />
      {loading ? <p role="status">섭취 기록을 불러오는 중…</p> : loaded ? <NutritionTotals daily={daily} /> : null}
      <NutritionTarget call={call} date={date} daily={!loading && !error ? daily : null} />
      <DailyReferenceTable />
      <button className="outline" disabled={busy} onClick={intakes.startManual}>
        음식 직접 입력
      </button>
      {manual || nutritionEditing ? (
        <NutritionForm
          key={nutritionEditing?.id || 'manual'}
          editing={nutritionEditing}
          date={date}
          busy={busy}
          onSubmit={intakes.saveNutrition}
          onCancel={intakes.cancelNutrition}
        />
      ) : null}
      <FoodSearch call={call} disabled={busy} onFavorite={intakes.favoriteAdded} onSelect={intakes.selectFood} />
      <FoodShelf call={call} disabled={busy} revision={shelfRevision} onSelect={intakes.selectFood} />
      {selected || editing ? (
        <RecordForm
          key={editing ? `edit-${editing.id}` : `food-${selected.foodCode}`}
          food={selected}
          editing={editing}
          date={date}
          busy={busy}
          onSubmit={intakes.save}
          onCancel={intakes.cancelRecord}
        />
      ) : null}
      <IntakeRecordList
        date={date}
        daily={daily}
        loading={loading}
        error={error}
        busy={busy}
        onEdit={intakes.startEdit}
        onNutritionEdit={intakes.startNutritionEdit}
        onDelete={intakes.remove}
        onRestore={intakes.restore}
      />
    </>
  )
}
