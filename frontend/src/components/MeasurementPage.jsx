import { today } from '../form-utils'
import useMeasurements from '../hooks/useMeasurements'
import { Field, SaveButton, Message } from './Forms'
export default function MeasurementPage({ measurements, latest, call, onSaved }) {
  const { busy, message, editing, setEditing, deleted, retry, page, setPage, command, submit } = useMeasurements(call, onSaved)
  return <>
    <div className="page-heading"><h1>신체 기록</h1><p>키와 몸무게 변화를 기록하세요.</p></div>
    {latest ? <p className="latest">최근 기록 · {latest.measuredDate} <strong>{latest.height} cm / {latest.weight} kg</strong></p> : null}
    <Message message={message} />
    <section className="panel"><h2>{editing ? '신체 기록 수정' : '신체 기록 추가'}</h2>
      <form key={editing?.measurementId || `new-${retry}`} onSubmit={submit}><fieldset className="measurement-fields" disabled={busy}>
        <Field label="측정일" name="measuredDate" type="date" max={today()} defaultValue={editing?.measuredDate || today()} />
        <Field label="키 (cm)" name="height" type="number" min="0.000001" step="any" defaultValue={editing?.height || ''} inputMode="decimal" />
        <Field label="몸무게 (kg)" name="weight" type="number" min="0.000001" step="any" defaultValue={editing?.weight || ''} inputMode="decimal" />
      </fieldset><div className="form-actions"><SaveButton busy={busy}>기록 저장</SaveButton>{editing ? <button className="outline" type="button" disabled={busy} onClick={() => setEditing(null)}>취소</button> : null}</div></form>
    </section>
    <section className="panel"><h2>기록 내역 <span className="count">{measurements.length}건</span></h2>
      <div className="table-wrap"><table><thead><tr><th scope="col">측정일</th><th scope="col">키 (cm)</th><th scope="col">몸무게 (kg)</th><th scope="col">관리</th></tr></thead>
        <tbody>{measurements.slice(page * 10, (page + 1) * 10).map(item => <tr key={item.measurementId}><td>{item.measuredDate}</td><td>{item.height}</td><td>{item.weight}</td><td><div className="row-actions">
          <button className="outline" disabled={busy} aria-label={`신체 기록 ${item.measurementId} 수정`} onClick={() => setEditing(item)}>수정</button>
          <button className="outline danger" disabled={busy} aria-label={`신체 기록 ${item.measurementId} 삭제`} onClick={() => command(item.measurementId, 'delete', '신체 기록을 삭제했습니다. 아래에서 복구할 수 있습니다.')}>삭제</button>
        </div></td></tr>)}</tbody></table></div>
      {measurements.length === 0 ? <p className="empty">아직 기록이 없습니다.<br /><span>첫 번째 신체 기록을 추가해보세요.</span></p> : <div className="pagination"><button className="outline" disabled={!page || busy} onClick={() => setPage(value => value - 1)}>이전 기록</button><span>{page + 1} / {Math.ceil(measurements.length / 10)}</span><button className="outline" disabled={(page + 1) * 10 >= measurements.length || busy} onClick={() => setPage(value => value + 1)}>다음 기록</button></div>}
      {deleted.length ? <div className="undo-list"><h3>삭제된 신체 기록</h3>{deleted.map(item => <p key={item.measurementId}>{item.measuredDate} · {item.weight} kg <button className="outline" disabled={busy} aria-label={`신체 기록 ${item.measurementId} 복구`} onClick={() => command(item.measurementId, 'restore', '신체 기록을 복구했습니다.')}>복구</button></p>)}</div> : null}
    </section>
  </>
}
