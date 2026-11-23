import { meals, formatNutrition as format } from '../../food-display'
export default function IntakeRecordList({ date, daily, loading, error, busy, onEdit, onNutritionEdit, onDelete, onRestore }) {
  return (
    <section className="panel"><h2>섭취 내역 <span className="count">{date}</span></h2>
      {!loading && daily?.date === date && !error ? daily.items.length === 0 ? <p className="empty">이 날짜에 등록된 섭취 기록이 없습니다.</p> : <ul className="intake-list">
        {daily.items.map(item => <li key={item.id}><div><span className="meal-label">{meals[item.mealType]}</span><strong>{item.foodName.replaceAll('_', ' ')}</strong>
          <p>{format(item.amount)} {item.unit} · {format(item.calories)} kcal</p><p className="help">{item.manual ? '직접 입력' : '식품 API'}{item.nutritionEdited ? ' · 사용자 수정' : ''}</p><p className="food-macros">탄수화물 {format(item.carbohydrate)} g · 단백질 {format(item.protein)} g · 지방 {format(item.fat)} g</p></div>
          <div className="row-actions"><button className="outline" disabled={busy} aria-label={`${item.foodName} 수정`} onClick={() => onEdit(item)}>수정</button>
            <button className="outline" disabled={busy} aria-label={`${item.foodName} 영양정보 수정`} onClick={() => onNutritionEdit(item)}>영양정보 수정</button>
            <button className="outline danger" disabled={busy} aria-label={`${item.foodName} 삭제`} onClick={() => onDelete(item)}>삭제</button></div></li>)}
      </ul> : <p className="help">날짜별 기록을 불러온 뒤 표시합니다.</p>}
      {!loading && !error && daily?.date === date && daily.deletedItems?.length > 0 ? <div className="undo-list"><h3>삭제된 기록</h3>{daily.deletedItems.map(item => <p key={item.id}>{item.foodName.replaceAll('_', ' ')} <button className="outline" disabled={busy}
        aria-label={`${item.foodName} 복구`} onClick={() => onRestore(item)}>복구</button></p>)}</div> : null}
    </section>
  )
}
