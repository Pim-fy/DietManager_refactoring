import { foodLabel, formatNutrition as format, meals } from '../../shared/utils/nutrition'

function IntakeItem({ item, busy, onEdit, onNutritionEdit, onDelete }) {
  return (
    <li>
      <div className="item-main">
        <strong>{foodLabel(item.foodName)}</strong>
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
      </div>
      <div className="item-side">
        <p className="item-kcal">
          {item.calories == null ? (
            <small>칼로리 정보 없음</small>
          ) : (
            <>
              {format(item.calories, 0)}
              <small> kcal</small>
            </>
          )}
        </p>
        <div className="row-actions">
          <button className="text-button" disabled={busy} aria-label={`${item.foodName} 수정`} onClick={() => onEdit(item)}>
            수정
          </button>
          <button className="text-button" disabled={busy} aria-label={`${item.foodName} 영양정보 수정`} onClick={() => onNutritionEdit(item)}>
            영양정보
          </button>
          <button className="text-button danger" disabled={busy} aria-label={`${item.foodName} 삭제`} onClick={() => onDelete(item)}>
            삭제
          </button>
        </div>
      </div>
    </li>
  )
}

function mealCalories(items) {
  return items.reduce((sum, item) => sum + (item.calories ?? 0), 0)
}

function DeletedIntakes({ items, busy, onRestore }) {
  return (
    <div className="undo-list">
      <h3>삭제한 기록</h3>
      {items.map(item => (
        <p key={item.id}>
          <span>{foodLabel(item.foodName)}</span>
          <button className="text-button" disabled={busy} aria-label={`${item.foodName} 복구`} onClick={() => onRestore(item)}>
            복구
          </button>
        </p>
      ))}
    </div>
  )
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
            <div className="meal-head">
              <h2>{label}</h2>
              {items.length ? <span className="meal-kcal">{format(mealCalories(items), 0)} kcal</span> : null}
              <button className="outline add" disabled={busy} aria-label={`${label}에 음식 추가`} onClick={() => onAdd(meal)}>
                + 추가
              </button>
            </div>
            {items.length ? (
              <ul className="intake-list">
                {items.map(item => (
                  <IntakeItem key={item.id} item={item} busy={busy} onEdit={onEdit} onNutritionEdit={onNutritionEdit} onDelete={onDelete} />
                ))}
              </ul>
            ) : (
              <p className="meal-empty">기록 없음</p>
            )}
          </div>
        )
      })}
      {daily.deletedItems?.length > 0 ? <DeletedIntakes items={daily.deletedItems} busy={busy} onRestore={onRestore} /> : null}
    </section>
  )
}
