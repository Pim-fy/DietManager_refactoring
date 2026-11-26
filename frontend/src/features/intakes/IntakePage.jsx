import DateStepper from '../../shared/components/DateStepper'
import Dialog from '../../shared/components/Dialog'
import { Message } from '../../shared/components/Forms'
import { meals } from '../../shared/utils/nutrition'
import FoodPicker from '../foods/FoodPicker'
import DailyReferenceTable from '../nutrition/DailyReferenceTable'
import DaySummary from '../nutrition/DaySummary'
import useRecommendation from '../nutrition/useRecommendation'
import { NutritionForm, RecordForm } from './IntakeForms'
import { defaultMeal } from './defaultMeal'
import IntakeRecordList from './IntakeRecordList'
import useIntakes from './useIntakes'

function dialogTitle({ adding, selected, editing, nutritionEditing }) {
  if (editing) return '섭취 기록 수정'
  if (nutritionEditing) return '음식명·영양정보 수정'
  if (selected) return `${meals[adding]} 섭취 기록 추가`
  return `${meals[adding]}에 음식 추가`
}

/** 계산 방식과 고정 기준치. 매일 볼 필요가 없어 접어둔다. */
function CalculationNote() {
  return (
    <details className="calc-note">
      <summary>권장량과 기준치는 어떻게 계산하나요?</summary>
      <p className="help">
        권장 섭취량은 만 19세 이상 일반 성인의 참고 추정값입니다. Mifflin–St Jeor 공식으로 안정 시 대사량을 구하고 활동량을 곱한 뒤, 목표에 따라 ±300 kcal를
        더합니다. 탄수화물·단백질·지방은 칼로리의 50·20·30%로 나눕니다. 지난 날짜는 그날 적용 중이던 목표와 활동량으로 계산합니다.
      </p>
      <DailyReferenceTable />
    </details>
  )
}

export default function IntakePage({ call }) {
  const intakes = useIntakes(call)
  const { date, daily, loading, busy, error, message, forms } = intakes
  const { adding, selected, editing, nutritionEditing } = forms
  const recommendation = useRecommendation(call, date)
  const loaded = daily && daily.date === date && !error
  const dialogOpen = Boolean(adding || editing || nutritionEditing)
  const shelfRevision = `${intakes.favoriteRevision}:${daily?.items.map(item => item.id).join(',') || ''}`

  function dialogContent() {
    if (editing) {
      return (
        <RecordForm
          key={`edit-${editing.id}`}
          food={selected}
          editing={editing}
          meal={adding}
          date={date}
          busy={busy}
          onSubmit={intakes.save}
          onCancel={intakes.close}
        />
      )
    }
    if (nutritionEditing) {
      return (
        <NutritionForm
          key={nutritionEditing.id}
          editing={nutritionEditing}
          date={date}
          busy={busy}
          onSubmit={intakes.saveNutrition}
          onCancel={intakes.close}
        />
      )
    }
    // 식품을 고른 뒤에도 picker를 숨긴 채 유지해 '다른 식품 고르기'로 돌아오면 탭·검색어·결과가 남아 있다.
    return (
      <>
        <div hidden={Boolean(selected)}>
          <FoodPicker
            call={call}
            disabled={busy}
            revision={shelfRevision}
            onSelect={intakes.selectFood}
            onFavorite={intakes.favoriteAdded}
            manual={
              <NutritionForm meal={adding} date={date} busy={busy} onSubmit={intakes.saveNutrition} onCancel={intakes.close} />
            }
          />
        </div>
        {selected ? (
          <RecordForm
            key={`food-${selected.foodCode}`}
            food={selected}
            meal={adding}
            date={date}
            busy={busy}
            onSubmit={intakes.save}
            onCancel={intakes.backToPicker}
          />
        ) : null}
      </>
    )
  }

  return (
    <>
      <div className="page-heading with-tools">
        <h1>섭취 기록</h1>
        <div className="page-tools">
          <DateStepper label="조회 날짜" value={date} disabled={busy} onChange={intakes.changeDate} />
          <button className="primary" disabled={busy || !loaded} onClick={() => intakes.startAdd(defaultMeal(date))}>
            음식 추가
          </button>
        </div>
      </div>
      {error ? (
        <div className="message error" role="alert">
          <p>{error}</p>
          <button className="outline" onClick={intakes.reload} disabled={busy || loading}>
            다시 불러오기
          </button>
        </div>
      ) : null}
      {dialogOpen ? null : <Message message={message} />}
      {loading ? (
        <p className="loading" role="status">
          섭취 기록을 불러오는 중…
        </p>
      ) : null}
      {loaded && !loading ? (
        <div className="intake-layout">
          <DaySummary
            date={date}
            daily={daily}
            target={recommendation.target}
            targetError={recommendation.error}
            onTargetRetry={recommendation.retry}
          />
          <IntakeRecordList
            date={date}
            daily={daily}
            loading={loading}
            error={error}
            busy={busy}
            onAdd={intakes.startAdd}
            onEdit={intakes.startEdit}
            onNutritionEdit={intakes.startNutritionEdit}
            onDelete={intakes.remove}
            onRestore={intakes.restore}
          />
        </div>
      ) : null}
      <CalculationNote />
      <Dialog open={dialogOpen} title={dialogOpen ? dialogTitle(forms) : ''} onClose={intakes.close}>
        <Message message={message} />
        {dialogOpen ? dialogContent() : null}
      </Dialog>
    </>
  )
}
