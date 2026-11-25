import { DAILY_REFERENCE_VALUES } from '../../shared/utils/nutrition'

/** 1일 영양성분 기준치 고정 참고표. 개인별 권장량과 구분해서 보여준다. */
export default function DailyReferenceTable() {
  return (
    <div className="reference">
      <h3>1일 영양성분 기준치</h3>
      <p className="help">
        식품 영양표시에 쓰는 고정 기준값으로, 개인 권장량과 다를 수 있습니다. 권장량을 계산할 수 없을 때는 이 값으로 탄수화물·단백질·지방 초과 여부를
        확인합니다. 당류·식이섬유·포화지방·콜레스테롤·나트륨은 참고용입니다.
      </p>
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
                <td className="num">{value.toLocaleString('ko-KR')}</td>
                <td>{unit}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
