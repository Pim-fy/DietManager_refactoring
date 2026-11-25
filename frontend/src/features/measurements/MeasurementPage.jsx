import { Field, Message, SaveButton } from '../../shared/components/Forms'
import { today } from '../../shared/utils/form'
import useMeasurements from './useMeasurements'

const PAGE_SIZE = 10

function MeasurementForm({ editing, retry, busy, onSubmit, onCancel }) {
  return (
    <section className="panel">
      <h2>{editing ? '신체 기록 수정' : '신체 기록 추가'}</h2>
      <form key={editing?.measurementId || `new-${retry}`} onSubmit={onSubmit}>
        <fieldset className="measurement-fields" disabled={busy}>
          <Field label="측정일" name="measuredDate" type="date" max={today()} defaultValue={editing?.measuredDate || today()} />
          <Field label="키 (cm)" name="height" type="number" min="0.000001" step="any" defaultValue={editing?.height || ''} inputMode="decimal" />
          <Field label="몸무게 (kg)" name="weight" type="number" min="0.000001" step="any" defaultValue={editing?.weight || ''} inputMode="decimal" />
        </fieldset>
        <div className="form-actions">
          <SaveButton busy={busy}>기록 저장</SaveButton>
          {editing ? (
            <button className="outline" type="button" disabled={busy} onClick={onCancel}>
              취소
            </button>
          ) : null}
        </div>
      </form>
    </section>
  )
}

function MeasurementRow({ item, busy, onEdit, onCommand }) {
  return (
    <tr>
      <td>{item.measuredDate}</td>
      <td className="num">{item.height}</td>
      <td className="num">{item.weight}</td>
      <td>
        <div className="row-actions">
          <button className="text-button" disabled={busy} aria-label={`신체 기록 ${item.measuredDate} ${item.weight}kg 수정`} onClick={() => onEdit(item)}>
            수정
          </button>
          <button
            className="text-button danger"
            disabled={busy}
            aria-label={`신체 기록 ${item.measuredDate} ${item.weight}kg 삭제`}
            onClick={() => onCommand(item.measurementId, 'delete', '신체 기록을 삭제했습니다. 삭제한 기록에서 복구할 수 있습니다.')}
          >
            삭제
          </button>
        </div>
      </td>
    </tr>
  )
}

function DeletedMeasurements({ deleted, busy, onCommand }) {
  if (!deleted.length) return null
  return (
    <div className="undo-list">
      <h3>삭제한 신체 기록</h3>
      {deleted.map(item => (
        <p key={item.measurementId}>
          <span>
            {item.measuredDate} · {item.weight} kg
          </span>
          <button
            className="text-button"
            disabled={busy}
            aria-label={`신체 기록 ${item.measuredDate} ${item.weight}kg 복구`}
            onClick={() => onCommand(item.measurementId, 'restore', '신체 기록을 복구했습니다.')}
          >
            복구
          </button>
        </p>
      ))}
    </div>
  )
}

export default function MeasurementPage({ measurements, latest, api, onSaved }) {
  const { busy, message, editing, setEditing, deleted, retry, page, setPage, submit, command } = useMeasurements(api, onSaved)
  const pageCount = Math.ceil(measurements.length / PAGE_SIZE)

  return (
    <>
      <div className="page-heading">
        <h1>신체 기록</h1>
      </div>
      {latest ? (
        <dl className="latest">
          <div>
            <dt>최근 측정</dt>
            <dd>{latest.measuredDate}</dd>
          </div>
          <div>
            <dt>키</dt>
            <dd>
              {latest.height} <small>cm</small>
            </dd>
          </div>
          <div>
            <dt>몸무게</dt>
            <dd>
              {latest.weight} <small>kg</small>
            </dd>
          </div>
        </dl>
      ) : null}
      <Message message={message} />
      <MeasurementForm editing={editing} retry={retry} busy={busy} onSubmit={submit} onCancel={() => setEditing(null)} />
      <section className="panel">
        <h2>
          기록 내역 <span className="count">{measurements.length}건</span>
        </h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">측정일</th>
                <th scope="col" className="num">
                  키 (cm)
                </th>
                <th scope="col" className="num">
                  몸무게 (kg)
                </th>
                <th scope="col">
                  <span className="visually-hidden">관리</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {measurements.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE).map(item => (
                <MeasurementRow key={item.measurementId} item={item} busy={busy} onEdit={setEditing} onCommand={command} />
              ))}
            </tbody>
          </table>
        </div>
        {measurements.length === 0 ? (
          <p className="empty">아직 기록이 없습니다. 위에서 키와 몸무게를 입력해 첫 기록을 남기세요.</p>
        ) : pageCount > 1 ? (
          <div className="pagination">
            <button className="outline" disabled={!page || busy} onClick={() => setPage(value => value - 1)}>
              이전 기록
            </button>
            <span>
              {page + 1} / {pageCount}
            </span>
            <button className="outline" disabled={(page + 1) * PAGE_SIZE >= measurements.length || busy} onClick={() => setPage(value => value + 1)}>
              다음 기록
            </button>
          </div>
        ) : null}
        <DeletedMeasurements deleted={deleted} busy={busy} onCommand={command} />
      </section>
    </>
  )
}
