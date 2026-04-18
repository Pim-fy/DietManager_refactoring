# DietManager_refactoring

## 기존 프로젝트 문제점
1. 불명확한 계층 분리
2. 빈약한 도메인 모델
3. 미흡한 예외 처리 및 유효성 검증
4. 성능 및 확장성 이슈
    - N + 1 문제 발생.
    - 코드 중복.

## 주요 아키텍처 변경 사항
1. Persistence Layer: H2 (In-memory) → MySQL (RDBMS) 
	- 데이터 휘발성 문제 해결 및 실 서비스 환경과 동일한 DB 환경 구축 
2. Frontend & Communication: Thymeleaf (SSR) → React (CSR) 
	- 백엔드와 프론트엔드의 완전한 분리 
	- 서버는 REST API 제공에 집중하고, 클라이언트는 상태 중심의 UI 렌더링 수행