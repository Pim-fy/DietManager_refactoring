import './StatList.css'

/** 수치 묶음(dl). items: [{ label, value, unit, note }]. value가 없으면 note를 작은 글씨로 보여준다. */
export default function StatList({ items }) {
  return (
    <dl className="latest">
      {items.map(({ label, value, unit, note }) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>
            {value != null ? (
              <>
                {value}
                {unit != null ? <small> {unit}</small> : null}
              </>
            ) : (
              <small>{note}</small>
            )}
          </dd>
        </div>
      ))}
    </dl>
  )
}
