import { foodLabel, formatNutrition as format, meals } from '../../shared/utils/nutrition'

function IntakeItem({ item, busy, onEdit, onNutritionEdit, onDelete }) {
  return (
    <li>
      <div>
        <span className="meal-label">{meals[item.mealType]}</span>
        <strong>{foodLabel(item.foodName)}</strong>
        <p>
          {format(item.amount)} {item.unit} · {format(item.calories)} kcal
        </p>
        <p className="help">
          {item.manual ? '직접 입력' : '식품 API'}
          {item.nutritionEdited ? ' · 사용자 수정' : ''}
        </p>
        <p className="food-macros">
          탄수화물 {format(item.carbohydrate)} g · 단백질 {format(item.protein)} g · 지방 {format(item.fat)} g
        </p>
      </div>
      <div className="row-actions">
        <button className="outline" disabled={busy} aria-label={`${item.foodName} 수정`} onClick={() => onEdit(item)}>
          수정
        </button>
        <button className="outline" disabled={busy} aria-label={`${item.foodName} 영양정보 수정`} onClick={() => onNutritionEdit(item)}>
          영양정보 수정
        </button>
        <button className="outline danger" disabled={busy} aria-label={`${item.foodName} 삭제`} onClick={() => onDelete(item)}>
          삭제
        </button>
      </div>
    </li>
  )
}

function DeletedIntakes({ items, busy, onRestore }) {
  return (
    <div className="undo-list">
      <h3>삭제된 기록</h3>
      {items.map(item => (
        <p key={item.id}>
          {foodLabel(item.foodName)}{' '}
          <button className="outline" disabled={busy} aria-label={`${item.foodName} 복구`} onClick={() => onRestore(item)}>
            복구
          </button>
        </p>
      ))}
    </div>
  )
}

export default function IntakeRecordList({ date, daily, loading, error, busy, onEdit, onNutritionEdit, onDelete, onRestore }) {
  const loaded = !loading && !error && daily?.date === date
  return (
    <section className="panel">
      <h2>
        섭취 내역 <span className="count">{date}</span>
      </h2>
      {!loaded ? (
        <p className="help">날짜별 기록을 불러온 뒤 표시합니다.</p>
      ) : daily.items.length === 0 ? (
        <p className="empty">이 날짜에 등록된 섭취 기록이 없습니다.</p>
      ) : (
        <ul className="intake-list">
          {daily.items.map(item => (
            <IntakeItem key={item.id} item={item} busy={busy} onEdit={onEdit} onNutritionEdit={onNutritionEdit} onDelete={onDelete} />
          ))}
        </ul>
      )}
      {loaded && daily.deletedItems?.length > 0 ? <DeletedIntakes items={daily.deletedItems} busy={busy} onRestore={onRestore} /> : null}
    </section>
  )
}
