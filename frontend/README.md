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
├─ features/<기능>/   화면, 그 기능의 API 호출(api.js), 상태 훅(use*.js)
│   dashboard, intakes, plans, measurements, trends, account, auth, foods, nutrition, workspace
├─ shared/           여러 기능이 함께 쓰는 컴포넌트·훅·유틸
├─ index.css         색·글꼴·크기 기준값
└─ App.css           화면 스타일
e2e/                 Playwright 테스트와 고정 계정 준비(global-setup.js)
```
