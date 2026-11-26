// 공통 컴포넌트 스타일을 화면 스타일보다 먼저 한꺼번에 불러온다(main.jsx에서 App보다 먼저 import).
// 각 컴포넌트도 자기 .css를 import하지만 CSS 순서는 처음 불러온 위치로 정해지므로, 화면 import 순서와 상관없이 공통 → 화면 순서가 된다.
import.meta.glob('../components/*.css', { eager: true })
