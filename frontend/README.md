# DietManager 프론트엔드

React 19 + Vite. 백엔드 실행과 전체 구성은 저장소 루트의 `README.md`를 참고한다.

## 실행

```powershell
npm ci
npm run dev -- --host 127.0.0.1   # http://127.0.0.1:5186
```

- 포트는 5186 고정(`strictPort`)이다. 사용 중이면 다른 포트로 옮기지 않고 멈춘다.
- `/api` 요청은 기본으로 `http://localhost:8080`에 전달된다. 다른 백엔드 주소는 `.env.local`의 `VITE_API_PROXY_TARGET`으로 지정한다(`.env.example` 참고).
- 화면 주소는 `#` 없는 경로(`/dashboard`, `/intakes`, `/plans`, `/measurements`, `/trends`, `/account`)다. 배포 서버는 파일이 없는 경로 요청을 `index.html`로 응답해야 한다.

## 명령

| 명령 | 내용 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 배포용 빌드(`dist/`) |
| `npm run lint` | 코드 검사 |
| `npm run test` | 단위 테스트(Vitest, 계산 로직) |
| `npm run test:e2e` | Playwright 브라우저 테스트(백엔드·프론트 서버를 먼저 실행) |

## E2E 테스트

- 고정 테스트 계정 하나로 반복 실행한다. `.env.local`에 `E2E_EMAIL`, `E2E_PASSWORD`를 넣는다(환경 변수가 있으면 그 값이 우선). 계정이 없으면 첫 실행 때 자동으로 가입한다.
- 프론트 서버는 `127.0.0.1:5186`으로 접속할 수 있어야 한다. `npm run dev`만 실행하면 `localhost`(IPv6)로만 열려 테스트가 연결하지 못하므로 `--host 127.0.0.1`을 붙인다.
- 실행할 때마다 고정 계정에 기록이 조금씩 쌓인다. 테스트는 빈 날짜와 실행별 값을 써서 쌓인 기록의 영향을 받지 않는다.
- 식품 검색 테스트는 실제 식품 API를 호출한다.

## 폴더 구조

```
src/
├─ features/<기능>/   화면, 그 기능의 API 호출(api.js), 상태 훅(use*.js), 화면 전용 스타일(<기능>.css)
│   dashboard, intakes, plans, measurements, trends, account, auth, foods, nutrition, workspace
├─ shared/
│   ├─ components/    공통 컴포넌트와 옆에 둔 스타일(<이름>.css). 모양·배치만 맡고 데이터는 props로 받는다
│   │   PageHeader, SectionHeader, EmptyState, RecordItem, Pagination, DeletedList, StatList,
│   │   Dialog, DateStepper, Forms, ResourceState, AppLink
│   ├─ styles/        common.css(제목·버튼·알림·표·탭), components.js(공통 컴포넌트 스타일을 먼저 불러옴)
│   └─ api/, hooks/, utils/
├─ index.css         색·글꼴·크기 기준값과 기본 스타일
└─ App.css           화면 틀(사이드바·상단 바·하단 탭)
e2e/                 Playwright 테스트와 고정 계정 준비(global-setup.js)
```

스타일은 `index.css` → `shared/styles/common.css` → 공통 컴포넌트 → `App.css` → 화면 순서로 들어간다(`main.jsx`). 같은 우선순위 선택자는 나중 것이 이기므로, 화면 스타일에서 공통 컴포넌트를 바꿀 때는 `.meal-group .add`처럼 화면 클래스를 앞에 붙인다. 클래스 이름은 e2e가 요소를 찾는 데도 쓰므로 바꾸지 않는다.
