import { DAILY_REFERENCE_VALUES } from '../../shared/utils/nutrition'

/** 1일 영양성분 기준치 고정 참고표. 개인별 권장량과 구분해서 보여준다. */
export default function DailyReferenceTable() {
  return (
    <section className="panel">
      <h2>1일 영양성분 기준치</h2>
      <div className="table-wrap">
        <table aria-label="1일 영양성분 기준치">
          <thead>
            <tr>
              <th scope="col">영양성분</th>
              <th scope="col">기준치</th>
              <th scope="col">단위</th>
            </tr>
          </thead>
          <tbody>
            {DAILY_REFERENCE_VALUES.map(([name, value, unit]) => (
              <tr key={name}>
                <td>{name}</td>
                <td>{value.toLocaleString('ko-KR')}</td>
                <td>{unit}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="help">
        식품 영양표시에 쓰는 고정 기준값입니다. 위의 권장 섭취량은 프로필과 신체 기록으로 계산한 개인 기준이라 값이 다를 수 있습니다. 당류·식이섬유·포화지방·콜레스테롤·나트륨은
        섭취량을 계산하지 않고 참고용으로만 보여줍니다.
      </p>
    </section>
  )
}
