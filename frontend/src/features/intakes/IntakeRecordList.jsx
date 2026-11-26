import DeletedList from '../../shared/components/DeletedList'
import EmptyState from '../../shared/components/EmptyState'
import RecordItem from '../../shared/components/RecordItem'
import SectionHeader from '../../shared/components/SectionHeader'
import { foodLabel, formatNutrition as format, meals } from '../../shared/utils/nutrition'

function IntakeItem({ item, busy, onEdit, onNutritionEdit, onDelete }) {
  return (
    <RecordItem
      title={foodLabel(item.foodName)}
      kcal={item.calories}
      actions={
        <>
          <button className="text-button" disabled={busy} aria-label={`${item.foodName} 수정`} onClick={() => onEdit(item)}>
            수정
          </button>
          <button className="text-button" disabled={busy} aria-label={`${item.foodName} 영양정보 수정`} onClick={() => onNutritionEdit(item)}>
            영양정보
          </button>
          <button className="text-button danger" disabled={busy} aria-label={`${item.foodName} 삭제`} onClick={() => onDelete(item)}>
            삭제
          </button>
        </>
      }
    >
      <p>
        {format(item.amount)} {item.unit}
        <span className="source">
          {item.manual ? '직접 입력' : '식품 DB'}
          {item.nutritionEdited ? ' · 영양정보 수정함' : ''}
        </span>
      </p>
      <p className="food-macros">
        <span>탄수화물 {format(item.carbohydrate, 1)} g</span>
        <span>단백질 {format(item.protein, 1)} g</span>
        <span>지방 {format(item.fat, 1)} g</span>
      </p>
    </RecordItem>
  )
}

function mealCalories(items) {
  return items.reduce((sum, item) => sum + (item.calories ?? 0), 0)
}

/** 그날 섭취 기록을 끼니별로 묶어 보여준다. 끼니마다 음식 추가 버튼이 있다. */
export default function IntakeRecordList({ date, daily, loading, error, busy, onAdd, onEdit, onNutritionEdit, onDelete, onRestore }) {
  const loaded = !loading && !error && daily?.date === date
  if (!loaded) return null

  return (
    <section className="meals" aria-label="섭취 내역">
      {Object.entries(meals).map(([meal, label]) => {
        const items = daily.items.filter(item => item.mealType === meal)
        return (
          <div key={meal} className="meal-group">
            <SectionHeader title={label} meta={items.length ? `${format(mealCalories(items), 0)} kcal` : null} divided>
              <button className="outline add" disabled={busy} aria-label={`${label}에 음식 추가`} onClick={() => onAdd(meal)}>
                + 추가
              </button>
            </SectionHeader>
            {items.length ? (
              <ul className="intake-list">
                {items.map(item => (
                  <IntakeItem key={item.id} item={item} busy={busy} onEdit={onEdit} onNutritionEdit={onNutritionEdit} onDelete={onDelete} />
                ))}
              </ul>
            ) : (
              <EmptyState size="compact">기록 없음</EmptyState>
            )}
          </div>
        )
      })}
      <DeletedList
        title="삭제한 기록"
        items={daily.deletedItems || []}
        itemKey={item => item.id}
        renderLabel={item => foodLabel(item.foodName)}
        restoreName={item => `${item.foodName} 복구`}
        busy={busy}
        onRestore={onRestore}
      />
    </section>
  )
}
