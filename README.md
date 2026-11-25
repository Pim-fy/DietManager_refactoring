# DietManager_refactoring

## 기존 프로젝트 문제점
1. 불명확한 계층 분리
2. 빈약한 도메인 모델
3. 미흡한 예외 처리 및 유효성 검증
4. 성능 및 확장성 이슈
    - SQL 쿼리 수·N + 1 여부는 아직 측정하지 않았다
    - 코드 중복 발생
5. 권장 섭취량을 상수로 두어 개인 별 추천이 불가능

## 주요 아키텍처 변경 사항
1. Persistence Layer: 로컬 MySQL 저장·연결 및 격리 통합 테스트 구축
	- 로컬 MySQL을 사용하고 Flyway로 스키마를 관리한다. 테스트는 H2 메모리 DB를 사용한다.
2. Frontend & Communication: Thymeleaf (SSR) → React (CSR) 
	- 백엔드와 프론트엔드의 완전한 분리 
	- 서버는 REST API 제공에 집중하고, 클라이언트는 상태 중심의 UI 렌더링 수행

## 주요 로직 변경 사항
1. 권장 섭취량 로직 변경
	- 사용자 프로필·최신 신체 기록·활동량을 기반으로 Mifflin–St Jeor 기준으로 개인별로 계산한다.

## User 도메인
- 회원가입, JWT 로그인, 계정 조회·수정, 비밀번호 변경 구현.
- 성별·생년월일·목표 프로필 조회·수정 구현.
- 키·몸무게 측정 기록 저장, 최신 기록과 전체 이력 조회 구현.
- 사용자 도메인 커스텀 예외와 공통 예외 응답 구현.

## 프론트엔드
- 로그인·회원가입, 내 정보(계정·프로필·비밀번호), 신체 기록 화면을 기존 API에 연결.
- JWT는 `sessionStorage`에 저장하고 요청의 `Authorization: Bearer` 헤더로 전달.
- 새로고침 시 로그인 상태 유지, 인증 만료 시 HttpOnly 쿠키로 갱신 후 한 번 재요청, 갱신 실패 시 재로그인 안내.
- 입력 검증, 요청 중 중복 제출 방지, 성공·오류 안내, 기록이 없는 상태와 모바일 화면 지원.
- 새 로그인 토큰은 서버 로그아웃·갱신 토큰 재사용으로 폐기된다. 비밀번호 변경은 기존 Access·Refresh Token을 모두 무효화한다.

### 로컬 실행
1. 백엔드 실행: `cd backend`, `./gradlew bootRun` (Windows: `gradlew.bat`). DB·JWT 설정이 담긴 `application-local.properties`가 필요하며 저장소에는 포함되지 않는다. 기본 설정에서 Flyway가 켜지고 JPA는 `validate`로 동작하므로 스키마를 다시 만들지 않는다.
2. 프론트엔드 실행: `cd frontend`, `npm ci`, `npm run dev`.
3. `http://localhost:5186`에서 접속한다. 개발·미리보기 포트는 5186으로 고정하며,
   사용 중이면 자동으로 다른 포트로 이동하지 않고 실행을 중단한다.
   `/api` 요청은 기본적으로 `http://localhost:8080`으로 프록시된다.
   다른 백엔드 포트는 `VITE_API_PROXY_TARGET` 환경변수로 지정한다.
   고정으로 쓰려면 `frontend/.env.example`을 `.env.local`로 복사해 값을 넣는다(`.env`, `.env.local`은 Git에서 제외).

포트를 지정해 실행하는 PowerShell 예(프론트 5186, 백엔드 18086):

```powershell
# backend 폴더
.\gradlew.bat bootRun --args="--server.address=127.0.0.1 --server.port=18086 --logging.level.root=ERROR"
# 별도 터미널의 frontend 폴더
$env:VITE_API_PROXY_TARGET='http://127.0.0.1:18086'
npm run dev -- --host 127.0.0.1
```

빌드: `npm run build`. 코드 검사: `npm run lint`.
배포 시 프론트와 같은 호스트의 `/api` 경로를 백엔드로 전달하도록 리버스 프록시를 설정해야 한다.

## 테스트
- 서비스·컨트롤러 단독 테스트와 실제 Spring Security 필터 체인을 사용하는 JWT 통합 테스트.
- JWT 통합 테스트는 회원가입·로그인부터 보호 API 조회·수정, 잘못된·만료된 토큰 거부,
  비밀번호 변경 후 재로그인, 사용자별 측정 이력 분리, 인증·입력 검증을 확인한다.
- Spring 컨텍스트 테스트는 `test` 프로필의 격리된 H2 메모리 DB와 테스트 전용 서명 키를 사용한다.
  개인 MySQL 데이터와 운영 JWT 키가 필요하지 않다.
- 실행: `cd backend`, `./gradlew test` (Windows: `gradlew.bat test`). MySQL 통합 테스트는 로컬 MySQL이 필요하며 기존 테이블과 분리된 임시 테이블을 사용한다: `./gradlew mysqlTest`. 브라우저 E2E는 서버 실행 후 `cd frontend`, `npm run test:e2e`.

## 권장량·기록 추이·식단 계획
- 활동량 5단계와 Mifflin–St Jeor 기준으로 권장 칼로리·영양소를 계산하고 실제 섭취와 비교한다.
- 신체 기록 수정·삭제·복구, 주·월별 영양·체중 추이와 표 페이지 이동을 지원한다.
- 식품 즐겨찾기·최근 식품과 식단 계획 저장·수정·삭제·복구·섭취 전환을 지원한다.
- 계산 기준과 API는 `domain/nutrition`, `domain/plan`, `domain/statistics` 패키지를 참고한다.

## 로컬 유지보수와 검증
- 토큰 갱신·폐기·재로그인, 로그인 실패 제한, Flyway 버전 관리, 백업·복원, MySQL 통합 테스트와 브라우저 E2E를 구현했다.
- 식품 API 일별 사용량 저장·분당/일별 제한, 오류 안내와 검색 재시도를 제공한다.
- 백업·복원은 `cd backend`, `./gradlew databaseMaintenance -Poperation=backup`(또는 `verify`, `migrate`, `restore`)으로 실행한다. 백업은 프로젝트 루트의 `.local-backups`에 저장되며 Git에서 제외된다.
- 새 버전 백엔드를 시작하기 전에 DB 쓰기를 중지한다. `databaseMaintenance -Poperation=backup`으로 JSON과 SHA-256 파일을 보관하고, `-Poperation=verify`를 실행한다. `verify`는 별도 백업을 생성해 체크섬, 임시 테이블 복원, 전체 행·열 비교를 확인한다.
- 검증에 실패하면 배포를 중단한다. 검증을 통과하면 `-Poperation=migrate`로 백업을 다시 검증하고 Flyway를 적용한 뒤 백엔드를 시작한다.
- CI, 다중 서버 캐시·부하 테스트, HTTPS·배포 프록시·운영 승인·서버 모니터링은 이번 범위에서 제외했다.

## 식품 검색·섭취 기록
- API 검색 없이 음식명과 영양값을 직접 입력할 수 있고, 기록별 음식명·기준 영양정보를 수정할 수 있다.
- 식약처 식품영양성분 API의 식품명 검색과 페이지 이동을 연결했다.
- 날짜·끼니·섭취량 저장, 수정, 삭제와 복구, 일별 칼로리·탄수화물·단백질·지방 합계를 제공한다.
- 영양값은 저장 당시 원본과 기준량을 보관하고 섭취량에 비례해 소수 둘째 자리까지 계산한다.
- 누락된 영양값은 0으로 간주하지 않으며, 일별 합계에는 누락 기록 수를 표시한다.
- g/ml 기준량을 해석할 수 없는 식품은 저장할 수 없다. 사용자별 기록 접근을 제한한다.
- 검색 캐시는 5분, 선택 식품 캐시는 1시간이다. 서버 재시작 또는 캐시 만료 후 새 기록을 저장하려면 다시 검색한다.
- 식약처 API 인증키는 `food-nutrition.service-key` 설정값으로 지정하며 저장소에 포함하지 않는다.

API: `GET /api/foods?keyword=김밥&page=1&size=10`, `GET /api/intakes?date=2026-10-01`,
`POST /api/intakes`, `PATCH /api/intakes/{id}`, `DELETE /api/intakes/{id}`,
`POST /api/intakes/{id}/restore`. 모두 JWT 인증이 필요하다.

DB 스키마는 `backend/src/main/resources/db/migration`의 Flyway V1~V4로 관리한다.
새 빈 DB에는 V1부터 순서대로 적용된다.

백엔드 일반 테스트 119개, MySQL 통합 테스트 24개, Chrome E2E 7개가 통과했다(2026-11-24 재실행).
E2E는 백엔드·프론트 서버를 실행한 뒤 직접 실행해야 한다.

성능(쿼리 수·N+1), 부하, 전체 보안 감사는 아직 검증하지 않았다.

## 코드 구조
- 백엔드: `global/`(error, config, common, security, maintenance)과 `domain/<기능>/`으로 나누고, 각 기능 안은 `controller`, `service`, `repository`, `entity`, `dto`, `exception` 계층으로 둔다.
- 프론트: `src/features/<기능>/`(화면·훅·`api.js`)과 `src/shared/`(API 클라이언트, 공통 컴포넌트·훅·유틸)로 나눈다.
- 오류 응답은 `{code, message[, errors]}` 형식이며 코드 목록은 `global/error/ErrorCode.java`에 있다.
