import DeletedList from '../../shared/components/DeletedList'
import EmptyState from '../../shared/components/EmptyState'
import { Field, Message, SaveButton } from '../../shared/components/Forms'
import PageHeader from '../../shared/components/PageHeader'
import Pagination from '../../shared/components/Pagination'
import StatList from '../../shared/components/StatList'
import { today } from '../../shared/utils/form'
import useMeasurements from './useMeasurements'
import './measurements.css'

const PAGE_SIZE = 10
const DELETED_PREVIEW = 5

function MeasurementForm({ editing, latest, retry, busy, onSubmit, onCancel }) {
  return (
    <section className="panel">
      <h2>{editing ? '신체 기록 수정' : '신체 기록 추가'}</h2>
      <form key={editing?.measurementId || `new-${retry}`} onSubmit={onSubmit}>
        <fieldset className="measurement-fields" disabled={busy}>
          <Field label="측정일" name="measuredDate" type="date" max={today()} defaultValue={editing?.measuredDate || today()} />
          <Field label="키 (cm)" name="height" type="number" min="0.000001" step="any" defaultValue={(editing ? editing.height : latest?.height) || ''} inputMode="decimal" />
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

export default function MeasurementPage({ measurements, latest, api, onSaved }) {
  const { busy, message, editing, setEditing, deleted, retry, page, setPage, submit, command } = useMeasurements(api, onSaved)
  const pageCount = Math.ceil(measurements.length / PAGE_SIZE)

  return (
    <>
      <PageHeader title="신체 기록" />
      {latest ? (
        <StatList
          items={[
            { label: '최근 측정', value: latest.measuredDate },
            { label: '키', value: latest.height, unit: 'cm' },
            { label: '몸무게', value: latest.weight, unit: 'kg' },
          ]}
        />
      ) : null}
      <Message message={message} />
      <MeasurementForm editing={editing} latest={latest} retry={retry} busy={busy} onSubmit={submit} onCancel={() => setEditing(null)} />
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
          <EmptyState>아직 기록이 없습니다. 위에서 키와 몸무게를 입력해 첫 기록을 남기세요.</EmptyState>
        ) : pageCount > 1 ? (
          <Pagination
            page={page + 1}
            total={pageCount}
            disabled={busy}
            onChange={value => setPage(value - 1)}
            previousLabel="이전 기록"
            nextLabel="다음 기록"
          />
        ) : null}
        <DeletedList
          title="삭제한 신체 기록"
          items={deleted}
          itemKey={item => item.measurementId}
          renderLabel={item => `${item.measuredDate} · ${item.weight} kg`}
          restoreName={item => `신체 기록 ${item.measuredDate} ${item.weight}kg 복구`}
          busy={busy}
          onRestore={item => command(item.measurementId, 'restore', '신체 기록을 복구했습니다.')}
          preview={DELETED_PREVIEW}
        />
      </section>
    </>
  )
}
